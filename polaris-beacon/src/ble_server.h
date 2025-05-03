#ifndef BLE_SERVER_H
#define BLE_SERVER_H

#include <Arduino.h>

void setupBLE();
bool isDeviceConnected();
void sendIndication(const std::string& message);

#endif