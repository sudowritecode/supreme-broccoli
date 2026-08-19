CREATE TABLE mini_app_manifests (
    id UUID PRIMARY KEY,
    app_id VARCHAR(64) NOT NULL,
    app_version VARCHAR(32) NOT NULL,
    issuer VARCHAR(120) NOT NULL,
    origin VARCHAR(255) NOT NULL,
    public_key_base64 VARCHAR(1024) NOT NULL,
    signature_base64 VARCHAR(1024) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (app_id, app_version)
);

CREATE TABLE mini_app_manifest_permissions (
    manifest_id UUID NOT NULL REFERENCES mini_app_manifests(id) ON DELETE CASCADE,
    permission VARCHAR(40) NOT NULL,
    PRIMARY KEY (manifest_id, permission)
);

CREATE TABLE mini_app_launch_tickets (
    id UUID PRIMARY KEY,
    manifest_id UUID NOT NULL REFERENCES mini_app_manifests(id),
    account_id UUID NOT NULL REFERENCES accounts(id),
    device_id UUID NOT NULL REFERENCES devices(id),
    nonce VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    ticket_signature_base64 VARCHAR(1024) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_mini_app_launch_tickets_expiry ON mini_app_launch_tickets(expires_at);
