ALTER TABLE outbound_messages
    ADD COLUMN op_type VARCHAR(50) NOT NULL DEFAULT 'NO_OP';
ALTER TABLE outbound_messages
    ALTER COLUMN op_type DROP DEFAULT;