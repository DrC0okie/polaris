#include "pol_request_processor.h"

#include <HardwareSerial.h>

#include "crypto.h"
#include "pol_request.h"
#include "pol_response.h"

PoLRequestProcessor::PoLRequestProcessor(uint32_t beacon_id,
                                         const uint8_t sk[32],
                                         const uint8_t pk[32],
                                         MinuteCounter& counter,
                                         BLECharacteristic* indicationChar)
    : _beacon_id(beacon_id), _counter(counter), _indicateChar(indicationChar) {
    memcpy(_sk, sk, 32);
    memcpy(_pk, pk, 32);
}

void PoLRequestProcessor::process(const uint8_t* data, size_t len) {
    if (len != PoLRequest::packedSize()) {
        Serial.println("[Processor] Invalid length");
        return;
    }

    PoLRequest req;
    if (!req.fromBytes(data, len)) {
        Serial.println("[Processor] Invalid format");
        return;
    }

    if (!verifyPoLRequestSignature(req)) {
        Serial.println("[Processor] Invalid signature");
        return;
    }

    Serial.println("[Processor] Valid request signature");

    PoLResponse resp;
    resp.flags = 0xB1;
    resp.beacon_id = _beacon_id;
    resp.counter = _counter.getValue();
    memcpy(resp.nonce, req.nonce, POL_NONCE_SIZE);
    signPoLResponse(resp, _sk, _pk);

    uint8_t buffer[PoLResponse::packedSize()];
    resp.toBytes(buffer);

    _indicateChar->setValue(buffer, sizeof(buffer));
    _indicateChar->indicate();

    Serial.println("[Processor] Response sent");
}