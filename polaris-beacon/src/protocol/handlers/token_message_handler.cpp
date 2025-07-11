#include "token_message_handler.h"

#include <HardwareSerial.h>

#include "../messages/pol_request.h"
#include "../messages/pol_response.h"

TokenMessageHandler::TokenMessageHandler(const CryptoService& cryptoService,
                                         const BeaconCounter& counter, IMessageTransport& transport)
    : _cryptoService(cryptoService), _counter(counter), _transport(transport) {
}

void TokenMessageHandler::process(const uint8_t* data, size_t len) {
    uint8_t buffer[PoLResponse::packedSize()];

    if (len != PoLRequest::packedSize()) {
        Serial.printf("[Processor] Invalid length. Got %zu, expected %zu\n", len,
                      PoLRequest::packedSize());
        return;
    }

    PoLRequest req;
    if (!req.fromBytes(data, len)) {
        Serial.println("[Processor] Invalid format");

        // Send a blank response signifying an error
        _transport.sendMessage(buffer, sizeof(buffer));
    }

    Serial.println("[processor] Verifiying signatures");
    if (!_cryptoService.verifyPoLRequestSignature(req)) {
        Serial.println("[Processor] Invalid signature");

        // Send a blank response signifying an error
        _transport.sendMessage(buffer, sizeof(buffer));
    }

    Serial.println("[Processor] Valid request signature");

    PoLResponse resp;
    resp.flags = 0x00;
    resp.beaconId = BEACON_ID;
    resp.counter = _counter.getValue();
    memcpy(resp.nonce, req.nonce, PROTOCOL_NONCE_SIZE);  // Echo the nonce

    // Sign the response, including context from the original request
    _cryptoService.signPoLResponse(resp, req);
    resp.toBytes(buffer);

    // Delegate sending the full message to the transport layer
    if (!_transport.sendMessage(buffer, sizeof(buffer))) {
        Serial.println("[Processor] Failed to send response via transport layer.");
    }

    Serial.println("[Processor] Response dispatched to transport layer.");
}