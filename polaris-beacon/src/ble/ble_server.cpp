#include "ble_server.h"

#include <BLE2902.h>         // For standard descriptors like CCCD
#include <BLEAdvertising.h>  // For BLEMultiAdvertising and BLEAdvertisementData
#include <BLEDevice.h>
#include <BLEUUID.h>
#include <BLEUtils.h>        // For helper functions like esp_err_to_name
#include <HardwareSerial.h>  // For Serial output

// ========== SERVER CALLBACKS ==========
BleServer::ServerCallbacks::ServerCallbacks(BleServer* parentServer) : _parentServer(parentServer) {
}

void BleServer::ServerCallbacks::onMtuChanged(BLEServer* /*pServer*/,
                                              esp_ble_gatts_cb_param_t* param) {
    Serial.printf("[BLE] Negotiated MTU: %u bytes\n", param->mtu.mtu);
}

void BleServer::ServerCallbacks::onConnect(BLEServer* /*pServer*/) {
    if (_parentServer && _parentServer->getMultiAdvertiser()) {
        Serial.println("[BLE] Client connected. Stopping legacy advertising instance.");
        if (!_parentServer->getMultiAdvertiser()->stop(1, &LEGACY_ADV_INSTANCE)) {
            Serial.printf("[BLE] Failed to stop legacy advertising instance "
                          "%d.\n",
                          LEGACY_ADV_INSTANCE);
        }
    } else {
        Serial.println("[BLE] Client connected, but multi-advertiser not "
                       "available to stop.");
    }
}

void BleServer::ServerCallbacks::onDisconnect(BLEServer* /*pServer*/) {
    if (_parentServer && _parentServer->getMultiAdvertiser()) {
        Serial.println("[BLE] Client disconnected. Restarting legacy "
                       "advertising instance.");
        if (!_parentServer->getMultiAdvertiser()->start(1, LEGACY_ADV_INSTANCE)) {
            Serial.printf("[BLE] Failed to restart legacy advertising instance "
                          "%d.\n",
                          LEGACY_ADV_INSTANCE);
        }
    } else {
        Serial.println("[BLE] Client disconnected, but multi-advertiser not "
                       "available to restart.");
    }
}

// ========== WRITE CHARACTERISTIC CALLBACK ==========
BleServer::WriteHandler::WriteHandler(BleServer* server) : _server(server) {
}

void BleServer::WriteHandler::onWrite(BLECharacteristic* pChar) {
    if (!pChar)
        return;
    std::string value = pChar->getValue();
    Serial.printf("[BLE] Write received: %zu bytes\n", value.size());
    if (_server && !value.empty()) {
        _server->queueRequest(reinterpret_cast<const uint8_t*>(value.data()), value.size());
    }
}

BleServer::BleServer(size_t maxRequestSize) : _maxRequestSize(maxRequestSize) {
    _multiAdvertiserPtr =
        std::unique_ptr<BLEMultiAdvertising>(new BLEMultiAdvertising(NUM_ADV_INSTANCES));
    if (!_multiAdvertiserPtr) {
        Serial.println("[BLE] CRITICAL: Failed to allocate BLEMultiAdvertising!");
    }
    _queue = xQueueCreate(4, sizeof(RequestMessage));  // Queue for 4 requests
    if (_queue == nullptr) {
        Serial.println("[BLE] CRITICAL: Failed to create request queue!");
    }
}

BleServer::~BleServer() {
    stop();
}

