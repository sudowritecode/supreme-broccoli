CREATE TABLE message_outbox (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL UNIQUE,
    conversation_id UUID NOT NULL REFERENCES conversations (id),
    sender_device_id UUID NOT NULL REFERENCES devices (id),
    recipient_device_id UUID NOT NULL REFERENCES devices (id),
    ciphertext VARCHAR(1000000) NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(16) NOT NULL,
    attempts INTEGER NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    last_error VARCHAR(512),
    CONSTRAINT ck_message_outbox_status CHECK (status IN ('PENDING', 'PUBLISHED')),
    CONSTRAINT ck_message_outbox_attempts CHECK (attempts >= 0)
);

CREATE INDEX idx_message_outbox_pending ON message_outbox (status, received_at);
