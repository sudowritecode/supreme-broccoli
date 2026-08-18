CREATE TABLE call_sessions (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations (id),
    started_by_device_id UUID NOT NULL REFERENCES devices (id),
    status VARCHAR(16) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE,
    provider_session_id VARCHAR(256),
    CONSTRAINT ck_call_session_status CHECK (status IN ('ACTIVE', 'ENDED'))
);

CREATE INDEX idx_call_sessions_active_conversation
    ON call_sessions (conversation_id, status, started_at);

CREATE TABLE call_participants (
    call_session_id UUID NOT NULL REFERENCES call_sessions (id),
    account_id UUID NOT NULL REFERENCES accounts (id),
    status VARCHAR(16) NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL,
    left_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (call_session_id, account_id),
    CONSTRAINT ck_call_participant_status CHECK (status IN ('ACTIVE', 'LEFT', 'REMOVED'))
);

CREATE INDEX idx_call_participants_active
    ON call_participants (call_session_id, status, account_id);
