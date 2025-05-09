#ifndef POL_REQUEST_H
#define POL_REQUEST_H

#include <stddef.h>
#include <stdint.h>

#include "pol_constants.h"

// Represents a proof-of-location request sent by a phone
class PoLRequest {
public:
    // Message fields
    uint8_t flags;                    // Bit flags for message type or options
    uint64_t phone_id;                // Unique phone identifier
    uint32_t beacon_id;               // Intended beacon recipient
    uint8_t nonce[POL_PROTOCOL_NONCE_SIZE];    // Random nonce to prevent replay
    uint8_t phone_pk[POL_Ed25519_PK_SIZE];    // Phone's Ed25519 public key
    uint8_t phone_sig[POL_SIG_SIZE];  // Signature of the message

    // Parses fields from a raw binary buffer (BLE payload)
    // Returns false if the size is incorrect
    bool fromBytes(const uint8_t* data, size_t len);

    // Serializes the entire message to a buffer
    void toBytes(uint8_t* out) const;

    // size of the fields that are signed
    static constexpr size_t SIGNED_SIZE = sizeof(uint8_t) + sizeof(uint64_t) +
                                          sizeof(uint32_t) + POL_PROTOCOL_NONCE_SIZE +
                                          POL_Ed25519_PK_SIZE;

    // Returns the exact size of the message when serialized
    static constexpr size_t packedSize() {
        return SIGNED_SIZE + POL_SIG_SIZE;
    }

    size_t getSignedSize() const;

    // Copies only the fields that are signed into `out` buffer
    // Used by cryptographic verification (excludes `phone_sig`)
    void getSignedData(uint8_t* out) const;
};

#endif  // POL_REQUEST_H
