#ifndef POL_REQUEST_H
#define POL_REQUEST_H

#include <stddef.h>
#include <stdint.h>

#include "../pol_constants.h"

/**
 * @class PoLRequest
 * @brief Represents an unencrypted PoL request from a client.
 *
 * This struct defines the fixed-size binary layout for a PoL token request.
 */
class PoLRequest {
public:
    /// @brief Bit flags for message options. Currently unused.
    uint8_t flags;

    /// @brief A unique identifier for the requesting phone/client.
    uint64_t phoneId;

    /// @brief The ID of the beacon this request is intended for.
    uint32_t beaconId;

    /// @brief A random nonce to prevent replay attacks.
    uint8_t nonce[PROTOCOL_NONCE_SIZE];

    /// @brief The Ed25519 public key of the requesting phone.
    uint8_t phonePk[Ed25519_PK_SIZE];

    /// @brief The Ed25519 signature of the preceding fields, created by the phone.
    uint8_t phoneSig[SIG_SIZE];

    /**
     * @brief Parses a PoLRequest from a raw byte buffer.
     * @param data The buffer containing the serialized request.
     * @param len The length of the buffer.
     * @return True if parsing was successful, false if length is incorrect.
     */
    bool fromBytes(const uint8_t* data, size_t len);

    /**
     * @brief Serializes the PoLRequest object into a raw byte buffer.
     * @param out The output buffer to write the serialized data into.
     */
    void toBytes(uint8_t* out) const;

    /**
     * @brief The size in bytes of the portion of the message that is signed.
     */
    static constexpr size_t SIGNED_SIZE = sizeof(uint8_t) + sizeof(uint64_t) + sizeof(uint32_t) +
                                          PROTOCOL_NONCE_SIZE + Ed25519_PK_SIZE;

    /**
     * @brief The total size in bytes of the serialized message.
     */
    static constexpr size_t packedSize() {
        return SIGNED_SIZE + SIG_SIZE;
    }

    size_t getSignedSize() const;

    /**
     * @brief Copies the signable data (all fields except `phoneSig`) into a buffer.
     * @param out The output buffer for the signable data.
     */
    void getSignedData(uint8_t* out) const;
};

#endif  // POL_REQUEST_H
