-- The double-entry ledger, owned by the ledger module: the source of truth for
-- every balance. Applied by its own Flyway run (schema `ledger`, location
-- classpath:db/migration/ledger); conventions as in customers V1.
--
-- The database enforces the ledger's rules, so no caller can get them wrong:
--   - Every journal balances: per currency, debits equal credits, over at
--     least two postings. Checked at commit, so postings can be inserted in
--     any order within the transaction.
--   - A posting's tenant and currency are its account's (composite foreign
--     key, no trigger needed).
--   - Journals and postings are append-only, and postings can only be added
--     in the transaction that created their journal. A correction is a
--     reversal: a new journal that exactly mirrors the original.
--   - An account without allow_negative never ends a transaction below zero.
--   - Posting an external_ref that was already posted returns the first
--     journal; reusing it with different entries is an error.
--
-- Write through ledger.post_journal. It inserts postings in account order,
-- which is also the order the balance rows get locked, so two journals over
-- the same accounts can't deadlock.
--
-- Requires PostgreSQL 19: post_journal uses INSERT ... ON CONFLICT DO SELECT.

CREATE SCHEMA IF NOT EXISTS ledger;

CREATE FUNCTION ledger.forbid_change() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION '%.% is append-only; post a reversing journal instead',
        TG_TABLE_SCHEMA, TG_TABLE_NAME
        USING ERRCODE = 'restrict_violation';
END;
$$;

-- ---------------------------------------------------------------------------
-- accounts: anything that holds a balance
-- ---------------------------------------------------------------------------
CREATE TABLE ledger.accounts (
    -- For a customer account, the same id as accounts.accounts.id.
    id             uuid        PRIMARY KEY DEFAULT uuidv7(),
    tenant_id      uuid        NOT NULL,
    currency       text        NOT NULL,
    -- customer: a customer's account; internal: settlement, fees, suspense...
    kind           text        NOT NULL,
    -- The side that increases the balance: credit for what the institution
    -- owes (customer deposits), debit for what it holds (settlement assets).
    normal_balance text        NOT NULL,
    allow_negative boolean     NOT NULL DEFAULT false,
    created_at     timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT accounts_currency_valid       CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT accounts_kind_valid           CHECK (kind IN ('customer', 'internal')),
    CONSTRAINT accounts_normal_balance_valid CHECK (normal_balance IN ('debit', 'credit')),
    -- Target of the postings foreign key.
    CONSTRAINT accounts_id_tenant_currency_key UNIQUE (id, tenant_id, currency)
);

-- An account's identity and balance rules are fixed once it exists: changing
-- normal_balance would silently flip the meaning of its stored balance.
CREATE FUNCTION ledger.guard_account() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF (NEW.id, NEW.tenant_id, NEW.currency, NEW.kind, NEW.normal_balance)
       IS DISTINCT FROM (OLD.id, OLD.tenant_id, OLD.currency, OLD.kind, OLD.normal_balance) THEN
        RAISE EXCEPTION 'only allow_negative can change on ledger account %', OLD.id
            USING ERRCODE = 'restrict_violation';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER accounts_guard
    BEFORE UPDATE ON ledger.accounts
    FOR EACH ROW EXECUTE FUNCTION ledger.guard_account();

CREATE TRIGGER accounts_no_delete
    BEFORE DELETE ON ledger.accounts
    FOR EACH ROW EXECUTE FUNCTION ledger.forbid_change();

