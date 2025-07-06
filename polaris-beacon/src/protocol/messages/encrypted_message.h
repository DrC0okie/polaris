
#ifndef ENCRYPTED_MESSAGE_H
#define ENCRYPTED_MESSAGE_H

#include <stddef.h>
#include <stdint.h>

#include "../../utils/crypto_service.h"
#include "../pol_constants.h"

/// @brief The maximum possible size of the encrypted portion of a message.
#define MAX_CIPHERTEXT_WITH_TAG_SIZE (MAX_INNER_PLAINTEXT_SIZE + POL_AEAD_TAG_SIZE)

/**
 * @struct InnerPlaintext
 * @brief The actual data structure that is encrypted and sent over the air.
 *
 * This struct is marked as `packed` to ensure its binary representation is
 * consistent across platforms, without compiler-injected padding bytes.
 */
struct __attribute__((packed)) InnerPlaintext {
    /// @brief A unique identifier for this specific message.
    uint32_t msgId;

    /// @brief The type of this message (e.g., REQ, ACK, ERR).
    uint8_t msgType;

    /// @brief The specific operation this message pertains to.
    uint8_t opType;

    /// @brief The beacon counter value at the time of message creation.
    uint32_t beaconCnt;

    /// @brief The actual length of the variable-size `payload` field.
    uint16_t actualPayloadLength;

    /// @brief A variable-length payload, typically containing JSON-formatted parameters.
    uint8_t payload[MAX_INNER_PLAINTEXT_SIZE - sizeof(uint32_t) - sizeof(uint8_t) -
                    sizeof(uint8_t) - sizeof(uint32_t) - sizeof(uint16_t)];
    /** @brief Serializes the struct into a byte buffer. */
    size_t serialize(uint8_t* buffer) const;

    /** @brief Deserializes a byte buffer into the struct fields. */
    bool deserialize(const uint8_t* buffer, size_t len);

    /** @brief Calculates the total serialized size based on the current payload length. */
    size_t getTotalSerializedSize() const;
};

/**
 * @class EncryptedMessage
 * @brief Represents the full message, including encrypted and unencrypted parts.
 *
 * This class encapsulates the logic for sealing (encrypting) an `InnerPlaintext`
 * into a transmittable format and unsealing (decrypting) a received buffer back
 * into an `InnerPlaintext`.
 */
class EncryptedMessage {
public:
    /// @brief The beacon ID, sent as unencrypted Associated Data.
    uint32_t beaconIdAd;

    /// @brief A 12-byte nonce, unique per message, sent in the clear.
    uint8_t nonce[POL_AEAD_NONCE_SIZE];

    /// @brief Buffer containing the ciphertext and authentication tag.
    uint8_t ciphertextWithTag[MAX_CIPHERTEXT_WITH_TAG_SIZE];

    /// @brief The actual length of the data in the `ciphertextWithTag` buffer.
    size_t ciphertextWithTagLen;

    /**
     * @brief Constructs an EncryptedMessage.
     * @param cryptoService A reference to the service used for encryption/decryption.
     */
    EncryptedMessage(const CryptoService& cryptoService);

    /**
     * @brief Encrypts an InnerPlaintext struct and populates the EncryptedMessage fields.
     * @param innerPt The plaintext data to seal.
     * @param senderBeaconIdAd The ID of the beacon to include as Associated Data.
     * @return True on successful encryption.
     */
    bool seal(const InnerPlaintext& innerPt, uint32_t senderBeaconIdAd);

    /**
     * @brief Decrypts the messag ciphertext and populates a given InnerPlaintext struct.
     * @param innerPtOut The InnerPlaintext struct to populate with decrypted data.
     * @param Optionnal. Overrides the key stored in the KeyManager to unseal
     * @return True on successful decryption and authentication.
     */
    bool unseal(InnerPlaintext& innerPtOut, const uint8_t* overrideKey = nullptr);

    /**
     * @brief Serializes the entire message (header + ciphertext) to a buffer for transmission.
     * @param buffer The output buffer.
     * @param bufferMaxLen The maximum size of the output buffer.
     * @return The total number of bytes written to the buffer.
     */
    size_t toBytes(uint8_t* buffer, size_t bufferMaxLen) const;

    /**
     * @brief Deserializes a raw byte buffer from BLE into the EncryptedMessage fields.
     * @param data The incoming raw data buffer.
     * @param len The length of the data buffer.
     * @return True on successful parsing.
     */
    bool fromBytes(const uint8_t* data, size_t len);

    /**
     * @brief Gets the total packed size of the current message for serialization.
     * @return The total size in bytes.
     */
    size_t packedSize() const;

private:
    /// @brief A reference to the cryptographic service provider.
    const CryptoService& _cryptoService;
};

#endif  // ENCRYPTED_MESSAGE_H