// ========== BEGIN ==========
void BleServer::begin(const std::string& deviceName) {
    Serial.println("[BLE] Initializing BLE Device stack...");
    BLEDevice::init("");

    Serial.println("[BLE] Setting global MTU...");
    BLEDevice::setMTU(517);

    Serial.println("[BLE] Creating GATT Server...");
    _pServer = BLEDevice::createServer();
    if (!_pServer) {
        Serial.println("[BLE] CRITICAL: Failed to create BLE server!");
        BLEDevice::deinit(true);
        return;
    }

    _serverCallbacks = std::unique_ptr<ServerCallbacks>(new ServerCallbacks(this));
    if (!_serverCallbacks) {
        Serial.println("[BLE] CRITICAL: Failed to allocate ServerCallbacks!");
        BLEDevice::deinit(true);
        return;
    }
    _pServer->setCallbacks(_serverCallbacks.get());

    Serial.println("[BLE] Creating GATT Service and Characteristics...");
    BLEService* service = _pServer->createService(BLEUUID(SERVICE_UUID));
    if (!service) {
        Serial.println("[BLE] CRITICAL: Failed to create BLE service!");
        BLEDevice::deinit(true);
        return;
    }

    // --- WRITE CHARACTERISTIC ---
    BLECharacteristic* pWrite =
        service->createCharacteristic(BLEUUID(WRITE_UUID), BLECharacteristic::PROPERTY_WRITE);
    if (!pWrite) {
        Serial.println("[BLE] CRITICAL: Failed to create Write characteristic!");
        BLEDevice::deinit(true);
        return;
    }
    pWrite->setAccessPermissions(ESP_GATT_PERM_WRITE);
    addUserDescription(pWrite, "PoL Request (write)");
    _writeHandler = std::unique_ptr<WriteHandler>(new WriteHandler(this));
    if (!_writeHandler) {
        Serial.println("[BLE] CRITICAL: Failed to allocate WriteHandler!");
        BLEDevice::deinit(true);
        return;
    }
    pWrite->setCallbacks(_writeHandler.get());

    // --- INDICATE CHARACTERISTIC ---
    _indicateCharacteristic =
        service->createCharacteristic(BLEUUID(INDICATE_UUID), BLECharacteristic::PROPERTY_INDICATE);
    if (!_indicateCharacteristic) {
        Serial.println("[BLE] CRITICAL: Failed to create Indicate characteristic!");
        BLEDevice::deinit(true);
        return;
    }
    _indicateCharacteristic->setAccessPermissions(ESP_GATT_PERM_READ);
    BLE2902* indicateDesc = new BLE2902();  // CCCD for indications/notifications
    if (!indicateDesc) {
        Serial.println("[BLE] CRITICAL: Failed to allocate BLE2902 descriptor!");
        BLEDevice::deinit(true);
        return;
    }
    indicateDesc->setAccessPermissions(ESP_GATT_PERM_READ | ESP_GATT_PERM_WRITE);
    _indicateCharacteristic->addDescriptor(indicateDesc);
    addUserDescription(_indicateCharacteristic, "PoL Response (indicate)");

    Serial.println("[BLE] Configuring Legacy Advertisement...");
    if (!configureLegacyAdvertisement(deviceName)) {
        Serial.println("[BLE] CRITICAL: Failed to configure legacy advertisement!");
        BLEDevice::deinit(true);
        return;
    }

    Serial.println("[BLE] Configuring Extended Advertisement...");
    if (!configureExtendedAdvertisement()) {
        Serial.println("[BLE] WARNING: Failed to configure extended advertisement.");
    }

    service->start();
    Serial.println("[BLE] GATT Service started.");

    Serial.println("[BLE] Starting Multi-Advertising instances...");
    if (!_multiAdvertiserPtr->start(NUM_ADV_INSTANCES, 0)) {
        Serial.printf("[BLE] CRITICAL: Failed to start multi-advertising.\n");
        BLEDevice::deinit(true);
        return;
    }
    Serial.println("[BLE] Multi-Advertising started.");

    Serial.println("[BLE] Starting PoL processor task...");
    _shutdownRequested = false;
    BaseType_t task_res = xTaskCreatePinnedToCore(processorTaskTrampoline, "PoLProc", 4096, this, 1,
                                                  &_processorTaskHandle, APP_CPU_NUM);
    if (task_res != pdPASS) {
        Serial.println("[BLE] CRITICAL: Failed to create PoL processor task!");
        // Consider full stop/deinit
    } else {
        Serial.println("[BLE] PoL processor task created.");
    }

    Serial.println("[BLE] BleServer::begin() complete.");
}

