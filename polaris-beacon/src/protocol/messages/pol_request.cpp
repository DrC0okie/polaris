#include "pol_request.h"

#include <string.h>

size_t PoLRequest::getSignedSize() const {
    return SIGNED_SIZE;
}

bool PoLRequest::fromBytes(const uint8_t* data, size_t len) {
    if (len < PoLRequest::packedSize())
        return false;

    size_t offset = 0;

    // Parse each field from the binary buffer in order
    flags = data[offset++];
    memcpy(&phoneId, data + offset, sizeof(phoneId));
    offset += sizeof(phoneId);

    memcpy(&beaconId, data + offset, sizeof(beaconId));
    offset += sizeof(beaconId);

    memcpy(nonce, data + offset, PROTOCOL_NONCE_SIZE);
    offset += PROTOCOL_NONCE_SIZE;

    memcpy(phonePk, data + offset, Ed25519_PK_SIZE);
    offset += Ed25519_PK_SIZE;

    memcpy(phoneSig, data + offset, SIG_SIZE);
    // offset += SIG_SIZE; // Optional, not needed here

    return true;
}

void PoLRequest::toBytes(uint8_t* out) const {
    size_t offset = 0;

    out[offset++] = flags;
    memcpy(out + offset, &phoneId, sizeof(phoneId));
    offset += sizeof(phoneId);

    memcpy(out + offset, &beaconId, sizeof(beaconId));
    offset += sizeof(beaconId);

    memcpy(out + offset, nonce, PROTOCOL_NONCE_SIZE);
    offset += PROTOCOL_NONCE_SIZE;

    memcpy(out + offset, phonePk, Ed25519_PK_SIZE);
    offset += Ed25519_PK_SIZE;

    memcpy(out + offset, phoneSig, SIG_SIZE);
}

void PoLRequest::getSignedData(uint8_t* out) const {
    if (!out)
        return;
    size_t offset = 0;

    out[offset++] = flags;
    memcpy(out + offset, &phoneId, sizeof(phoneId));
    offset += sizeof(phoneId);

    memcpy(out + offset, &beaconId, sizeof(beaconId));
    offset += sizeof(beaconId);

    memcpy(out + offset, nonce, PROTOCOL_NONCE_SIZE);
    offset += PROTOCOL_NONCE_SIZE;

    memcpy(out + offset, phonePk, Ed25519_PK_SIZE);
}
