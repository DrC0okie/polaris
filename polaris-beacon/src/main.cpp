#include <Arduino.h>

#include "ble/ble_server.h"
#include "utils/counter.h"
#include "protocol/crypto.h"
#include "protocol/pol_request_processor.h"

MinuteCounter counter;
BleServer server(PoLRequest::packedSize());

uint8_t sk[32], pk[32];
const uint32_t BEACON_ID = 1;

void setup() {
    Serial.begin(115200);

    counter.begin();
    Serial.println("Counter initialized");
    generateKeyPair(pk, sk);
    Serial.println("Key par created");

    server.begin();

    auto* indicationChar = server.getIndicationCharacteristic();
    if (!indicationChar) {
        Serial.println("[MAIN] Could not get indication characteristic");
        return;
    }

    server.setRequestProcessor(
        new PoLRequestProcessor(BEACON_ID, sk, pk, counter, indicationChar));

    Serial.println("BLE Server started");
}

void loop() {
    // Nothing needed
}