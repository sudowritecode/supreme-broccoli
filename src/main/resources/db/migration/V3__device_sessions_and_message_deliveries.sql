CREATE TABLE device_sessions (
    id UUID PRIMARY KEY,
    device_id UUID NOT NULL REFERENCES devices (id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    last_seen_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_device_sessions_expiry CHECK (expires_at > issued_at)
);

CREATE INDEX idx_device_sessions_active
    ON device_sessions (device_id, expires_at, revoked_at);

CREATE TABLE message_deliveries (
    id UUID PRIMARY KEY,
    outbox_id UUID NOT NULL REFERENCES message_outbox (id),
    recipient_device_id UUID NOT NULL REFERENCES devices (id),
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    delivered_at TIMESTAMP WITH TIME ZONE,
    last_attempt_at TIMESTAMP WITH TIME ZONE,
    attempts INTEGER NOT NULL,
    CONSTRAINT uq_message_delivery_outbox_recipient UNIQUE (outbox_id, recipient_device_id),
    CONSTRAINT ck_message_delivery_status CHECK (status IN ('PENDING', 'DELIVERED')),
    CONSTRAINT ck_message_delivery_attempts CHECK (attempts >= 0)
);

CREATE INDEX idx_message_deliveries_pending
    ON message_deliveries (recipient_device_id, status, created_at);
