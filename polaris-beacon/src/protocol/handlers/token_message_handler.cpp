#include "token_message_handler.h"

#include <HardwareSerial.h>

#include "../messages/pol_request.h"
#include "../messages/pol_response.h"

TokenMessageHandler::TokenMessageHandler(const CryptoService& cryptoService,
                                         const MinuteCounter& counter,
                                         BLECharacteristic* indicationChar)
    : _cryptoService(cryptoService), _counter(counter), _indicateChar(indicationChar) {
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
    if (!_cryptoService.verifyPoLRequestSignature(req)) {
        Serial.println("[Processor] Invalid signature");
        return;
    }

    Serial.println("[Processor] Valid request signature");

    PoLResponse resp;
    resp.flags = 0x00;
    resp.beaconId = BEACON_ID;
    resp.counter = _counter.getValue();
    memcpy(resp.nonce, req.nonce, PROTOCOL_NONCE_SIZE);  // Echo the nonce

    // Sign the response, including context from the original request
    _cryptoService.signPoLResponse(resp, req);

    uint8_t buffer[PoLResponse::packedSize()];
    resp.toBytes(buffer);

    _indicateChar->setValue(buffer, sizeof(buffer));
    _indicateChar->indicate();

    Serial.println("[Processor] Response sent");
}