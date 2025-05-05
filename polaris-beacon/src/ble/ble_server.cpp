#include "ble_server.h"

#include <BLE2902.h>
#include <BLEDevice.h>
#include <BLEUUID.h>
#include <BLEUtils.h>
#include <HardwareSerial.h>

// ========== SERVER CALLBACKS ==========
class BleServer::ServerCallbacks : public BLEServerCallbacks {
    void onMtuChanged(BLEServer *pServer,
                      esp_ble_gatts_cb_param_t *param) override {
        Serial.printf("[BLE] Negotiated MTU: %u bytes\n", param->mtu.mtu);
    }
    void onConnect(BLEServer *) override {
        if (BLEDevice::getInitialized()) {
            BLEDevice::getAdvertising()->stop();
        }
        Serial.println("[BLE] Client connected");
    }
    void onDisconnect(BLEServer *) override {
        if (BLEDevice::getInitialized()) {
            BLEDevice::getAdvertising()->start();
        }
        Serial.println("[BLE] Client disconnected");
    }
};

// ========== WRITE CHARACTERISTIC CALLBACK ==========
class BleServer::WriteHandler : public BLECharacteristicCallbacks {
public:
    explicit WriteHandler(BleServer *server) : _server(server) {
    }

    void onWrite(BLECharacteristic *pChar) override {
        auto value = pChar->getValue();
        Serial.printf("[BLE] Write received: %d bytes\n", (int)value.size());
        if (_server && !value.empty()) {
            _server->queueRequest((const uint8_t *)value.data(), value.size());
        }
    }

private:
    BleServer *_server;
};

BleServer::BleServer(size_t maxRequestSize) : _maxRequestSize(maxRequestSize) {
    _queue = xQueueCreate(4, sizeof(RequestMessage));
    if (_queue == nullptr) {
        Serial.println("[BLE] Failed to create request queue");
    }
}

BleServer::~BleServer() {
    stop();

    if (_writeHandler) {
        delete _writeHandler;
        _writeHandler = nullptr;
    }
    if (_serverCallbacks) {
        delete _serverCallbacks;
        _serverCallbacks = nullptr;
    }
}

void BleServer::begin() {
    BLEDevice::init("PoL Beacon");
    BLEDevice::setMTU(517);

    _pServer = BLEDevice::createServer();

    if (!_pServer) {
        Serial.println("[BLE] Failed to create BLE server!");
        return;
    }

    _serverCallbacks = new ServerCallbacks();

    if (!_serverCallbacks) {
        Serial.println("[BLE] Failed to allocate ServerCallbacks!");
        return;
    }

    _pServer->setCallbacks(_serverCallbacks);

    BLEService *service = _pServer->createService(SERVICE_UUID);

    // --- WRITE CHARACTERISTIC ---
    BLECharacteristic *pWrite = service->createCharacteristic(
        WRITE_UUID, BLECharacteristic::PROPERTY_WRITE);
    pWrite->setAccessPermissions(ESP_GATT_PERM_WRITE);
    addUserDescription(pWrite, "PoL Request (write)");
    _writeHandler = new WriteHandler(this);
    pWrite->setCallbacks(_writeHandler);

    // --- INDICATE CHARACTERISTIC ---
    _indicateCharacteristic = service->createCharacteristic(
        INDICATE_UUID, BLECharacteristic::PROPERTY_INDICATE);
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

    _shutdownRequested = false;  // Ensure flag is reset before task starts
    BaseType_t res = xTaskCreatePinnedToCore(
        processorTaskTrampoline,  // Static trampoline function
        "PoLProc",                // Task name
        4096,                     // Stack size
        this,                     // Pass 'this' pointer as parameter
        1,                        // Priority
        &_processorTaskHandle,    // Store task handle
        APP_CPU_NUM               // Core to pin to (usually core 1)
    );

    if (res != pdPASS) {
        Serial.println("[BLE] Failed to create PoL processor task!");
    }
}

void BleServer::stop() {
    // Signal and stop the task
    if (_processorTaskHandle != nullptr) {
        _shutdownRequested = true;

        // Give the task a moment to see the flag
        vTaskDelay(pdMS_TO_TICKS(150));
        vTaskDelete(_processorTaskHandle);
        _processorTaskHandle = nullptr;
    }

    // Stop Advertising (check if initialized)
    if (BLEDevice::getInitialized()) {
        BLEDevice::getAdvertising()->stop();
    }

    // Delete Queue (check handle)
    if (_queue != nullptr) {
        vQueueDelete(_queue);
        _queue = nullptr;
    }

    // Delete the request processor AFTER the task is stopped
    if (_requestProcessor != nullptr) {
        delete _requestProcessor;
        _requestProcessor = nullptr;  // Prevent double deletion
    }

    // Deinitialize BLE Stack (check if initialized)
    if (BLEDevice::getInitialized()) {
        BLEDevice::deinit(true);
    }

    _pServer = nullptr;
    _indicateCharacteristic = nullptr;
}

// ========== ADD REQUEST TO QUEUE ==========
void BleServer::queueRequest(const uint8_t *data, size_t len) {
    if (!_queue || len != PoLRequest::packedSize()) {
        Serial.printf(
            "[BLE] Invalid request size %d (expected %d). Dropping.\n", len,
            PoLRequest::packedSize());
        return;
    }

    RequestMessage msg;
    memcpy(msg.data, data, len);
    msg.len = len;

    if (xQueueSend(_queue, &msg, 0) != pdTRUE) {
        Serial.println("[BLE] Request queue full, dropping request");
    }
}

/* Workaround to integrate C++ object methods with C-based task schedulers */
void BleServer::processorTaskTrampoline(void *pvParameters) {
    // Cast the parameter back to the BleServer instance pointer
    BleServer *serverInstance = static_cast<BleServer *>(pvParameters);
    if (serverInstance) {
        // Call the actual member function
        serverInstance->processRequests();
    }
    // Task function should not return, but if it does, delete itself.
    vTaskDelete(NULL);
}

// ========== PROCESS REQUEST QUEUE ==========
void BleServer::processRequests() {
    Serial.println("[BLE] PoL processor task started");
    RequestMessage msg;
    while (!_shutdownRequested) {
        // Wait for a message, but with a timeout to allow checking the shutdown
        // flag
        if (xQueueReceive(_queue, &msg, pdMS_TO_TICKS(100)) == pdTRUE) {
            Serial.println("[BLE] Got request from queue");
            if (_requestProcessor) {
                _requestProcessor->process(msg.data, msg.len);
            } else {
                Serial.println("[BLE] No request processor set");
            }
        }
    }
}

// ========== UTILS ==========
void BleServer::addUserDescription(BLECharacteristic *characteristic,
                                   const std::string &description) {
    auto *desc = new BLEDescriptor(BLEUUID((uint16_t)0x2901));
    desc->setValue(description);
    desc->setAccessPermissions(ESP_GATT_PERM_READ);
    characteristic->addDescriptor(desc);
}

void BleServer::setRequestProcessor(IPolRequestProcessor *processor) {
    // Delete the currently held processor, if one exists, before assigning the
    // new one.
    if (_requestProcessor != nullptr && _requestProcessor != processor) {
        delete _requestProcessor;
        _requestProcessor = nullptr;
    } else if (_requestProcessor == processor) {
        return;
    }

    // Take ownership of the new processor pointer
    _requestProcessor = processor;
}

BLECharacteristic *BleServer::getIndicationCharacteristic() const {
    return _indicateCharacteristic;
}