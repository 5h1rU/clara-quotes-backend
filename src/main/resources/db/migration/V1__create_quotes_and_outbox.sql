CREATE TABLE quotes (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    age INTEGER NOT NULL CHECK (age BETWEEN 1 AND 120),
    zip_code VARCHAR(10) NOT NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('DRAFT', 'SUBMISSION_FAILED', 'SUBMITTED', 'EXPIRED')),
    coverage_type VARCHAR(16) CHECK (coverage_type IN ('BASIC', 'STANDARD', 'PREMIUM')),
    has_preexisting_conditions BOOLEAN,
    takes_prescription_medication BOOLEAN,
    uses_tobacco BOOLEAN,
    needs_spouse_coverage BOOLEAN,
    estimated_monthly_premium NUMERIC(10,2),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    submitted_at TIMESTAMPTZ
);
CREATE TABLE quote_conditions (
    quote_id UUID NOT NULL REFERENCES quotes(id) ON DELETE CASCADE,
    condition VARCHAR(32) NOT NULL,
    PRIMARY KEY (quote_id, condition)
);
CREATE INDEX quotes_draft_expiration ON quotes (created_at) WHERE status = 'DRAFT';
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    quote_id UUID NOT NULL UNIQUE REFERENCES quotes(id),
    premium NUMERIC(10,2) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);
CREATE INDEX outbox_pending ON outbox_events (occurred_at) WHERE published_at IS NULL;
