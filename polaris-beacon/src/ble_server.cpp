#include "ble_server.h"
#include "pol_request.h"
#include "pol_response.h"
#include "crypto.h"

#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <BLE2902.h>

#define SERVICE_UUID         "f44dce36-ffb2-565b-8494-25fa5a7a7cd6"
#define WRITE_CHAR_UUID      "8e8c14b7-d9f0-5e5c-9da8-6961e1f33d6b"
#define INDICATE_CHAR_UUID   "d234a7d8-ea1f-5299-8221-9cf2f942d3df"

static BLECharacteristic* pIndicateCharacteristic = nullptr;
static BleServer* instance = nullptr;

BleServer::BleServer(uint32_t beacon_id, const uint8_t sk[32], const uint8_t pk[32], MinuteCounter& counter)
    : _beacon_id(beacon_id), _counter(counter), _handler(beacon_id, sk, pk, counter){
    memcpy(_sk, sk, 32);
    memcpy(_pk, pk, 32);
    instance = this;
    _queue = xQueueCreate(4, sizeof(RequestMessage));
    if (_queue == nullptr) {
      Serial.println("[BLE] Failed to create request queue");
  }
}

void printHex(const uint8_t* data, size_t len) {
  Serial.print("HEX DUMP (");
  Serial.print(len);
  Serial.println(" bytes):");

  char line[16 * 3 + 1]; // Up to 16 bytes per line, 3 chars per byte + null terminator

  for (size_t i = 0; i < len; i += 16) {
      size_t chunk = (len - i >= 16) ? 16 : len - i;
      for (size_t j = 0; j < chunk; ++j) {
          sprintf(&line[j * 3], "%02X ", data[i + j]);
      }
      line[chunk * 3] = '\0'; // Null terminate
      Serial.println(line);
  }
}

void BleServer::begin() {
  BLEDevice::init("PoL Beacon");
  BLEServer* server = BLEDevice::createServer();

  BLEService* service = server->createService(SERVICE_UUID);
  BLECharacteristic* pWrite = service->createCharacteristic(
    WRITE_CHAR_UUID, BLECharacteristic::PROPERTY_WRITE);

  pIndicateCharacteristic = service->createCharacteristic(
    INDICATE_CHAR_UUID, BLECharacteristic::PROPERTY_INDICATE);
    pIndicateCharacteristic->addDescriptor(new BLE2902());

    // Set up write callback
    class WriteHandler : public BLECharacteristicCallbacks {
      void onWrite(BLECharacteristic* pChar) override {
        std::string value = pChar->getValue();

        Serial.printf("[BLE] Write received: %d bytes\n", (int)value.size());
        printHex((const uint8_t*)value.data(), value.size());

        if (instance && !value.empty()) {
          instance->sendResponse((const uint8_t*)value.data(), value.size());
        }
      }
    };

  pWrite->setCallbacks(new WriteHandler());
  service->start();

  BLEAdvertising* advertising = BLEDevice::getAdvertising();
  BLEAdvertisementData advData;
  advData.setName("PoL Beacon");
  advData.setCompleteServices(BLEUUID(SERVICE_UUID));
  advertising->setAdvertisementData(advData);

  advertising->addServiceUUID(SERVICE_UUID);
  advertising->setScanResponse(false);
  advertising->setMinPreferred(0x06);  // Recommended for iOS
  advertising->setMaxPreferred(0x12);
  advertising->start();

  
  BLEDevice::setMTU(517);
  Serial.println("[BLE] Advertising started");

  BaseType_t res = xTaskCreatePinnedToCore(
    [](void* arg) {
        static_cast<BleServer*>(arg)->processRequests();
    },
    "PoLProc",         // Task name
    4096,              // Stack size
    this,              // Task parameter
    1,                 // Priority
    nullptr,           // Task handle
    APP_CPU_NUM        // Core to pin to
);
  
  if (res != pdPASS) {
    Serial.println("[BLE] Failed to create PoL processor task!");
}
}

void BleServer::sendResponse(const uint8_t* data, size_t len) {
  if (!_queue || len > sizeof(RequestMessage::data)) {
    Serial.println("[BLE] Request too long or queue uninitialized");
    return;
  }

  RequestMessage msg;
  memcpy(msg.data, data, len);
  msg.len = len;

  if (xQueueSend(_queue, &msg, 0) != pdTRUE) {
    Serial.println("[BLE] Request queue full, dropping request");
  }
}

void BleServer::processRequests() {
  Serial.println("[BLE] PoL processor task started");
  RequestMessage msg;
  while (true) {
    if (xQueueReceive(_queue, &msg, portMAX_DELAY) == pdTRUE) {
      Serial.println("[BLE] Got request from queue");
      handlePoLRequest(msg.data, msg.len);
    }
  }
}

void BleServer::handlePoLRequest(const uint8_t* data, size_t len) {
  Serial.println("[BLE] Received PoLRequest");

  PoLResponse resp;
  if (!_handler.handle(data, len, resp)) {
      Serial.println("[BLE] Request handling failed");
      return;
  }

  Serial.println("[BLE] Sending valid PoLResponse");

  uint8_t buffer[PoLResponse::packedSize()];
  resp.toBytes(buffer);
  Serial.println("Data sent back:");
  printHex(buffer, sizeof(buffer));

  if (pIndicateCharacteristic) {
      pIndicateCharacteristic->setValue(buffer, sizeof(buffer));
      pIndicateCharacteristic->indicate();
      Serial.println("[BLE] Sent PoLResponse");
  }
}
