-- Account products and the accounts opened on them, owned by the accounts
-- module. Applied by its own Flyway run (schema `accounts`, location
-- classpath:db/migration/accounts); conventions as in customers V1.
--
-- Balances are not stored here: they come from the ledger. An account's id is
-- also its id in ledger.accounts, created when the account opens.

CREATE SCHEMA IF NOT EXISTS accounts;

CREATE FUNCTION accounts.set_updated_at() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$;

-- ---------------------------------------------------------------------------
-- products: account definitions a tenant offers (DDA, savings, wallet)
-- ---------------------------------------------------------------------------
CREATE TABLE accounts.products (
    id         uuid        PRIMARY KEY DEFAULT uuidv7(),
    tenant_id  uuid        NOT NULL,
    code       text        NOT NULL,
    name       text        NOT NULL,
    kind       text        NOT NULL,
    status     text        NOT NULL DEFAULT 'active',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT products_kind_valid   CHECK (kind IN ('dda', 'savings', 'wallet')),
    CONSTRAINT products_status_valid CHECK (status IN ('active', 'retired')),
    CONSTRAINT products_tenant_code_key UNIQUE (tenant_id, code),
    CONSTRAINT products_id_tenant_key   UNIQUE (id, tenant_id)
);

CREATE TRIGGER products_set_updated_at
    BEFORE UPDATE ON accounts.products
    FOR EACH ROW EXECUTE FUNCTION accounts.set_updated_at();

-- ---------------------------------------------------------------------------
-- accounts
-- ---------------------------------------------------------------------------
CREATE TABLE accounts.accounts (
    id          uuid        PRIMARY KEY DEFAULT uuidv7(),
    tenant_id   uuid        NOT NULL,
    -- customers.customers.id; no foreign key across module schemas.
    customer_id uuid        NOT NULL,
    product_id  uuid        NOT NULL,
    currency    text        NOT NULL,
    status      text        NOT NULL DEFAULT 'open',
    label       text,
    opened_at   timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    closed_at   timestamptz,

    -- No index on product_id: products are retired, never deleted, so the
    -- foreign key is never checked from the products side.
    CONSTRAINT accounts_product_fk
        FOREIGN KEY (product_id, tenant_id) REFERENCES accounts.products (id, tenant_id),
    CONSTRAINT accounts_currency_valid CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT accounts_status_valid   CHECK (status IN ('open', 'frozen', 'closed')),
    CONSTRAINT accounts_closed_at_matches_status
        CHECK ((status = 'closed') = (closed_at IS NOT NULL))
);

-- GET /accounts?customer_id=...
CREATE INDEX accounts_customer_id_idx ON accounts.accounts (tenant_id, customer_id, id);

CREATE TRIGGER accounts_set_updated_at
    BEFORE UPDATE ON accounts.accounts
    FOR EACH ROW EXECUTE FUNCTION accounts.set_updated_at();

COMMENT ON TABLE accounts.accounts IS 'Customer accounts. The id is also the account''s ledger.accounts id; the balance lives in the ledger.';
