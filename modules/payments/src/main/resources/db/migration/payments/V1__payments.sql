-- Payment intents and API idempotency keys, owned by the payments module.
-- Applied by its own Flyway run (schema `payments`, location
-- classpath:db/migration/payments); conventions as in customers V1.
--
-- Payments move money by posting to the ledger (ledger.post_journal, with
-- the intent id as external_ref), so a retried post never double-books.
--
-- Requires PostgreSQL 19 for the idempotency pattern below
-- (INSERT ... ON CONFLICT DO SELECT).

CREATE SCHEMA IF NOT EXISTS payments;

-- ---------------------------------------------------------------------------
-- intents: one request to move money
-- ---------------------------------------------------------------------------
--   transfer: account_id -> counterparty_account_id, both ours, booked at once
--   payout:   account_id -> an external party over a rail
--   debit:    an external party -> account_id, pulled under a mandate
--
-- Status: pending -> processing -> succeeded | failed, or pending -> canceled.
CREATE TABLE payments.intents (
    id                      uuid        PRIMARY KEY DEFAULT uuidv7(),
    tenant_id               uuid        NOT NULL,
    kind                    text        NOT NULL,
    status                  text        NOT NULL DEFAULT 'pending',
    -- accounts.accounts.id: debited by a transfer or payout, credited by a debit.
    account_id              uuid        NOT NULL,
    -- transfer only: the receiving accounts.accounts.id.
    counterparty_account_id uuid,
    -- payout and debit only: the external party. References and tokens only,
    -- never raw card or bank numbers.
    counterparty            jsonb,
    amount_minor            bigint      NOT NULL,
    currency                text        NOT NULL,
    -- e.g. ach, sepa, rtp, pix; null for a transfer.
    rail                    text,
    -- The rail's id for this payment, for reconciliation and callbacks.
    rail_ref                text,
    -- ledger.journals.id once booked.
    journal_id              uuid,
    failure_code            text,
    created_at              timestamptz NOT NULL DEFAULT now(),
    updated_at              timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT intents_kind_valid   CHECK (kind IN ('transfer', 'payout', 'debit')),
    CONSTRAINT intents_status_valid
        CHECK (status IN ('pending', 'processing', 'succeeded', 'failed', 'canceled')),
    CONSTRAINT intents_amount_positive CHECK (amount_minor > 0),
    CONSTRAINT intents_currency_valid  CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT intents_transfer_has_account
        CHECK ((kind = 'transfer') = (counterparty_account_id IS NOT NULL)),
    CONSTRAINT intents_external_has_counterparty
        CHECK ((kind <> 'transfer') = (counterparty IS NOT NULL)),
    CONSTRAINT intents_not_to_self CHECK (counterparty_account_id <> account_id),
    CONSTRAINT intents_succeeded_is_booked
        CHECK (status <> 'succeeded' OR journal_id IS NOT NULL),
    CONSTRAINT intents_failed_has_code
        CHECK ((status = 'failed') = (failure_code IS NOT NULL))
);

CREATE INDEX intents_tenant_id_idx  ON payments.intents (tenant_id, id);
CREATE INDEX intents_account_id_idx ON payments.intents (account_id, id);
-- Workers find payments still in flight. Partial: a finished payment leaves
-- the index, so it stays the size of the in-flight set.
CREATE INDEX intents_in_flight_idx
    ON payments.intents (status, updated_at) WHERE status IN ('pending', 'processing');
CREATE UNIQUE INDEX intents_rail_ref_key
    ON payments.intents (rail, rail_ref) WHERE rail_ref IS NOT NULL;

-- What a payment is can't change after it's created, and status only moves
-- forward along the allowed transitions.
CREATE FUNCTION payments.guard_intent() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF (NEW.id, NEW.tenant_id, NEW.kind, NEW.account_id, NEW.counterparty_account_id,
        NEW.counterparty, NEW.amount_minor, NEW.currency, NEW.created_at)
       IS DISTINCT FROM
       (OLD.id, OLD.tenant_id, OLD.kind, OLD.account_id, OLD.counterparty_account_id,
        OLD.counterparty, OLD.amount_minor, OLD.currency, OLD.created_at) THEN
        RAISE EXCEPTION 'payment % can''t change what it pays; cancel it and create another', OLD.id
            USING ERRCODE = 'restrict_violation';
    END IF;
    IF NEW.status <> OLD.status AND (OLD.status, NEW.status) NOT IN (
        ('pending', 'processing'),
        ('pending', 'canceled'),
        ('pending', 'failed'),
        ('processing', 'succeeded'),
        ('processing', 'failed')
    ) THEN
        RAISE EXCEPTION 'payment % can''t go from % to %', OLD.id, OLD.status, NEW.status
            USING ERRCODE = 'check_violation';
    END IF;
    IF OLD.journal_id IS NOT NULL AND NEW.journal_id IS DISTINCT FROM OLD.journal_id THEN
        RAISE EXCEPTION 'payment % is already booked to journal %', OLD.id, OLD.journal_id
            USING ERRCODE = 'restrict_violation';
    END IF;
    NEW.updated_at := now();
    RETURN NEW;
END;
$$;

CREATE TRIGGER intents_guard
    BEFORE UPDATE ON payments.intents
    FOR EACH ROW EXECUTE FUNCTION payments.guard_intent();

CREATE FUNCTION payments.forbid_delete() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'payments are kept for audit; cancel or fail payment % instead', OLD.id
        USING ERRCODE = 'restrict_violation';
END;
$$;

CREATE TRIGGER intents_no_delete
    BEFORE DELETE ON payments.intents
    FOR EACH ROW EXECUTE FUNCTION payments.forbid_delete();

-- ---------------------------------------------------------------------------
-- idempotency_keys: the Idempotency-Key header of write requests
-- ---------------------------------------------------------------------------
-- Claim a key and learn whether it was already used, in one round trip
-- (PostgreSQL 19):
--
--   INSERT INTO payments.idempotency_keys (tenant_id, key, request_hash)
--   VALUES ($1, $2, $3)
--   ON CONFLICT (tenant_id, key) DO SELECT
--   RETURNING request_hash, intent_id, response_status, response_body;
--
-- A different request_hash means the key was reused for another request:
-- reject it (422). A stored response is replayed; none yet means the first
-- request is still in progress (409).
CREATE TABLE payments.idempotency_keys (
    tenant_id       uuid        NOT NULL,
    key             text        NOT NULL,
    -- SHA-256 of the request, so a reused key with a different body is
    -- rejected rather than answered with another request's response.
    request_hash    bytea       NOT NULL,
    intent_id       uuid        REFERENCES payments.intents (id),
    response_status smallint,
    response_body   jsonb,
    created_at      timestamptz NOT NULL DEFAULT now(),
    expires_at      timestamptz NOT NULL DEFAULT now() + interval '24 hours',

    PRIMARY KEY (tenant_id, key),
    CONSTRAINT idempotency_keys_key_length   CHECK (length(key) BETWEEN 1 AND 255),
    CONSTRAINT idempotency_keys_hash_sha256  CHECK (octet_length(request_hash) = 32),
    CONSTRAINT idempotency_keys_response_complete
        CHECK ((response_status IS NULL) = (response_body IS NULL))
);

-- Only the expiry sweeper reads expires_at, and it rises with insert order:
-- BRIN covers it in a few pages, where a btree would be updated on every insert.
CREATE INDEX idempotency_keys_expires_at_idx
    ON payments.idempotency_keys USING brin (expires_at);
