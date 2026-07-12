-- QuoteGuard initial schema.
--
-- This replaces Hibernate's `ddl-auto=update` as the source of truth for
-- the database schema (see application.properties, now `validate`).
-- Column nullability below intentionally mirrors the JPA entity
-- annotations exactly (nullable where the entity doesn't declare
-- nullable=false, NOT NULL only where it does, or where the Java field is
-- a primitive) so Hibernate's startup validation has nothing to disagree
-- with.

CREATE TABLE users (
    id       BIGSERIAL PRIMARY KEY,
    name     VARCHAR(255),
    email    VARCHAR(255) NOT NULL,
    password VARCHAR(255),
    role     VARCHAR(50),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE clients (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(255),
    email   VARCHAR(255),
    gstin   VARCHAR(50),
    phone   VARCHAR(50),
    user_id BIGINT,
    CONSTRAINT fk_clients_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_clients_user_id ON clients (user_id);

CREATE TABLE invoices (
    id             BIGSERIAL PRIMARY KEY,
    uuid           VARCHAR(36)    NOT NULL,
    invoice_number VARCHAR(255)   NOT NULL,
    issue_date     DATE           NOT NULL,
    due_date       DATE           NOT NULL,
    currency       VARCHAR(3)     NOT NULL,
    subtotal       NUMERIC(19, 2) NOT NULL,
    tax            NUMERIC(19, 2) NOT NULL,
    total_amount   NUMERIC(19, 2) NOT NULL,
    invoice_hash   VARCHAR(64)    NOT NULL,
    status         VARCHAR(20)    NOT NULL,
    revoked_at     TIMESTAMP,
    revoked_reason VARCHAR(500),
    created_at     TIMESTAMP      NOT NULL,
    client_id      BIGINT         NOT NULL,
    user_id        BIGINT         NOT NULL,
    paid           BOOLEAN        NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_invoices_uuid UNIQUE (uuid),
    -- Closes the check-then-save race condition in
    -- InvoiceService.createInvoice: the application checks uniqueness
    -- before saving, but only this constraint makes it impossible for two
    -- concurrent requests to both win that check and both insert.
    CONSTRAINT uk_invoice_user_number UNIQUE (user_id, invoice_number),
    CONSTRAINT fk_invoices_client FOREIGN KEY (client_id) REFERENCES clients (id),
    CONSTRAINT fk_invoices_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_invoices_user_id ON invoices (user_id);
CREATE INDEX idx_invoices_client_id ON invoices (client_id);
-- uuid and the (user_id, invoice_number) pair already have implicit
-- indexes from their UNIQUE constraints above; findByUuid and
-- existsByUserIdAndInvoiceNumber both benefit from those without any
-- extra index needed here.

CREATE TABLE invoice_items (
    id         BIGSERIAL PRIMARY KEY,
    product    VARCHAR(255),
    quantity   INTEGER        NOT NULL,
    unit_price NUMERIC(19, 2) NOT NULL,
    invoice_id BIGINT,
    CONSTRAINT fk_invoice_items_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id)
);

CREATE INDEX idx_invoice_items_invoice_id ON invoice_items (invoice_id);

CREATE TABLE refresh_tokens (
    id         BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL,
    user_id    BIGINT      NOT NULL,
    expires_at TIMESTAMP   NOT NULL,
    created_at TIMESTAMP   NOT NULL,
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
-- Supports RefreshTokenCleanupTask's hourly `WHERE expires_at < :now` purge.
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
