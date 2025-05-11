#include "token_message_handler.h"

#include <HardwareSerial.h>

#include "../crypto.h"
#include "../messages/pol_request.h"
#include "../messages/pol_response.h"

TokenMessageHandler::TokenMessageHandler(uint32_t beacon_id, const uint8_t sk[POL_Ed25519_SK_SIZE],
                                         MinuteCounter& counter, BLECharacteristic* indicationChar)
    : _beacon_id(beacon_id), _counter(counter), _indicateChar(indicationChar) {
    memcpy(_sk, sk, POL_Ed25519_SK_SIZE);
}

void TokenMessageHandler::process(const uint8_t* data, size_t len) {
    if (len != PoLRequest::packedSize()) {
        Serial.println("[Processor] Invalid length");
        return;
    }

    PoLRequest req;
    if (!req.fromBytes(data, len)) {
        Serial.println("[Processor] Invalid format");
        return;
    }

    Serial.println("[processor] Verifiying signatures");
    if (!verifyPoLRequestSignature(req)) {
        Serial.println("[Processor] Invalid signature");
        return;
    }

    Serial.println("[Processor] Valid request signature");

    PoLResponse resp;
    resp.flags = 0x01;
    resp.beacon_id = _beacon_id;
    resp.counter = _counter.getValue();
    memcpy(resp.nonce, req.nonce, POL_PROTOCOL_NONCE_SIZE);
    signPoLResponse(resp, _sk);

    uint8_t buffer[PoLResponse::packedSize()];
    resp.toBytes(buffer);

    _indicateChar->setValue(buffer, sizeof(buffer));
    _indicateChar->indicate();

    Serial.println("[Processor] Response sent");
}