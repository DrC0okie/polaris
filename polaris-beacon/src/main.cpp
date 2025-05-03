// #include <Arduino.h>
// #include "ble_server.h"

// void setup() {
//   Serial.begin(115200);
//   setupBLE();
// }

// void loop() {
//   static unsigned long lastTime = 0;
//   if (millis() - lastTime > 5000) {
//     lastTime = millis();
//     Serial.println("Heartbeat");
//     if (isDeviceConnected()) {
//       sendIndication("Hello from Polaris");
//     }
//   }
// }

#include <Arduino.h>
#include "counter.h"
#include "crypto.h"
#include "ble_server.h"

MinuteCounter counter;
BleServer* server;

uint8_t sk[32], pk[32];

void setup() {
    Serial.begin(115200);

    counter.begin();
    Serial.println("Counter initialized");
    generateKeyPair(pk, sk);
    Serial.println("Key par created");

    static BleServer srv(0xBADC0DE, sk, pk, counter);
    server = &srv;
    server->begin();
    Serial.println("BLE Server started");
}

void loop() {
    // Nothing needed
}