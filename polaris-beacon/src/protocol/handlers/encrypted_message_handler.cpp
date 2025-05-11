#include "encrypted_message_handler.h"

#include <HardwareSerial.h>

#include "../crypto.h"
#include "../messages/encrypted_message.h"

EncryptedMessageHandler::EncryptedMessageHandler(BLECharacteristic* indicationChar)
    : _indicateChar(indicationChar) {
}

void EncryptedMessageHandler::process(const uint8_t* data, size_t len) {
    if (len >= 512) {
        Serial.println("[Encrypted processor] Invalid length");
        return;
    }

    EncryptedMessage msg;
    if (!msg.fromBytes(data, len)) {
        Serial.println("[Encrypted processor] Invalid format");
        return;
    }

    uint8_t buffer[len];
    msg.toBytes(buffer, len);

    _indicateChar->setValue(buffer, sizeof(buffer));
    _indicateChar->indicate();

    Serial.println("[Encrypted processor] Response sent");
}