-- ---------------------------------------------------------------------------
-- balances: one row per account, maintained by the postings trigger
-- ---------------------------------------------------------------------------
-- Read balances here instead of summing postings. Grant applications SELECT
-- only; the triggers below are the one writer.
--
-- No indexed column changes on a posting, so updates are HOT (no index
-- write). At the default fillfactor, page pruning already keeps 98.6% of them
-- HOT (measured on PostgreSQL 19: 100,000 updates over 20,000 accounts), and
-- fillfactor 70 bought the last 1.4% with a 35% larger table, so it's left
-- at the default.
--
-- A posting holds its account's balance row lock until commit, so postings to
-- one account are serialised. Internal accounts that take a posting per
-- payment (settlement, fees) become the hot rows under load.
CREATE TABLE ledger.balances (
    account_id    uuid        PRIMARY KEY REFERENCES ledger.accounts (id),
    -- Signed by the account's normal side: positive means the account holds
    -- that much, in minor units.
    balance_minor bigint      NOT NULL DEFAULT 0,
    -- Bumped by every posting.
    version       bigint      NOT NULL DEFAULT 0,
    updated_at    timestamptz NOT NULL DEFAULT now()
);

CREATE FUNCTION ledger.open_balance() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO ledger.balances (account_id) VALUES (NEW.id);
    RETURN NULL;
END;
$$;

CREATE TRIGGER accounts_open_balance
    AFTER INSERT ON ledger.accounts
    FOR EACH ROW EXECUTE FUNCTION ledger.open_balance();

-- ---------------------------------------------------------------------------
-- journals: one business event, e.g. a transfer or a fee
-- ---------------------------------------------------------------------------
CREATE TABLE ledger.journals (
    id           uuid        PRIMARY KEY DEFAULT uuidv7(),
    tenant_id    uuid        NOT NULL,
    type         text        NOT NULL,
    -- The caller's idempotency key, e.g. the payment intent id. Required:
    -- every post must be safely retryable.
    external_ref text        NOT NULL,
    value_date   date        NOT NULL DEFAULT current_date,
    -- The journal this one reverses. UNIQUE: a journal is reversed at most once.
    reverses     uuid,
    metadata     jsonb       NOT NULL DEFAULT '{}',
    posted_at    timestamptz NOT NULL DEFAULT now(),
    -- The top-level transaction that created the journal; postings may only be
    -- added in that transaction.
    created_xact xid8        NOT NULL DEFAULT pg_current_xact_id(),

    CONSTRAINT journals_tenant_external_ref_key UNIQUE (tenant_id, external_ref),
    CONSTRAINT journals_id_tenant_key           UNIQUE (id, tenant_id),
    CONSTRAINT journals_reverses_key            UNIQUE (reverses),
    CONSTRAINT journals_reverses_fk
        FOREIGN KEY (reverses, tenant_id) REFERENCES ledger.journals (id, tenant_id),
    CONSTRAINT journals_not_self_reversing CHECK (reverses <> id)
);

-- ---------------------------------------------------------------------------
-- postings: the debit and credit lines of a journal
-- ---------------------------------------------------------------------------
CREATE TABLE ledger.postings (
    -- bigint identity rather than uuid: postings are never addressed alone,
    -- and an 8-byte sequential key keeps this, the largest table, and its
    -- indexes small. It also orders an account's history.
    id           bigint      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    journal_id   uuid        NOT NULL,
    tenant_id    uuid        NOT NULL,
    account_id   uuid        NOT NULL,
    direction    text        NOT NULL,
    amount_minor bigint      NOT NULL,
    currency     text        NOT NULL,

    CONSTRAINT postings_direction_valid CHECK (direction IN ('debit', 'credit')),
    CONSTRAINT postings_amount_positive CHECK (amount_minor > 0),
    CONSTRAINT postings_journal_fk
        FOREIGN KEY (journal_id, tenant_id) REFERENCES ledger.journals (id, tenant_id),
    CONSTRAINT postings_account_fk
        FOREIGN KEY (account_id, tenant_id, currency)
        REFERENCES ledger.accounts (id, tenant_id, currency)
);

-- An account's history, newest first by keyset on id:
--   WHERE account_id = $1 AND id < $cursor ORDER BY id DESC LIMIT $n
CREATE INDEX postings_account_id_idx ON ledger.postings (account_id, id);
CREATE INDEX postings_journal_id_idx ON ledger.postings (journal_id);

