#ifndef POL_RESPONSE_H
#define POL_RESPONSE_H

#include <stddef.h>
#include <stdint.h>

#include "../pol_constants.h"

// Proof-of-Location response from the beacon
class PoLResponse {
public:
    uint8_t flags;                           // Message type flags
    uint32_t beacon_id;                      // ID of the responding beacon
    uint64_t counter;                        // Monotonic counter value
    uint8_t nonce[POL_PROTOCOL_NONCE_SIZE];  // Echoed nonce from the request
    uint8_t beacon_sig[POL_SIG_SIZE];        // Signature by beacon's private key

    // Deserialize from raw buffer
    bool fromBytes(const uint8_t* data, size_t len);

    // Serialize to raw buffer
    void toBytes(uint8_t* out) const;

    // size of the fields that are signed
    static constexpr size_t SIGNED_SIZE =
        sizeof(uint8_t) + sizeof(uint32_t) + sizeof(uint64_t) + POL_PROTOCOL_NONCE_SIZE;

    // Total packed size of the message
    static constexpr size_t packedSize() {
        return SIGNED_SIZE + POL_SIG_SIZE;
    }

    size_t getSignedSize() const;

    // Get the signed portion of the message (all fields except beacon_sig)
    void getSignedData(uint8_t* out) const;
};

#endif  // POL_RESPONSE_H
