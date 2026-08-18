CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    username VARCHAR(40) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    disabled_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE devices (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts (id),
    public_identity_key VARCHAR(4096) NOT NULL,
    access_token_hash VARCHAR(64) NOT NULL UNIQUE,
    label VARCHAR(120) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_devices_account_active ON devices (account_id, revoked_at);

CREATE TABLE contact_requests (
    id UUID PRIMARY KEY,
    sender_account_id UUID NOT NULL REFERENCES accounts (id),
    recipient_account_id UUID NOT NULL REFERENCES accounts (id),
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    responded_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_contact_request_not_self CHECK (sender_account_id <> recipient_account_id),
    CONSTRAINT ck_contact_request_status CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED')),
    CONSTRAINT uq_contact_request_direction UNIQUE (sender_account_id, recipient_account_id)
);

CREATE INDEX idx_contact_requests_recipient_status ON contact_requests (recipient_account_id, status);

CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    direct_key VARCHAR(73) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE conversation_members (
    conversation_id UUID NOT NULL REFERENCES conversations (id),
    account_id UUID NOT NULL REFERENCES accounts (id),
    added_at TIMESTAMP WITH TIME ZONE NOT NULL,
    left_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (conversation_id, account_id)
);

CREATE INDEX idx_conversation_members_account_active ON conversation_members (account_id, left_at);
