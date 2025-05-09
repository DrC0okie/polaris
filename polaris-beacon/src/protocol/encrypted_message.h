
#ifndef ENCRYPTED_MESSAGE_H
#define ENCRYPTED_MESSAGE_H

#include <stddef.h>
#include <stdint.h>

#include "pol_constants.h"

// Max size for the inner plaintext
#define MAX_INNER_PLAINTEXT_SIZE 200
// Max size for ciphertext + tag
#define MAX_CIPHERTEXT_WITH_TAG_SIZE (MAX_INNER_PLAINTEXT_SIZE + POL_AEAD_TAG_SIZE)

// Represents the structure that gets encrypted (the "plaintext" for AEAD)
struct InnerPlaintext {
    uint32_t msg_id;
    uint8_t msg_type;
    uint8_t op_type;
    uint32_t beacon_cnt;
    uint16_t actual_payload_length;  // Length of the 'payload' field below
    uint8_t payload[MAX_INNER_PLAINTEXT_SIZE - sizeof(uint32_t) - sizeof(uint8_t) -
                    sizeof(uint8_t) - sizeof(uint32_t) - sizeof(uint16_t)];

    size_t serialize(uint8_t* buffer) const;
    bool deserialize(const uint8_t* buffer, size_t len);
    size_t getTotalSerializedSize() const;  // Calculates size based on actual_payload_length
};

// Represents the full encrypted message sent over BLE
// Format: beacon_id (AD) || nonce || (ciphertext || tag)
class EncryptedMessage {
public:
    uint32_t beacon_id_ad;                                      // Associated Data (sent in clear)
    uint8_t nonce[POL_AEAD_NONCE_SIZE];                         // 12-byte Nonce (sent in clear)
    uint8_t ciphertext_with_tag[MAX_CIPHERTEXT_WITH_TAG_SIZE];  // Ciphertext + Tag
    size_t ciphertext_with_tag_len;  // Actual length of ciphertext_with_tag

    EncryptedMessage();

    // Prepare message for sending: encrypts inner_pt, populates fields
    bool seal(const InnerPlaintext& inner_pt,
              uint32_t sender_beacon_id_ad,  // For AD
              const uint8_t my_x25519_sk[POL_X25519_SK_SIZE],
              const uint8_t their_x25519_pk[POL_X25519_PK_SIZE]);

    // Process received message: decrypts, populates inner_pt if successful
    bool unseal(InnerPlaintext& inner_pt_out,  // Output for decrypted data
                const uint8_t my_x25519_sk[POL_X25519_SK_SIZE],
                const uint8_t their_x25519_pk[POL_X25519_PK_SIZE]);

    // Serialize the entire EncryptedMessage to a buffer for sending over BLE
    // Returns total bytes written or 0 on error
    size_t toBytes(uint8_t* buffer, size_t buffer_max_len) const;

    // Deserialize an EncryptedMessage from a buffer received over BLE
    // Returns true on success, false on error (e.g., insufficient length)
    bool fromBytes(const uint8_t* data, size_t len);

    // Helper to get total packed size for current message
    size_t packedSize() const;
};

#endif  // ENCRYPTED_MESSAGE_H
