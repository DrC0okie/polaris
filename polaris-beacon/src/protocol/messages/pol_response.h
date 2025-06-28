#ifndef POL_RESPONSE_H
#define POL_RESPONSE_H

#include <stddef.h>
#include <stdint.h>

#include "../pol_constants.h"
#include "pol_request.h"

/**
 * @class PoLResponse
 * @brief Represents an unencrypted PoL response from the beacon.
 *
 * This struct defines the fixed-size binary layout for a PoL token response.
 */
class PoLResponse {
public:
    /// @brief Bit flags for message options. Currently unused.
    uint8_t flags;

    /// @brief The ID of this beacon.
    uint32_t beaconId;

    /// @brief The beacon current monotonic counter value.
    uint64_t counter;

    /// @brief The nonce echoed back from the original PoLRequest.
    uint8_t nonce[PROTOCOL_NONCE_SIZE];

    /// @brief The beacon Ed25519 signature over the response and parts of the original request.
    uint8_t beaconSig[SIG_SIZE];

    /**
     * @brief Parses a PoLResponse from a raw byte buffer.
     * @param data The buffer containing the serialized response.
     * @param len The length of the buffer.
     * @return True if parsing was successful, false if length is incorrect.
     */
    bool fromBytes(const uint8_t* data, size_t len);

    /**
     * @brief Serializes the PoLResponse object into a raw byte buffer.
     * @param out The output buffer to write the serialized data into.
     */
    void toBytes(uint8_t* out) const;

    /**
     * @brief The total size in bytes of the portion of the message that is signed.
     *
     * Note: The signature covers the response fields plus context from the original request.
     */
    static constexpr size_t SIGNED_SIZE = sizeof(uint8_t)        // flags
                                          + sizeof(uint32_t)     // beacon id
                                          + sizeof(uint64_t)     // beacon counter
                                          + PROTOCOL_NONCE_SIZE  // echo nonce
                                          + sizeof(uint64_t)     // request phoneId
                                          + Ed25519_PK_SIZE      // request phonePk
                                          + SIG_SIZE;            // request phone signature

    /**
     * @brief The total size in bytes of the serialized message.
     */
    static constexpr size_t packedSize() {
        return sizeof(uint8_t)        // flags
               + sizeof(uint32_t)     // beacon id
               + sizeof(uint64_t)     // beacon counter
               + PROTOCOL_NONCE_SIZE  // echo nonce
               + SIG_SIZE;            // response beacon signature
    }

    size_t getSignedSize() const;

    /**
     * @brief Copies the signable data into a buffer for cryptographic signing.
     *
     * The data includes fields from this response and is contextualized with
     * fields from the original request to prevent replay and manipulation.
     * @param out The output buffer for the signable data.
     * @param originalReq The original PoLRequest that this response is for.
     */
    void getSignedData(uint8_t* out, const PoLRequest& originalReq) const;
};

#endif  // POL_RESPONSE_H
