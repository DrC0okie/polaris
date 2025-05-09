#include "pol_response.h"
#include <string.h>

size_t PoLResponse::getSignedSize() const {
    return SIGNED_SIZE;
}

bool PoLResponse::fromBytes(const uint8_t* data, size_t len) {
    if (len < packedSize()) return false;

    size_t offset = 0;

    flags = data[offset++];
    memcpy(&beacon_id, data + offset, sizeof(beacon_id));
    offset += sizeof(beacon_id);

    memcpy(&counter, data + offset, sizeof(counter));
    offset += sizeof(counter);

    memcpy(nonce, data + offset, POL_PROTOCOL_NONCE_SIZE);
    offset += POL_PROTOCOL_NONCE_SIZE;

    memcpy(beacon_sig, data + offset, POL_SIG_SIZE);
    return true;
}

void PoLResponse::toBytes(uint8_t* out) const {
    size_t offset = 0;

    out[offset++] = flags;
    memcpy(out + offset, &beacon_id, sizeof(beacon_id));
    offset += sizeof(beacon_id);

    memcpy(out + offset, &counter, sizeof(counter));
    offset += sizeof(counter);

    memcpy(out + offset, nonce, POL_PROTOCOL_NONCE_SIZE);
    offset += POL_PROTOCOL_NONCE_SIZE;

    memcpy(out + offset, beacon_sig, POL_SIG_SIZE);
}

void PoLResponse::getSignedData(uint8_t* out) const {
    size_t offset = 0;

    out[offset++] = flags;
    memcpy(out + offset, &beacon_id, sizeof(beacon_id));
    offset += sizeof(beacon_id);

    memcpy(out + offset, &counter, sizeof(counter));
    offset += sizeof(counter);

    memcpy(out + offset, nonce, POL_PROTOCOL_NONCE_SIZE);
}
