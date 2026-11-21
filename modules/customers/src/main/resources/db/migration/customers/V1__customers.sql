-- Customers (people and businesses) and their identity verification (KYC/KYB),
-- owned by the customers module.
--
-- Each module applies its own migrations with its own Flyway run: here schema
-- `customers`, history table customers.flyway_schema_history, location
-- classpath:db/migration/customers. No foreign key leaves the schema: other
-- modules refer to a customer by id, so modules migrate independently.
--
-- Conventions shared by every module's migrations:
--   - Primary keys are uuidv7(): time-ordered, so inserts append to the
--     primary-key index (far fewer full-page images in WAL than random v4
--     keys), and (tenant_id, id) doubles as a newest-first pagination key.
--     A v7 id encodes its creation time to the millisecond.
--   - Every row carries tenant_id. Child tables reference (id, tenant_id), so
--     a row can't point at another tenant's parent.
--   - States are text with a CHECK, not enum types: values can be added and
--     removed later in an ordinary migration.
--   - Rows that matter for audit are closed, not deleted.

CREATE SCHEMA IF NOT EXISTS customers;

CREATE FUNCTION customers.set_updated_at() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$;

-- ---------------------------------------------------------------------------
-- customers
-- ---------------------------------------------------------------------------
CREATE TABLE customers.customers (
    id           uuid        PRIMARY KEY DEFAULT uuidv7(),
    tenant_id    uuid        NOT NULL,
    kind         text        NOT NULL,
    status       text        NOT NULL DEFAULT 'active',
    legal_name   text        NOT NULL,
    email        text,
    phone        text,
    -- The partner's own id for this customer.
    external_ref text,
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz NOT NULL DEFAULT now(),
    closed_at    timestamptz,

    CONSTRAINT customers_kind_valid   CHECK (kind IN ('person', 'business')),
    CONSTRAINT customers_status_valid CHECK (status IN ('active', 'closed')),
    CONSTRAINT customers_closed_at_matches_status
        CHECK ((status = 'closed') = (closed_at IS NOT NULL)),
    CONSTRAINT customers_id_tenant_key UNIQUE (id, tenant_id)
);

CREATE INDEX customers_tenant_id_idx ON customers.customers (tenant_id, id);
CREATE UNIQUE INDEX customers_tenant_external_ref_key
    ON customers.customers (tenant_id, external_ref) WHERE external_ref IS NOT NULL;

CREATE TRIGGER customers_set_updated_at
    BEFORE UPDATE ON customers.customers
    FOR EACH ROW EXECUTE FUNCTION customers.set_updated_at();

COMMENT ON TABLE customers.customers IS 'People and businesses a tenant serves. Closed, never deleted.';

-- ---------------------------------------------------------------------------
-- verifications: KYC/KYB checks run by a provider
-- ---------------------------------------------------------------------------
CREATE TABLE customers.verifications (
    id           uuid        PRIMARY KEY DEFAULT uuidv7(),
    tenant_id    uuid        NOT NULL,
    customer_id  uuid        NOT NULL,
    status       text        NOT NULL DEFAULT 'pending',
    provider     text        NOT NULL,
    -- The provider's id for the check; its callbacks are matched on it.
    provider_ref text,
    reason       text,
    decided_at   timestamptz,
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT verifications_customer_fk
        FOREIGN KEY (customer_id, tenant_id) REFERENCES customers.customers (id, tenant_id),
    CONSTRAINT verifications_status_valid
        CHECK (status IN ('pending', 'needs_info', 'verified', 'rejected')),
    CONSTRAINT verifications_decided_at_matches_status
        CHECK ((status IN ('verified', 'rejected')) = (decided_at IS NOT NULL))
);

CREATE INDEX verifications_customer_id_idx ON customers.verifications (customer_id, tenant_id, id);
-- At most one verification in progress per customer.
CREATE UNIQUE INDEX verifications_one_open_per_customer
    ON customers.verifications (customer_id) WHERE status IN ('pending', 'needs_info');
CREATE UNIQUE INDEX verifications_provider_ref_key
    ON customers.verifications (provider, provider_ref) WHERE provider_ref IS NOT NULL;

CREATE TRIGGER verifications_set_updated_at
    BEFORE UPDATE ON customers.verifications
    FOR EACH ROW EXECUTE FUNCTION customers.set_updated_at();

COMMENT ON TABLE customers.verifications IS 'KYC/KYB verifications; status pending -> needs_info -> verified | rejected.';
