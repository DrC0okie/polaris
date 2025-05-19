
#ifndef ENCRYPTED_MESSAGE_H
#define ENCRYPTED_MESSAGE_H

#include <stddef.h>
#include <stdint.h>

#include "../pol_constants.h"

// Max size for ciphertext + tag
#define MAX_CIPHERTEXT_WITH_TAG_SIZE (MAX_INNER_PLAINTEXT_SIZE + POL_AEAD_TAG_SIZE)

// Represents the structure that gets encrypted (the "plaintext" for AEAD)
struct InnerPlaintext {
    uint32_t msgId;
    uint8_t msgType;
    uint8_t opType;
    uint32_t beaconCnt;
    uint16_t actualPayloadLength;  // Length of the 'payload' field below
    uint8_t payload[MAX_INNER_PLAINTEXT_SIZE - sizeof(uint32_t) - sizeof(uint8_t) -
                    sizeof(uint8_t) - sizeof(uint32_t) - sizeof(uint16_t)];

    size_t serialize(uint8_t* buffer) const;
    bool deserialize(const uint8_t* buffer, size_t len);
    size_t getTotalSerializedSize() const;  // Calculates size based on actualPayloadLength
};

// Represents the full encrypted message sent over BLE
class EncryptedMessage {
public:
    uint32_t beaconIdAd;                                      // Associated Data (sent in clear)
    uint8_t nonce[POL_AEAD_NONCE_SIZE];                       // 12-byte Nonce (sent in clear)
    uint8_t ciphertextWithTag[MAX_CIPHERTEXT_WITH_TAG_SIZE];  // Ciphertext + Tag
    size_t ciphertextWithTagLen;                              // Actual length of ciphertextWithTag

    EncryptedMessage();

    // Prepare message for sending: encrypts innerPt, populates fields
    bool seal(const InnerPlaintext& innerPt, uint32_t senderBeaconIdAd,
              const uint8_t sharedKey[SHARED_KEY_SIZE]);

    // Process received message: decrypts, populates innerPt if successful
    bool unseal(InnerPlaintext& innerPtOut, const uint8_t sharedKey[SHARED_KEY_SIZE]);

    // Serialize the entire EncryptedMessage to a buffer for sending over BLE
    // Returns total bytes written or 0 on error
    size_t toBytes(uint8_t* buffer, size_t bufferMaxLen) const;

    // Deserialize an EncryptedMessage from a buffer received over BLE
    // Returns true on success, false on error (e.g., insufficient length)
    bool fromBytes(const uint8_t* data, size_t len);

    // Helper to get total packed size for current message
    size_t packedSize() const;
};

#endif  // ENCRYPTED_MESSAGE_H