// ========== STOP ==========
void BleServer::stop() {
    Serial.println("[BLE] Stopping BleServer...");
    if (_processorTaskHandle != nullptr) {
        Serial.println("[BLE] Signaling processor task to shut down...");
        _shutdownRequested = true;
        vTaskDelay(pdMS_TO_TICKS(200));
        TaskHandle_t tempHandle = _processorTaskHandle;
        _processorTaskHandle = nullptr;
        vTaskDelete(tempHandle);
    }

    if (_multiAdvertiserPtr) {
        Serial.println("[BLE] Stopping and clearing multi-advertiser instances...");
        _multiAdvertiserPtr->stop(NUM_ADV_INSTANCES, nullptr);
        _multiAdvertiserPtr->clear();
    }

    if (_queue != nullptr) {
        vQueueDelete(_queue);
        _queue = nullptr;
    }

    if (BLEDevice::getInitialized()) {
        BLEDevice::deinit(true);
    }

    _pServer = nullptr;
    _indicateCharacteristic = nullptr;
}

// ========== QUEUE REQUEST ==========
void BleServer::queueRequest(const uint8_t* data, size_t len) {
    if (!_queue) {
        Serial.println("[BLE] Queue not initialized, dropping request.");
        return;
    }
    if (len == 0) {
        Serial.println("[BLE] Empty request received, dropping.");
        return;
    }
    if (len > PoLRequest::packedSize()) {
        Serial.printf("[BLE] Request too large (%zu bytes, max %zu). Dropping.\n", len,
                      PoLRequest::packedSize());
        return;
    }

    RequestMessage msg;
    memcpy(msg.data, data, len);
    msg.len = len;

    if (xQueueSend(_queue, &msg, pdMS_TO_TICKS(10)) != pdTRUE) {
        Serial.println("[BLE] Request queue full, dropping request.");
    }
}

// ========== PROCESSOR TASK & TRAMPOLINE ==========
void BleServer::processorTaskTrampoline(void* pvParameters) {
    BleServer* serverInstance = static_cast<BleServer*>(pvParameters);
    if (serverInstance) {
        serverInstance->processRequests();
    }
    vTaskDelete(NULL);
}

void BleServer::processRequests() {
    Serial.println("[BLE] PoL processor task started.");
    RequestMessage msg;
    while (!_shutdownRequested) {
        if (xQueueReceive(_queue, &msg, pdMS_TO_TICKS(100)) == pdTRUE) {
            if (_requestProcessor) {
                _requestProcessor->process(msg.data, msg.len);
            } else {
                Serial.println("[BLE] No request processor set, request ignored.");
            }
        }
    }
}

// ========== UTILS ==========
void BleServer::addUserDescription(BLECharacteristic* characteristic,
                                   const std::string& description) {
    if (!characteristic)
        return;
    BLEDescriptor* desc = new BLEDescriptor(BLEUUID((uint16_t)0x2901));  // User Description UUID
    if (!desc) {
        Serial.println("[BLE] Failed to allocate User Description descriptor!");
        return;
    }
    desc->setValue(description);
    desc->setAccessPermissions(ESP_GATT_PERM_READ);
    characteristic->addDescriptor(desc);
}

void BleServer::setRequestProcessor(std::unique_ptr<IPolRequestProcessor> processor) {
    _requestProcessor = std::move(processor);
}

BLECharacteristic* BleServer::getIndicationCharacteristic() const {
    return _indicateCharacteristic;
}

BLEMultiAdvertising* BleServer::getMultiAdvertiser() {
    return _multiAdvertiserPtr.get();
}

