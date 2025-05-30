#include "pol_response.h"

#include <string.h>

size_t PoLResponse::getSignedSize() const {
    return SIGNED_SIZE;
}

bool PoLResponse::fromBytes(const uint8_t* data, size_t len) {
    if (len < packedSize())
        return false;

    size_t offset = 0;

    flags = data[offset++];
    memcpy(&beaconId, data + offset, sizeof(beaconId));
    offset += sizeof(beaconId);

    memcpy(&counter, data + offset, sizeof(counter));
    offset += sizeof(counter);

    memcpy(nonce, data + offset, PROTOCOL_NONCE_SIZE);
    offset += PROTOCOL_NONCE_SIZE;

    memcpy(beaconSig, data + offset, SIG_SIZE);
    return true;
}

void PoLResponse::toBytes(uint8_t* out) const {
    size_t offset = 0;

    out[offset++] = flags;
    memcpy(out + offset, &beaconId, sizeof(beaconId));
    offset += sizeof(beaconId);

    memcpy(out + offset, &counter, sizeof(counter));
    offset += sizeof(counter);

    memcpy(out + offset, nonce, PROTOCOL_NONCE_SIZE);
    offset += PROTOCOL_NONCE_SIZE;

    memcpy(out + offset, beaconSig, SIG_SIZE);
}

void PoLResponse::getSignedData(uint8_t* out, const PoLRequest& originalReq) const {
    size_t offset = 0;

    // Response fields
    out[offset++] = flags;
    memcpy(out + offset, &beaconId, sizeof(beaconId));
    offset += sizeof(beaconId);
    memcpy(out + offset, &counter, sizeof(counter));
    offset += sizeof(counter);
    memcpy(out + offset, nonce, PROTOCOL_NONCE_SIZE);  // This nonce is from originalReq.nonce
    offset += PROTOCOL_NONCE_SIZE;

    // Request fields
    memcpy(out + offset, &originalReq.phoneId, sizeof(originalReq.phoneId));
    offset += sizeof(originalReq.phoneId);
    memcpy(out + offset, originalReq.phonePk, Ed25519_PK_SIZE);
    offset += Ed25519_PK_SIZE;
    memcpy(out + offset, originalReq.phoneSig, SIG_SIZE);
    offset += SIG_SIZE;
}