-- Postings join their journal's transaction or not at all.
CREATE FUNCTION ledger.guard_posting() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF (SELECT j.created_xact FROM ledger.journals j WHERE j.id = NEW.journal_id)
       IS DISTINCT FROM pg_current_xact_id() THEN
        RAISE EXCEPTION 'postings can only be added in the transaction that created journal %', NEW.journal_id
            USING ERRCODE = 'restrict_violation';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER postings_guard
    BEFORE INSERT ON ledger.postings
    FOR EACH ROW EXECUTE FUNCTION ledger.guard_posting();

-- Moves the account's balance. Locks its balance row until commit.
CREATE FUNCTION ledger.apply_posting() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    UPDATE ledger.balances b
       SET balance_minor = b.balance_minor
                         + CASE WHEN NEW.direction = a.normal_balance
                                THEN NEW.amount_minor ELSE -NEW.amount_minor END,
           version       = b.version + 1,
           updated_at    = now()
      FROM ledger.accounts a
     WHERE b.account_id = NEW.account_id
       AND a.id = NEW.account_id;
    RETURN NULL;
END;
$$;

CREATE TRIGGER postings_apply
    AFTER INSERT ON ledger.postings
    FOR EACH ROW EXECUTE FUNCTION ledger.apply_posting();

-- At commit: the account isn't overdrawn. Deferred, so a journal that takes
-- an account below zero and back within the transaction is fine.
CREATE FUNCTION ledger.check_overdraft() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (
        SELECT 1
          FROM ledger.balances b
          JOIN ledger.accounts a ON a.id = b.account_id
         WHERE b.account_id = NEW.account_id
           AND b.balance_minor < 0
           AND NOT a.allow_negative
    ) THEN
        RAISE EXCEPTION 'account % would be overdrawn', NEW.account_id
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER postings_no_overdraft
    AFTER INSERT ON ledger.postings
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION ledger.check_overdraft();

-- At commit: the journal has at least two postings and balances per currency.
-- Fires once per journal; postings can't be added in a later transaction, so
-- this holds for the journal's whole life.
CREATE FUNCTION ledger.check_journal() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    postings int;
    unbalanced boolean;
BEGIN
    SELECT coalesce(sum(per_currency.n), 0),
           coalesce(bool_or(per_currency.net <> 0), false)
      INTO postings, unbalanced
      FROM (SELECT count(*) AS n,
                   sum(CASE p.direction WHEN 'debit' THEN p.amount_minor ELSE -p.amount_minor END) AS net
              FROM ledger.postings p
             WHERE p.journal_id = NEW.id
             GROUP BY p.currency) per_currency;
    IF postings < 2 THEN
        RAISE EXCEPTION 'journal % has % posting(s); a journal needs at least two', NEW.id, postings
            USING ERRCODE = 'check_violation';
    END IF;
    IF unbalanced THEN
        RAISE EXCEPTION 'journal % does not balance: debits and credits differ in some currency', NEW.id
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER journals_balanced
    AFTER INSERT ON ledger.journals
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION ledger.check_journal();

-- Append-only.
CREATE TRIGGER journals_append_only
    BEFORE UPDATE OR DELETE ON ledger.journals
    FOR EACH ROW EXECUTE FUNCTION ledger.forbid_change();
CREATE TRIGGER journals_no_truncate
    BEFORE TRUNCATE ON ledger.journals
    FOR EACH STATEMENT EXECUTE FUNCTION ledger.forbid_change();
CREATE TRIGGER postings_append_only
    BEFORE UPDATE OR DELETE ON ledger.postings
    FOR EACH ROW EXECUTE FUNCTION ledger.forbid_change();
CREATE TRIGGER postings_no_truncate
    BEFORE TRUNCATE ON ledger.postings
    FOR EACH STATEMENT EXECUTE FUNCTION ledger.forbid_change();

