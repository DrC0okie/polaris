#include "pol_request.h"
#include <string.h>

size_t PoLRequest::getSignedSize() const {
    return SIGNED_SIZE ;
}

bool PoLRequest::fromBytes(const uint8_t* data, size_t len) {
    if (len < PoLRequest::packedSize()) return false;

    size_t offset = 0;

    // Parse each field from the binary buffer in order
    flags = data[offset++];
    memcpy(&phone_id, data + offset, sizeof(phone_id));
    offset += sizeof(phone_id);

    memcpy(&beacon_id, data + offset, sizeof(beacon_id));
    offset += sizeof(beacon_id);

    memcpy(nonce, data + offset, POL_NONCE_SIZE);
    offset += POL_NONCE_SIZE;

    memcpy(phone_pk, data + offset, POL_PK_SIZE);
    offset += POL_PK_SIZE;

    memcpy(phone_sig, data + offset, POL_SIG_SIZE);
    // offset += POL_SIG_SIZE; // Optional, not needed here

    return true;
}

void PoLRequest::toBytes(uint8_t* out) const {
    size_t offset = 0;

    out[offset++] = flags;
    memcpy(out + offset, &phone_id, sizeof(phone_id));
    offset += sizeof(phone_id);

    memcpy(out + offset, &beacon_id, sizeof(beacon_id));
    offset += sizeof(beacon_id);

    memcpy(out + offset, nonce, POL_NONCE_SIZE);
    offset += POL_NONCE_SIZE;

    memcpy(out + offset, phone_pk, POL_PK_SIZE);
    offset += POL_PK_SIZE;

    memcpy(out + offset, phone_sig, POL_SIG_SIZE);
}

void PoLRequest::getSignedData(uint8_t* out) const {
    if (!out) return;
    size_t offset = 0;

    out[offset++] = flags;
    memcpy(out + offset, &phone_id, sizeof(phone_id));
    offset += sizeof(phone_id);

    memcpy(out + offset, &beacon_id, sizeof(beacon_id));
    offset += sizeof(beacon_id);

    memcpy(out + offset, nonce, POL_NONCE_SIZE);
    offset += POL_NONCE_SIZE;

    memcpy(out + offset, phone_pk, POL_PK_SIZE);
}
