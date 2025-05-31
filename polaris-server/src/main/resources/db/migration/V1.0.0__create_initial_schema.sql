-- V1.0.0__create_initial_schema.sql

CREATE TABLE beacons (
                         id BIGINT NOT NULL PRIMARY KEY,
                         beacon_id INT NOT NULL UNIQUE,
                         public_key BYTEA NOT NULL,
                         last_known_counter BIGINT NOT NULL,
                         name VARCHAR(255),
                         location_description TEXT,
                         created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                         updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE SEQUENCE beacons_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE registered_phones (
                                   id BIGINT NOT NULL PRIMARY KEY,
                                   phone_technical_id BIGINT NOT NULL UNIQUE,
                                   public_key BYTEA NOT NULL UNIQUE,
                                   user_agent VARCHAR(255),
                                   last_seen_at TIMESTAMP WITH TIME ZONE,
                                   created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                   updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_phone_public_key ON registered_phones (public_key);
CREATE SEQUENCE registered_phones_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE pol_token_records (
                                   id BIGINT NOT NULL PRIMARY KEY,
                                   flags SMALLINT NOT NULL,
                                   phone_id_fk BIGINT NOT NULL REFERENCES registered_phones(id),
                                   beacon_id BIGINT NOT NULL REFERENCES beacons(id),
                                   beacon_counter BIGINT NOT NULL,
                                   nonce_hex VARCHAR(32) NOT NULL,
                                   phone_pk_used BYTEA NOT NULL,
                                   beacon_pk_used BYTEA NOT NULL,
                                   phone_sig BYTEA NOT NULL,
                                   beacon_sig BYTEA NOT NULL,
                                   is_valid BOOLEAN NOT NULL DEFAULT FALSE,
                                   validation_error VARCHAR(255),
                                   received_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX idx_poltoken_unique_proof ON pol_token_records (beacon_id, beacon_counter, nonce_hex);
CREATE SEQUENCE pol_token_records_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE admin_users (
                             id BIGINT NOT NULL PRIMARY KEY,
                             username VARCHAR(255) NOT NULL UNIQUE,
                             password_hash VARCHAR(255) NOT NULL,
                             role VARCHAR(50) NOT NULL
);
CREATE SEQUENCE admin_users_seq START WITH 1 INCREMENT BY 50;