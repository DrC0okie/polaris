#include "encrypted_message.h"

#include <HardwareSerial.h>
#include <string.h>
#include <sodium.h>

#include "crypto.h"

// --- InnerPlaintext Implementation ---
size_t InnerPlaintext::getTotalSerializedSize() const {
    return sizeof(msg_id) + sizeof(msg_type) + sizeof(op_type) + sizeof(beacon_cnt) +
           sizeof(actual_payload_length) + actual_payload_length;
}

size_t InnerPlaintext::serialize(uint8_t* buffer) const {
    size_t offset = 0;
    memcpy(buffer + offset, &msg_id, sizeof(msg_id));
    offset += sizeof(msg_id);
    memcpy(buffer + offset, &msg_type, sizeof(msg_type));
    offset += sizeof(msg_type);
    memcpy(buffer + offset, &op_type, sizeof(op_type));
    offset += sizeof(op_type);
    memcpy(buffer + offset, &beacon_cnt, sizeof(beacon_cnt));
    offset += sizeof(beacon_cnt);
    memcpy(buffer + offset, &actual_payload_length, sizeof(actual_payload_length));
    offset += sizeof(actual_payload_length);
    memcpy(buffer + offset, payload, actual_payload_length);
    offset += actual_payload_length;
    return offset;
}

bool InnerPlaintext::deserialize(const uint8_t* buffer, size_t len) {
    size_t expected_min_len = sizeof(msg_id) + sizeof(msg_type) + sizeof(op_type) +
                              sizeof(beacon_cnt) + sizeof(actual_payload_length);
    if (len < expected_min_len)
        return false;

    size_t offset = 0;
    memcpy(&msg_id, buffer + offset, sizeof(msg_id));
    offset += sizeof(msg_id);
    memcpy(&msg_type, buffer + offset, sizeof(msg_type));
    offset += sizeof(msg_type);
    memcpy(&op_type, buffer + offset, sizeof(op_type));
    offset += sizeof(op_type);
    memcpy(&beacon_cnt, buffer + offset, sizeof(beacon_cnt));
    offset += sizeof(beacon_cnt);
    memcpy(&actual_payload_length, buffer + offset, sizeof(actual_payload_length));
    offset += sizeof(actual_payload_length);

    if (len < expected_min_len + actual_payload_length)
        return false;  // Check total length
    if (actual_payload_length > sizeof(payload))
        return false;  // Check buffer overflow

    memcpy(payload, buffer + offset, actual_payload_length);
    return true;
}

// --- EncryptedMessage Implementation ---
EncryptedMessage::EncryptedMessage() : ciphertext_with_tag_len(0) {
    memset(nonce, 0, sizeof(nonce));
    memset(ciphertext_with_tag, 0, sizeof(ciphertext_with_tag));
    beacon_id_ad = 0;
}

bool EncryptedMessage::seal(const InnerPlaintext& inner_pt, uint32_t sender_beacon_id_ad,
                            const uint8_t my_x25519_sk[POL_X25519_SK_SIZE],
                            const uint8_t their_x25519_pk[POL_X25519_PK_SIZE]) {
    this->beacon_id_ad = sender_beacon_id_ad;

    // Derive shared key
    uint8_t shared_key[POL_SHARED_KEY_SIZE];
    if (!deriveAEADSharedKey(shared_key, my_x25519_sk, their_x25519_pk)) {
        Serial.println("[EncMsg] Seal: Failed to derive shared key.");
        return false;
    }

    // Generate a unique 12-byte nonce for this message
    //    IMPORTANT: This nonce MUST be unique for each message encrypted with the same key.
    //    For a beacon, a simple incrementing counter stored in NVS
    //    could work, or random bytes. For PoC, random is fine.
    randombytes_buf(this->nonce, POL_AEAD_NONCE_SIZE);

    // Serialize InnerPlaintext
    uint8_t inner_plaintext_buffer[MAX_INNER_PLAINTEXT_SIZE];
    size_t inner_plaintext_len = inner_pt.serialize(inner_plaintext_buffer);
    if (inner_plaintext_len == 0 || inner_plaintext_len > MAX_INNER_PLAINTEXT_SIZE) {
        Serial.println("[EncMsg] Seal: Inner plaintext serialization error or too large.");
        return false;
    }

    // Prepare Associated Data (beacon_id)
    uint8_t ad_buffer[sizeof(uint32_t)];
    memcpy(ad_buffer, &this->beacon_id_ad, sizeof(this->beacon_id_ad));

    // Encrypt
    if (!encryptAEAD(this->ciphertext_with_tag, this->ciphertext_with_tag_len,
                     inner_plaintext_buffer, inner_plaintext_len, ad_buffer, sizeof(ad_buffer),
                     this->nonce, shared_key)) {
        Serial.println("[EncMsg] Seal: AEAD encryption failed.");
        return false;
    }

    return true;
}

