CREATE TABLE payment_intents (
    id UUID PRIMARY KEY,
    owner_account_id UUID NOT NULL REFERENCES accounts(id),
    idempotency_key VARCHAR(128) NOT NULL,
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    provider_reference VARCHAR(128) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    authorized_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    UNIQUE (owner_account_id, idempotency_key)
);

CREATE INDEX idx_payment_intents_owner_created ON payment_intents(owner_account_id, created_at);
