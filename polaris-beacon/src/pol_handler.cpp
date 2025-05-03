#include "pol_handler.h"
#include "crypto.h"
#include <string.h>

PoLRequestHandler::PoLRequestHandler(uint32_t beacon_id, const uint8_t sk[32], const uint8_t pk[32], MinuteCounter& counter)
    : _beacon_id(beacon_id), _counter(counter) {
    memcpy(_sk, sk, 32);
    memcpy(_pk, pk, 32);
}

bool PoLRequestHandler::handle(const uint8_t* data, size_t len, PoLResponse& outResponse) {
    if (len != PoLRequest::packedSize()) {
        Serial.println("[Handler] Invalid request length");
        return false;
    }

    PoLRequest req;
    if (!req.fromBytes(data, len)) {
        Serial.println("[Handler] Invalid request format");
        return false;
    }

    if (!verifyPoLRequestSignature(req)) {
        Serial.println("[Handler] Signature verification failed");
        return false;
    }

    Serial.println("[Handler] PoLRequest is valid");

    outResponse.flags = 0xB1;
    outResponse.beacon_id = _beacon_id;
    outResponse.counter = _counter.getValue();
    memcpy(outResponse.nonce, req.nonce, POL_NONCE_SIZE);

    signPoLResponse(outResponse, _sk, _pk);
    return true;
}
