CREATE TABLE signal_device_key_bundles (
    device_id UUID PRIMARY KEY REFERENCES devices(id),
    protocol_profile VARCHAR(64) NOT NULL,
    protocol_device_id INTEGER NOT NULL,
    registration_id INTEGER NOT NULL,
    identity_key VARCHAR(4096) NOT NULL,
    signed_prekey_id BIGINT NOT NULL,
    signed_prekey_public VARCHAR(4096) NOT NULL,
    signed_prekey_signature VARCHAR(4096) NOT NULL,
    signed_prekey_created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    signed_prekey_expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_signal_protocol_device_id CHECK (protocol_device_id > 0),
    CONSTRAINT ck_signal_registration_id CHECK (registration_id > 0),
    CONSTRAINT ck_signal_signed_prekey_lifetime CHECK (signed_prekey_expires_at > signed_prekey_created_at)
);

CREATE TABLE signal_one_time_prekeys (
    id UUID PRIMARY KEY,
    device_id UUID NOT NULL REFERENCES devices(id),
    prekey_id BIGINT NOT NULL,
    public_key VARCHAR(4096) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    claimed_at TIMESTAMP WITH TIME ZONE,
    claim_id UUID,
    CONSTRAINT uq_signal_one_time_prekey_id UNIQUE (device_id, prekey_id),
    CONSTRAINT uq_signal_one_time_prekey_claim UNIQUE (claim_id)
);

CREATE INDEX idx_signal_one_time_prekeys_available
    ON signal_one_time_prekeys(device_id, claimed_at, created_at);

CREATE TABLE signal_kyber_prekeys (
    id UUID PRIMARY KEY,
    device_id UUID NOT NULL REFERENCES devices(id),
    prekey_id BIGINT NOT NULL,
    public_key VARCHAR(16384) NOT NULL,
    signature VARCHAR(4096) NOT NULL,
    last_resort BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    claimed_at TIMESTAMP WITH TIME ZONE,
    claim_id UUID,
    CONSTRAINT uq_signal_kyber_prekey_id UNIQUE (device_id, prekey_id),
    CONSTRAINT uq_signal_kyber_prekey_claim UNIQUE (claim_id)
);

CREATE INDEX idx_signal_kyber_prekeys_available
    ON signal_kyber_prekeys(device_id, last_resort, claimed_at, created_at);

CREATE TABLE signal_identity_verifications (
    id UUID PRIMARY KEY,
    verifier_device_id UUID NOT NULL REFERENCES devices(id),
    subject_device_id UUID NOT NULL REFERENCES devices(id),
    safety_number_fingerprint VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL,
    verified_at TIMESTAMP WITH TIME ZONE,
    changed_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_signal_identity_verification UNIQUE (verifier_device_id, subject_device_id),
    CONSTRAINT ck_signal_identity_verification_not_self CHECK (verifier_device_id <> subject_device_id)
);

CREATE INDEX idx_signal_identity_verifications_verifier
    ON signal_identity_verifications(verifier_device_id, status);
