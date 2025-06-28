-- Table to store messages initiated by and received from beacons
CREATE TABLE inbound_messages (
                                  id BIGINT NOT NULL PRIMARY KEY,
                                  beacon_id BIGINT NOT NULL REFERENCES beacons(id),
                                  beacon_msg_id BIGINT NOT NULL,
                                  msg_type VARCHAR(20) NOT NULL,
                                  op_type VARCHAR(50) NOT NULL,
                                  beacon_counter BIGINT NOT NULL,
                                  payload JSONB,
                                  received_at TIMESTAMP WITH TIME ZONE NOT NULL
);
-- Create a unique constraint to prevent processing the same message twice
CREATE UNIQUE INDEX idx_inbound_message_unique ON inbound_messages (beacon_id, beacon_msg_id);
CREATE SEQUENCE inbound_messages_seq START WITH 1 INCREMENT BY 50;