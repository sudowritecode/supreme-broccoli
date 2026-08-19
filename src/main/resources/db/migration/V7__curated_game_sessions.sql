CREATE TABLE game_sessions (
    id UUID PRIMARY KEY,
    game_id VARCHAR(40) NOT NULL,
    source_type VARCHAR(16) NOT NULL,
    source_id UUID NOT NULL,
    started_by_account_id UUID NOT NULL REFERENCES accounts(id),
    started_by_device_id UUID NOT NULL REFERENCES devices(id),
    status VARCHAR(16) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_game_sessions_active_source ON game_sessions(source_type, source_id, status);

CREATE TABLE game_session_participants (
    game_session_id UUID NOT NULL REFERENCES game_sessions(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES accounts(id),
    participant_status VARCHAR(16) NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL,
    left_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (game_session_id, account_id)
);

CREATE INDEX idx_game_session_participants_account ON game_session_participants(account_id, participant_status);
