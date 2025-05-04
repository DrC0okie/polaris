#include "ble_server.h"

#include <BLE2902.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUUID.h>
#include <BLEUtils.h>
#include <Preferences.h>

#define SERVICE_UUID "f44dce36-ffb2-565b-8494-25fa5a7a7cd6"
#define WRITE_CHAR_UUID "8e8c14b7-d9f0-5e5c-9da8-6961e1f33d6b"
#define INDICATE_CHAR_UUID "d234a7d8-ea1f-5299-8221-9cf2f942d3df"

// ========== SERVER CALLBACKS ==========
class BleServer::ServerCallbacks : public BLEServerCallbacks {
    void onMtuChanged(BLEServer *pServer,
                      esp_ble_gatts_cb_param_t *param) override {
        Serial.printf("[BLE] Negotiated MTU: %u bytes\n", param->mtu.mtu);
    }
    void onConnect(BLEServer *) override {
        Serial.println("[BLE] Client connected");
    }
    void onDisconnect(BLEServer *) override {
        Serial.println("[BLE] Client disconnected");
    }
};

// ========== WRITE HANDLER ==========
class BleServer::WriteHandler : public BLECharacteristicCallbacks {
public:
    explicit WriteHandler(BleServer *server) : _server(server) {
    }

    void onWrite(BLECharacteristic *pChar) override {
        auto value = pChar->getValue();
        Serial.printf("[BLE] Write received: %d bytes\n", (int)value.size());
        printHex((const uint8_t *)value.data(), value.size());
        if (_server && !value.empty()) {
            _server->queueRequest((const uint8_t *)value.data(), value.size());
        }
    }

private:
    BleServer *_server;
    void printHex(const uint8_t *data, size_t len) {
        Serial.print("HEX DUMP (");
        Serial.print(len);
        Serial.println(" bytes):");

        char line[16 * 3 + 1];  // Up to 16 bytes per line, 3 chars per byte +
                                // null terminator

        for (size_t i = 0; i < len; i += 16) {
            size_t chunk = (len - i >= 16) ? 16 : len - i;
            for (size_t j = 0; j < chunk; ++j) {
                sprintf(&line[j * 3], "%02X ", data[i + j]);
            }
            line[chunk * 3] = '\0';  // Null terminate
            Serial.println(line);
        }
    }
};

BleServer::BleServer(size_t maxRequestSize) : _maxRequestSize(maxRequestSize) {
    _queue = xQueueCreate(4, sizeof(RequestMessage));
    if (_queue == nullptr) {
        Serial.println("[BLE] Failed to create request queue");
    }
}

BleServer::~BleServer() {
    if (_writeHandler) {
        delete _writeHandler;
        _writeHandler = nullptr;
    }

    if (_serverCallbacks) {
        delete _serverCallbacks;
        _serverCallbacks = nullptr;
    }

    if (_queue) {
        vQueueDelete(_queue);
        _queue = nullptr;
    }
}

void BleServer::begin() {
    BLEDevice::init("PoL Beacon");
    BLEDevice::setMTU(517);

    BLEServer *server = BLEDevice::createServer();
    _serverCallbacks = new ServerCallbacks();
    server->setCallbacks(_serverCallbacks);

    BLEService *service = server->createService(SERVICE_UUID);

    // --- WRITE CHARACTERISTIC ---
    BLECharacteristic *pWrite = service->createCharacteristic(
        WRITE_CHAR_UUID, BLECharacteristic::PROPERTY_WRITE);
    pWrite->setAccessPermissions(ESP_GATT_PERM_WRITE);
    addUserDescription(pWrite, "PoL Request (write)");
    _writeHandler = new WriteHandler(this);
    pWrite->setCallbacks(_writeHandler);

    // --- INDICATE CHARACTERISTIC ---
    _indicateCharacteristic = service->createCharacteristic(
        INDICATE_CHAR_UUID, BLECharacteristic::PROPERTY_INDICATE);
    _indicateCharacteristic->setAccessPermissions(ESP_GATT_PERM_READ);
    BLE2902 *desc = new BLE2902();
    desc->setAccessPermissions(ESP_GATT_PERM_READ | ESP_GATT_PERM_WRITE);
    _indicateCharacteristic->addDescriptor(desc);
    addUserDescription(_indicateCharacteristic, "PoL Response (indicate)");

    service->start();

    BLEAdvertising *advertising = BLEDevice::getAdvertising();
    BLEAdvertisementData advData;
    advData.setName("PoL Beacon");
    advData.setCompleteServices(BLEUUID(SERVICE_UUID));
    advertising->setAdvertisementData(advData);
    advertising->addServiceUUID(SERVICE_UUID);
    advertising->setScanResponse(false);
    advertising->setMinPreferred(0x06);  // Recommended for iOS
    advertising->setMaxPreferred(0x12);
    advertising->start();

    Serial.println("[BLE] Advertising started");

    BaseType_t res = xTaskCreatePinnedToCore(
        [](void *arg) { static_cast<BleServer *>(arg)->processRequests(); },
        "PoLProc",   // Task name
        4096,        // Stack size
        this,        // Task parameter
        1,           // Priority
        nullptr,     // Task handle
        APP_CPU_NUM  // Core to pin to
    );

    if (res != pdPASS) {
        Serial.println("[BLE] Failed to create PoL processor task!");
    }
}

// ========== ADD REQUEST TO QUEUE ==========
void BleServer::queueRequest(const uint8_t *data, size_t len) {
    if (!_queue || len > _maxRequestSize) {
        Serial.println("[BLE] Request too long or queue uninitialized");
        return;
    }

    RequestMessage msg = {};
    msg.data = static_cast<uint8_t *>(malloc(len));
    if (!msg.data) {
        Serial.println("[BLE] Failed to allocate memory for request");
        return;
    }

    memcpy(msg.data, data, len);
    msg.len = len;

    if (xQueueSend(_queue, &msg, 0) != pdTRUE) {
        Serial.println("[BLE] Request queue full, dropping request");
        free(msg.data);
    }
}

// ========== PROCESS REQUEST QUEUE ==========
void BleServer::processRequests() {
    Serial.println("[BLE] PoL processor task started");
    RequestMessage msg;
    while (true) {
        if (xQueueReceive(_queue, &msg, portMAX_DELAY) == pdTRUE) {
            Serial.println("[BLE] Got request from queue");
            if (_requestProcessor) {
                _requestProcessor->process(msg.data, msg.len);
            } else {
                Serial.println("[BLE] No request processor set");
            }
            free(msg.data);
        }
    }
}

// ========== UTILS ==========
void BleServer::addUserDescription(BLECharacteristic *characteristic,
                                   const std::string &description) {
    auto *desc = new BLEDescriptor(BLEUUID((uint16_t)0x2901));
    desc->setValue(description);
    characteristic->addDescriptor(desc);
}

void BleServer::setRequestProcessor(IPolRequestProcessor *processor) {
    _requestProcessor = processor;
}

BLECharacteristic *BleServer::getIndicationCharacteristic() const {
    return _indicateCharacteristic;
}