bool EncryptedMessage::unseal(InnerPlaintext& inner_pt_out,
                              const uint8_t my_x25519_sk[POL_X25519_SK_SIZE],
                              const uint8_t their_x25519_pk[POL_X25519_PK_SIZE]) {
    // Assumes beacon_id_ad, nonce, ciphertext_with_tag, and ciphertext_with_tag_len
    // have been populated by fromBytes()

    // Derive shared key
    uint8_t shared_key[POL_SHARED_KEY_SIZE];
    if (!deriveAEADSharedKey(shared_key, my_x25519_sk, their_x25519_pk)) {
        Serial.println("[EncMsg] Unseal: Failed to derive shared key.");
        return false;
    }

    // Prepare Associated Data
    uint8_t ad_buffer[sizeof(uint32_t)];
    memcpy(ad_buffer, &this->beacon_id_ad, sizeof(this->beacon_id_ad));

    // Decrypt
    uint8_t decrypted_inner_plaintext_buffer[MAX_INNER_PLAINTEXT_SIZE];
    size_t decrypted_inner_plaintext_len = 0;

    if (!decryptAEAD(decrypted_inner_plaintext_buffer, decrypted_inner_plaintext_len,
                     this->ciphertext_with_tag, this->ciphertext_with_tag_len, ad_buffer,
                     sizeof(ad_buffer), this->nonce, shared_key)) {
        Serial.println("[EncMsg] Unseal: AEAD decryption failed (bad tag or data).");
        return false;
    }

    // Deserialize InnerPlaintext
    if (!inner_pt_out.deserialize(decrypted_inner_plaintext_buffer,
                                  decrypted_inner_plaintext_len)) {
        Serial.println("[EncMsg] Unseal: Failed to deserialize inner plaintext.");
        return false;
    }
    return true;
}

size_t EncryptedMessage::packedSize() const {
    return sizeof(beacon_id_ad) + POL_AEAD_NONCE_SIZE + ciphertext_with_tag_len;
}

size_t EncryptedMessage::toBytes(uint8_t* buffer, size_t buffer_max_len) const {
    size_t total_len = packedSize();
    if (buffer_max_len < total_len)
        return 0;  // Not enough space

    size_t offset = 0;
    memcpy(buffer + offset, &beacon_id_ad, sizeof(beacon_id_ad));
    offset += sizeof(beacon_id_ad);
    memcpy(buffer + offset, nonce, POL_AEAD_NONCE_SIZE);
    offset += POL_AEAD_NONCE_SIZE;
    memcpy(buffer + offset, ciphertext_with_tag, ciphertext_with_tag_len);
    offset += ciphertext_with_tag_len;
    return offset;
}

bool EncryptedMessage::fromBytes(const uint8_t* data, size_t len) {
    size_t min_header_len = sizeof(beacon_id_ad) + POL_AEAD_NONCE_SIZE;
    if (len < min_header_len)
        return false;

    size_t offset = 0;
    memcpy(&beacon_id_ad, data + offset, sizeof(beacon_id_ad));
    offset += sizeof(beacon_id_ad);
    memcpy(nonce, data + offset, POL_AEAD_NONCE_SIZE);
    offset += POL_AEAD_NONCE_SIZE;

    this->ciphertext_with_tag_len = len - offset;
    if (this->ciphertext_with_tag_len > MAX_CIPHERTEXT_WITH_TAG_SIZE)
        return false;  // Too large for buffer

    memcpy(ciphertext_with_tag, data + offset, this->ciphertext_with_tag_len);
    return true;
}