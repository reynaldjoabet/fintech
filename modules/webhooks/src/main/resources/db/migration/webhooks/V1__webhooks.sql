-- Webhook endpoints, the event outbox and deliveries, owned by the webhooks
-- module. Applied by its own Flyway run (schema `webhooks`, location
-- classpath:db/migration/webhooks); conventions as in customers V1.
--
-- Outbox: an event is inserted in the same transaction as the change it
-- reports, together with one delivery per subscribed endpoint, so an event is
-- never lost or sent for a change that rolled back. Dispatchers claim due
-- deliveries with SKIP LOCKED, so several can run at once without queueing
-- behind each other:
--
--   UPDATE webhooks.deliveries d
--      SET attempts = d.attempts + 1,
--          next_attempt_at = now() + $backoff
--    WHERE d.id IN (SELECT id FROM webhooks.deliveries
--                    WHERE status = 'pending' AND next_attempt_at <= now()
--                    ORDER BY next_attempt_at
--                    LIMIT $n FOR UPDATE SKIP LOCKED)
--   RETURNING d.*;
--
-- Moving next_attempt_at forward at claim time doubles as a lease: a
-- dispatcher that dies mid-send leaves the delivery to be retried after the
-- backoff.

CREATE SCHEMA IF NOT EXISTS webhooks;

CREATE FUNCTION webhooks.set_updated_at() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$;

-- ---------------------------------------------------------------------------
-- endpoints
-- ---------------------------------------------------------------------------
CREATE TABLE webhooks.endpoints (
    id                  uuid        PRIMARY KEY DEFAULT uuidv7(),
    tenant_id           uuid        NOT NULL,
    url                 text        NOT NULL,
    status              text        NOT NULL DEFAULT 'active',
    -- Empty means every event type.
    event_types         text[]      NOT NULL DEFAULT '{}',
    -- References into the secrets manager, never the signing secrets
    -- themselves. While a rotation is in progress both secrets sign, so
    -- receivers can switch without dropping events.
    secret_ref          text        NOT NULL,
    previous_secret_ref text,
    secret_rotated_at   timestamptz,
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT endpoints_url_https     CHECK (url ~ '^https://'),
    CONSTRAINT endpoints_status_valid  CHECK (status IN ('active', 'disabled')),
    CONSTRAINT endpoints_rotation_dated
        CHECK ((previous_secret_ref IS NULL) OR (secret_rotated_at IS NOT NULL)),
    CONSTRAINT endpoints_id_tenant_key UNIQUE (id, tenant_id)
);

CREATE INDEX endpoints_tenant_id_idx ON webhooks.endpoints (tenant_id, id);

CREATE TRIGGER endpoints_set_updated_at
    BEFORE UPDATE ON webhooks.endpoints
    FOR EACH ROW EXECUTE FUNCTION webhooks.set_updated_at();

-- ---------------------------------------------------------------------------
-- events: the outbox
-- ---------------------------------------------------------------------------
CREATE TABLE webhooks.events (
    id         uuid        PRIMARY KEY DEFAULT uuidv7(),
    tenant_id  uuid        NOT NULL,
    -- e.g. payment.succeeded, transfer.posted, payout.settled
    type       text        NOT NULL,
    payload    jsonb       NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT events_id_tenant_key UNIQUE (id, tenant_id)
);

CREATE INDEX events_tenant_id_idx ON webhooks.events (tenant_id, id);

-- ---------------------------------------------------------------------------
-- deliveries: one event to one endpoint, with its retry state
-- ---------------------------------------------------------------------------
CREATE TABLE webhooks.deliveries (
    -- bigint identity: deliveries are many, and never addressed alone in the API.
    id               bigint      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id        uuid        NOT NULL,
    event_id         uuid        NOT NULL,
    endpoint_id      uuid        NOT NULL,
    -- dead: retries exhausted; replayed only by hand.
    status           text        NOT NULL DEFAULT 'pending',
    attempts         integer     NOT NULL DEFAULT 0,
    next_attempt_at  timestamptz NOT NULL DEFAULT now(),
    last_status_code smallint,
    last_error       text,
    delivered_at     timestamptz,
    created_at       timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT deliveries_event_fk
        FOREIGN KEY (event_id, tenant_id) REFERENCES webhooks.events (id, tenant_id),
    CONSTRAINT deliveries_endpoint_fk
        FOREIGN KEY (endpoint_id, tenant_id) REFERENCES webhooks.endpoints (id, tenant_id),
    CONSTRAINT deliveries_event_endpoint_key UNIQUE (event_id, endpoint_id),
    CONSTRAINT deliveries_status_valid  CHECK (status IN ('pending', 'delivered', 'dead')),
    CONSTRAINT deliveries_attempts_nonneg CHECK (attempts >= 0),
    CONSTRAINT deliveries_delivered_at_matches_status
        CHECK ((status = 'delivered') = (delivered_at IS NOT NULL))
);

-- The dispatcher's queue. Partial, so it holds only pending deliveries and
-- stays small however many have been delivered.
CREATE INDEX deliveries_due_idx
    ON webhooks.deliveries (next_attempt_at) WHERE status = 'pending';
-- GET /webhooks/deliveries?endpoint_id=...
CREATE INDEX deliveries_endpoint_id_idx ON webhooks.deliveries (endpoint_id, id);

-- Every attempt updates its delivery, and the global autovacuum threshold is
-- a fraction of the table: on a large table, dead rows from retries pile up
-- long before it is reached. Vacuum after a fixed number of changes instead.
ALTER TABLE webhooks.deliveries SET (
    autovacuum_vacuum_scale_factor = 0,
    autovacuum_vacuum_threshold    = 10000
);
