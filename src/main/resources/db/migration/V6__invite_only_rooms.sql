CREATE TABLE rooms (
    id UUID PRIMARY KEY,
    topic VARCHAR(120) NOT NULL,
    capacity INTEGER NOT NULL CHECK (capacity BETWEEN 2 AND 50),
    status VARCHAR(16) NOT NULL,
    host_account_id UUID NOT NULL REFERENCES accounts(id),
    host_device_id UUID NOT NULL REFERENCES devices(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_rooms_active_host ON rooms(host_account_id, status);

CREATE TABLE room_interest_tags (
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    tag VARCHAR(32) NOT NULL,
    PRIMARY KEY (room_id, tag)
);

CREATE INDEX idx_room_interest_tags_tag ON room_interest_tags(tag);

CREATE TABLE account_interest_preferences (
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    tag VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (account_id, tag)
);

CREATE INDEX idx_account_interest_preferences_tag ON account_interest_preferences(tag);

CREATE TABLE room_participants (
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES accounts(id),
    participant_role VARCHAR(16) NOT NULL,
    participant_status VARCHAR(16) NOT NULL,
    invited_by_account_id UUID REFERENCES accounts(id),
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    admitted_at TIMESTAMP WITH TIME ZONE,
    left_at TIMESTAMP WITH TIME ZONE,
    removed_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (room_id, account_id)
);

CREATE INDEX idx_room_participants_lobby ON room_participants(room_id, participant_status, requested_at);
CREATE INDEX idx_room_participants_account ON room_participants(account_id, participant_status);

CREATE TABLE room_blocks (
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    blocked_account_id UUID NOT NULL REFERENCES accounts(id),
    issued_by_account_id UUID NOT NULL REFERENCES accounts(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (room_id, blocked_account_id)
);

CREATE TABLE room_reports (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    reporter_account_id UUID NOT NULL REFERENCES accounts(id),
    reported_account_id UUID REFERENCES accounts(id),
    reason VARCHAR(40) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_room_reports_room_created ON room_reports(room_id, created_at);
