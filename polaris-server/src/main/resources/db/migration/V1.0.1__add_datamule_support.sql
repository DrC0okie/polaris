-- Adds support for the datamule feature (encrypted payloads)

-- Add the X25519 public key column to the beacons table for AEAD
ALTER TABLE beacons ADD COLUMN public_key_x25519 BYTEA;
CREATE UNIQUE INDEX IF NOT EXISTS idx_beacon_pk_x25519 ON beacons (public_key_x25519);

-- Table to store outbound message jobs
CREATE TABLE outbound_messages (
                                   id BIGINT NOT NULL PRIMARY KEY,
                                   beacon_id BIGINT NOT NULL REFERENCES beacons(id),
                                   status VARCHAR(20) NOT NULL,
                                   command_payload JSONB NOT NULL,
                                   encrypted_blob BYTEA,
                                   server_msg_id BIGINT NOT NULL UNIQUE,
                                   redundancy_factor INT NOT NULL DEFAULT 1,
                                   delivery_count INT NOT NULL DEFAULT 0,
                                   created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                   first_acknowledged_at TIMESTAMP WITH TIME ZONE
);
CREATE SEQUENCE outbound_messages_seq START WITH 1 INCREMENT BY 50;

-- Table to track each individual delivery of a message to a phone
CREATE TABLE message_deliveries (
                                    id BIGINT NOT NULL PRIMARY KEY,
                                    outbound_message_id BIGINT NOT NULL REFERENCES outbound_messages(id) ON DELETE CASCADE,
                                    phone_id BIGINT NOT NULL REFERENCES registered_phones(id),
                                    delivered_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                    ack_status VARCHAR(20) NOT NULL,
                                    ack_received_at TIMESTAMP WITH TIME ZONE,
                                    raw_ack_blob BYTEA
);
CREATE SEQUENCE message_deliveries_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE server_msg_id_seq START WITH 1 INCREMENT BY 1;