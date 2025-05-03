#include "ble_server.h"
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>

// UUIDs Polaris
#define SERVICE_UUID "f44dce36-ffb2-565b-8494-25fa5a7a7cd6"
#define WRITE_CHAR_UUID "8e8c14b7-d9f0-5e5c-9da8-6961e1f33d6b"
#define INDICATE_CHAR_UUID "d234a7d8-ea1f-5299-8221-9cf2f942d3df"

static BLEServer* pServer = nullptr;
static BLECharacteristic* pIndicateCharacteristic = nullptr;
static bool deviceConnected = false;

class ServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer*) override {
    deviceConnected = true;
    Serial.println("Device connected");
  }
  void onDisconnect(BLEServer*) override {
    deviceConnected = false;
    Serial.println("Device disconnected");
    pServer->getAdvertising()->start();
  }
};

class WriteCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* pChar) override {
    std::string value = pChar->getValue();
    Serial.print("Received: ");
    Serial.println(value.c_str());
  }
};

void setupBLE() {
  BLEDevice::init("PoL beacon server");

  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new ServerCallbacks());

  BLEService* service = pServer->createService(SERVICE_UUID);

  auto pWrite = service->createCharacteristic(
    WRITE_CHAR_UUID, BLECharacteristic::PROPERTY_WRITE);
  pWrite->setCallbacks(new WriteCallbacks());

  pIndicateCharacteristic = service->createCharacteristic(
    INDICATE_CHAR_UUID, BLECharacteristic::PROPERTY_INDICATE);

  service->start();

  BLEAdvertising* advertising = BLEDevice::getAdvertising();
  advertising->addServiceUUID(SERVICE_UUID);
  advertising->setScanResponse(true);
  advertising->start();

  BLEDevice::setMTU(517);

  Serial.println("BLE server ready");
}

bool isDeviceConnected() {
  return deviceConnected;
}

void sendIndication(const std::string& message) {
  if (deviceConnected && pIndicateCharacteristic) {
    pIndicateCharacteristic->setValue(message);
    pIndicateCharacteristic->indicate();
  }
}
