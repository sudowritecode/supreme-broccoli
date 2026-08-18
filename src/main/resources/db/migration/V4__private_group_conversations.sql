ALTER TABLE conversations ALTER COLUMN direct_key DROP NOT NULL;
ALTER TABLE conversations ADD COLUMN conversation_type VARCHAR(16) NOT NULL DEFAULT 'DIRECT';
ALTER TABLE conversations ADD COLUMN name VARCHAR(120);
ALTER TABLE conversations ADD COLUMN membership_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE conversations ADD CONSTRAINT ck_conversation_type CHECK (conversation_type IN ('DIRECT', 'GROUP'));

ALTER TABLE conversation_members ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'MEMBER';
ALTER TABLE conversation_members ADD COLUMN member_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE conversation_members ADD COLUMN membership_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE conversation_members ADD CONSTRAINT ck_conversation_member_role CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER'));
ALTER TABLE conversation_members ADD CONSTRAINT ck_conversation_member_status CHECK (member_status IN ('INVITED', 'ACTIVE', 'DECLINED', 'LEFT', 'REMOVED'));

CREATE INDEX idx_conversation_members_active_group
    ON conversation_members (conversation_id, member_status, account_id);
