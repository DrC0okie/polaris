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

MinuteCounter counter;

void setup(){
  Serial.begin(115200);
  counter.begin();
}

void loop(){
}