// --- ADVERTISING CONFIGURATION HELPERS ---
bool BleServer::configureLegacyAdvertisement(const std::string& deviceName) {
    esp_ble_gap_ext_adv_params_t legacy_params = {
        .type = ESP_BLE_GAP_SET_EXT_ADV_PROP_LEGACY_IND,  // Connectable,
                                                          // Scannable, Legacy
        .interval_min = 0x50,
        .interval_max = 0x50,
        .channel_map = ADV_CHNL_ALL,
        .own_addr_type = BLE_ADDR_TYPE_PUBLIC,
        .filter_policy = ADV_FILTER_ALLOW_SCAN_ANY_CON_ANY,
        .tx_power = EXT_ADV_TX_PWR_NO_PREFERENCE,
        .primary_phy = ESP_BLE_GAP_PHY_1M,
        .secondary_phy = ESP_BLE_GAP_PHY_1M,
        .sid = LEGACY_ADV_SID,
        .scan_req_notif = false,
    };

    if (!_multiAdvertiserPtr->setAdvertisingParams(LEGACY_ADV_INSTANCE, &legacy_params)) {
        Serial.println("[BLE] Failed to set legacy advertising parameters.");
        return false;
    }
    Serial.println("[BLE] Legacy advertising parameters set.");

    // Advertising Data
    BLEAdvertisementData advData;
    advData.setFlags(ESP_BLE_ADV_FLAG_GEN_DISC | ESP_BLE_ADV_FLAG_BREDR_NOT_SPT);

    advData.setCompleteServices(BLEUUID(SERVICE_UUID));

    std::string advPayload = advData.getPayload();
    if (advPayload.length() > ESP_BLE_ADV_DATA_LEN_MAX) {
        Serial.printf("[BLE] WARNING: Legacy adv payload too long (%zu bytes, max 31).\n",
                      advPayload.length());
    }
    if (!_multiAdvertiserPtr->setAdvertisingData(
            LEGACY_ADV_INSTANCE, advPayload.length(),
            reinterpret_cast<const uint8_t*>(advPayload.data()))) {
        Serial.println("[BLE] Failed to set legacy advertising data.");
        return false;
    }
    Serial.println("[BLE] Legacy advertising data set.");

    // Scan Response Data
    BLEAdvertisementData scanRspData;
    scanRspData.setName(deviceName);  // Full name in scan response

    std::string scanRspPayload = scanRspData.getPayload();
    if (scanRspPayload.length() > ESP_BLE_SCAN_RSP_DATA_LEN_MAX) {
        Serial.printf("[BLE] WARNING: Legacy scan response payload too long "
                      "(%zu bytes, max 31).\n",
                      scanRspPayload.length());
    }
    if (!scanRspPayload.empty()) {
        if (!_multiAdvertiserPtr->setScanRspData(
                LEGACY_ADV_INSTANCE, scanRspPayload.length(),
                reinterpret_cast<const uint8_t*>(scanRspPayload.data()))) {
            Serial.println("[BLE] Failed to set legacy scan response data.");
            return false;
        }
        Serial.println("[BLE] Legacy scan response data set.");
    }

    _multiAdvertiserPtr->setDuration(LEGACY_ADV_INSTANCE, 0, 0);
    Serial.println("[BLE] Legacy Advertisement (Instance 0) configured successfully.");
    return true;
}

bool BleServer::configureExtendedAdvertisement() {
    Serial.println("[BLE] Configuring Extended Advertisement...");

    esp_ble_gap_ext_adv_params_t ext_params = {
        .type = ESP_BLE_GAP_SET_EXT_ADV_PROP_NONCONN_NONSCANNABLE_UNDIRECTED,
        .interval_min = 0x60,
        .interval_max = 0x60,
        .channel_map = ADV_CHNL_ALL,
        .own_addr_type = BLE_ADDR_TYPE_PUBLIC,
        .filter_policy = ADV_FILTER_ALLOW_SCAN_ANY_CON_ANY,
        .tx_power = EXT_ADV_TX_PWR_NO_PREFERENCE,
        .primary_phy = ESP_BLE_GAP_PHY_1M,
        .secondary_phy = ESP_BLE_GAP_PHY_CODED,  // For longer range broadcast
        .sid = EXTENDED_ADV_SID,
        .scan_req_notif = false,
    };

    if (!_multiAdvertiserPtr->setAdvertisingParams(EXTENDED_ADV_INSTANCE, &ext_params)) {
        Serial.println("[BLE] Failed to set extended advertising parameters.");
        return false;
    }
    Serial.println("[BLE] Extended advertising parameters set.");

    // The BeaconAdvertiser will overwrite this with the actual dynamic data.
    uint8_t placeholder_data[] = {0x02, 0x01, 0x06};

    if (!_multiAdvertiserPtr->setAdvertisingData(EXTENDED_ADV_INSTANCE, sizeof(placeholder_data),
                                                 placeholder_data)) {
        Serial.println("[BLE] Failed to set extended advertising data.");
        return false;
    }
    Serial.println("[BLE] Extended advertising data set.");

    _multiAdvertiserPtr->setDuration(EXTENDED_ADV_INSTANCE, 0, 0);
    Serial.println("[BLE] Extended Advertisement (Instance 1) configured successfully.");
    return true;
}