-- ---------------------------------------------------------------------------
-- post_journal: the write path
-- ---------------------------------------------------------------------------
-- Posts a journal and its postings, given as parallel arrays, in one call.
-- Returns the journal id, and created = false when external_ref had already
-- been posted with the same entries (a retry): nothing is posted twice.
-- With p_reverses, the entries must exactly mirror that journal's.
CREATE FUNCTION ledger.post_journal(
    p_tenant_id    uuid,
    p_type         text,
    p_external_ref text,
    p_account_ids  uuid[],
    p_directions   text[],
    p_amounts      bigint[],
    p_currencies   text[],
    p_value_date   date  DEFAULT current_date,
    p_metadata     jsonb DEFAULT '{}',
    p_reverses     uuid  DEFAULT NULL,
    OUT journal_id uuid,
    OUT created    boolean
)
LANGUAGE plpgsql AS $$
BEGIN
    -- PostgreSQL 19: on a conflict, return the existing journal in the same
    -- round trip instead of raising a unique violation.
    INSERT INTO ledger.journals AS j (tenant_id, type, external_ref, value_date, metadata, reverses)
    VALUES (p_tenant_id, p_type, p_external_ref, p_value_date, p_metadata, p_reverses)
    ON CONFLICT (tenant_id, external_ref) DO SELECT
    RETURNING j.id, j.created_xact = pg_current_xact_id()
         INTO journal_id, created;

    IF NOT created THEN
        -- A retry must ask for exactly what was posted.
        IF EXISTS (
            (SELECT p.account_id, p.direction, p.amount_minor, p.currency
               FROM ledger.postings p WHERE p.journal_id = post_journal.journal_id
             EXCEPT ALL
             SELECT * FROM unnest(p_account_ids, p_directions, p_amounts, p_currencies))
            UNION ALL
            (SELECT * FROM unnest(p_account_ids, p_directions, p_amounts, p_currencies)
             EXCEPT ALL
             SELECT p.account_id, p.direction, p.amount_minor, p.currency
               FROM ledger.postings p WHERE p.journal_id = post_journal.journal_id)
        ) THEN
            RAISE EXCEPTION 'external_ref % was already posted with different entries', p_external_ref
                USING ERRCODE = 'unique_violation';
        END IF;
        RETURN;
    END IF;

    IF p_reverses IS NOT NULL AND EXISTS (
        (SELECT p.account_id,
                CASE p.direction WHEN 'debit' THEN 'credit' ELSE 'debit' END,
                p.amount_minor, p.currency
           FROM ledger.postings p WHERE p.journal_id = p_reverses
         EXCEPT ALL
         SELECT * FROM unnest(p_account_ids, p_directions, p_amounts, p_currencies))
        UNION ALL
        (SELECT * FROM unnest(p_account_ids, p_directions, p_amounts, p_currencies)
         EXCEPT ALL
         SELECT p.account_id,
                CASE p.direction WHEN 'debit' THEN 'credit' ELSE 'debit' END,
                p.amount_minor, p.currency
           FROM ledger.postings p WHERE p.journal_id = p_reverses)
    ) THEN
        RAISE EXCEPTION 'a reversal must mirror journal % exactly', p_reverses
            USING ERRCODE = 'check_violation';
    END IF;

    -- Account order is the lock order of the balance rows: see the header.
    INSERT INTO ledger.postings (journal_id, tenant_id, account_id, direction, amount_minor, currency)
    SELECT post_journal.journal_id, p_tenant_id, t.account_id, t.direction, t.amount_minor, t.currency
      FROM unnest(p_account_ids, p_directions, p_amounts, p_currencies)
           AS t(account_id, direction, amount_minor, currency)
     ORDER BY t.account_id;
END;
$$;

COMMENT ON TABLE ledger.journals IS 'Append-only. One balanced set of postings per business event; corrected by reversal.';
COMMENT ON TABLE ledger.postings IS 'Append-only debit/credit lines; amounts in minor units, always positive.';
COMMENT ON TABLE ledger.balances IS 'Current balance per account, maintained by the postings trigger. Read-only for applications.';
