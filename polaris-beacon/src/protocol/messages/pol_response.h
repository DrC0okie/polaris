#ifndef POL_RESPONSE_H
#define POL_RESPONSE_H

#include <stddef.h>
#include <stdint.h>

#include "../pol_constants.h"
#include "pol_request.h"

// Proof-of-Location response from the beacon
class PoLResponse {
public:
    uint8_t flags;                       // Message type flags
    uint32_t beaconId;                   // ID of the responding beacon
    uint64_t counter;                    // Monotonic counter value
    uint8_t nonce[PROTOCOL_NONCE_SIZE];  // Echoed nonce from the request
    uint8_t beaconSig[SIG_SIZE];         // Signature by beacon's private key

    // Deserialize from raw buffer
    bool fromBytes(const uint8_t* data, size_t len);

    // Serialize to raw buffer
    void toBytes(uint8_t* out) const;

    // size of the fields that are signed
    static constexpr size_t SIGNED_SIZE = sizeof(uint8_t)        // flags
                                          + sizeof(uint32_t)     // beacon id
                                          + sizeof(uint64_t)     // beacon counter
                                          + PROTOCOL_NONCE_SIZE  // echo nonce
                                          + sizeof(uint64_t)     // request phoneId
                                          + Ed25519_PK_SIZE      // request phonePk
                                          + SIG_SIZE;            // request phone signature

    // Total packed size of the message
    static constexpr size_t packedSize() {
        return sizeof(uint8_t)        // flags
               + sizeof(uint32_t)     // beacon id
               + sizeof(uint64_t)     // beacon counter
               + PROTOCOL_NONCE_SIZE  // echo nonce
               + SIG_SIZE;            // response beacon signature
    }

    size_t getSignedSize() const;

    // Get the signed portion of the message (all fields except beaconSig)
    void getSignedData(uint8_t* out, const PoLRequest& originalReq) const;
};

#endif  // POL_RESPONSE_H
