#include "ble_server.h"

#include <BLE2902.h>         // For standard descriptors like CCCD
#include <BLEAdvertising.h>  // For BLEMultiAdvertising and BLEAdvertisementData
#include <BLEDevice.h>
#include <BLEUUID.h>
#include <BLEUtils.h>        // For helper functions like esp_err_to_name
#include <HardwareSerial.h>  // For Serial output

#include <algorithm>

#include "characteristics/indicate_characteristic.h"
#include "characteristics/write_characteristic.h"

// ========== SERVER CALLBACKS ==========
BleServer::ServerCallbacks::ServerCallbacks(BleServer* parentServer) : _parentServer(parentServer) {
}

void BleServer::ServerCallbacks::onMtuChanged(BLEServer* /*pServer*/,
                                              esp_ble_gatts_cb_param_t* param) {
    Serial.printf("[BLE] Negotiated MTU: %u bytes\n", param->mtu.mtu);
}

void BleServer::ServerCallbacks::onConnect(BLEServer* /*pServer*/) {
    if (_parentServer && _parentServer->getMultiAdvertiser()) {
        Serial.println("[BLE] Client connected.");
        if (!_parentServer->getMultiAdvertiser()->stop(1, &LEGACY_TOKEN_ADV_INSTANCE)) {
            Serial.printf("[BLE] Failed to stop legacy advertising instance %d.\n",
                          LEGACY_TOKEN_ADV_INSTANCE);
        }
    }
}

void BleServer::ServerCallbacks::onDisconnect(BLEServer* /*pServer*/) {
    if (_parentServer && _parentServer->getMultiAdvertiser()) {
        Serial.println("[BLE] Client disconnected. Restarting legacy "
                       "advertising instance.");
        if (!_parentServer->getMultiAdvertiser()->start(1, LEGACY_TOKEN_ADV_INSTANCE)) {
            Serial.printf("[BLE] Failed to restart legacy advertising instance %d.\n",
                          LEGACY_TOKEN_ADV_INSTANCE);
        }
    }
}

// ========== CONSTRUCTOR & DESTRUCTOR ==========
BleServer::BleServer() {
    _multiAdvertiserPtr =
        std::unique_ptr<BLEMultiAdvertising>(new BLEMultiAdvertising(NUM_ADV_INSTANCES));
    if (!_multiAdvertiserPtr) {
        Serial.println("[BLE] CRITICAL: Failed to allocate BLEMultiAdvertising!");
    }
    _tokenQueue = xQueueCreate(4, sizeof(TokenRequestMessage));
    if (_tokenQueue == nullptr) {
        Serial.println("[BLE] CRITICAL: Failed to create request queue!");
    }
    _encryptedQueue = xQueueCreate(4, sizeof(EncryptedRequestMessage));
    if (_encryptedQueue == nullptr) {
        Serial.println("[BLE] CRITICAL: Failed to create encrypted request queue!");
    }
}

BleServer::~BleServer() {
    stop();
}

void BleServer::begin(const std::string& deviceName) {
    Serial.println("[BLE] Initializing BLE Device stack...");
    BLEDevice::init("");

    Serial.println("[BLE] Setting global MTU...");
    BLEDevice::setMTU(517);

    Serial.println("[BLE] Creating GATT Server...");
    _pServer = BLEDevice::createServer();
    if (!_pServer) {
        Serial.println("[BLE] CRITICAL: Failed to create BLE server!");
        stop();
        return;
    }

    _serverCallbacks = std::unique_ptr<ServerCallbacks>(new ServerCallbacks(this));
    if (!_serverCallbacks) {
        Serial.println("[BLE] CRITICAL: Failed to allocate server callback!");
        stop();
        return;
    }
    _pServer->setCallbacks(_serverCallbacks.get());

    // Setup all characteristics

    // Write characteristic for pol request
    _polServiceChars.push_back(std::unique_ptr<WriteCharacteristic>(new WriteCharacteristic(
        TOKEN_WRITE,
        [this](const uint8_t* data, size_t len) { this->queueTokenRequest(data, len); },
        "PoL Request (write)")));

    // Write characteristic for encrypted data
    _polServiceChars.push_back(std::unique_ptr<WriteCharacteristic>(new WriteCharacteristic(
        ENCRYPTED_WRITE,
        [this](const uint8_t* data, size_t len) { this->queueEncryptedRequest(data, len); },
        "Encrypted Data (write)")));

    // Indicate characteristic for pol response
    auto tokenIndicateWrapper = std::unique_ptr<IndicateCharacteristic>(
        new IndicateCharacteristic(TOKEN_INDICATE, "PoL Response (indicate)"));
    _polServiceChars.push_back(std::move(tokenIndicateWrapper));

    // Indicate characteristic for encrypted data
    auto encryptedIndicateWrapper = std::unique_ptr<IndicateCharacteristic>(
        new IndicateCharacteristic(ENCRYPTED_INDICATE, "Encrypted Response (indicate)"));
    _polServiceChars.push_back(std::move(encryptedIndicateWrapper));

    // pol service configuration
    Serial.println("[BLE] Creating pol service and Characteristics...");
    BLEService* polService = _pServer->createService(BLEUUID(POL_SERVICE));
    if (!polService) {
        Serial.println("[BLE] Failed to create token service!");
        stop();
        return;
    }

    // Add characteristics to the service
    for (const auto& charWrapper : _polServiceChars) {
        printf("Adding %s characteristic.\n", charWrapper->getName());
        if (!charWrapper->configure(*polService)) {
            Serial.printf(
                "[BLE] CRITICAL: Failed to configure a characteristic for PoL service.\n");
            stop();
            return;
        }
    }

    // Advertising configuration for token service
    Serial.println("[BLE] Configuring Legacy Advertisement...");
    if (!configureTokenSrvcAdvertisement(deviceName, LEGACY_TOKEN_ADV_INSTANCE, POL_SERVICE)) {
        stop();
        return;
    }

    Serial.println("[BLE] Configuring Extended Advertisement...");
    if (!configureExtendedAdvertisement()) {
        Serial.println("[BLE] WARNING: Failed to configure extended advertisement.");
    }

    polService->start();

    Serial.println("[BLE] Starting Multi-Advertising instances...");
    if (!_multiAdvertiserPtr->start(NUM_ADV_INSTANCES, 0)) {
        Serial.printf("[BLE] CRITICAL: Failed to start multi-advertising.\n");
        stop();
        return;
    }

    Serial.println("[BLE] Starting PoL processor task...");
    _shutdownRequested = false;
    BaseType_t task_res = xTaskCreatePinnedToCore(tokenProcessorTask, "PoLProc", 6144, this, 1,
                                                  &_tokenProcessorTask, tskNO_AFFINITY);
    if (task_res != pdPASS) {
        Serial.println("[BLE] CRITICAL: Failed to create PoL processor task!");
    }

    Serial.println("[BLE] Starting Encrypted Data processor task...");
    BaseType_t enc_task_res = xTaskCreatePinnedToCore(encryptedProcessorTask, "EncProc", 6144, this,
                                                      1, &_encryptedProcessorTask, tskNO_AFFINITY);
    if (enc_task_res != pdPASS) {
        Serial.println("[BLE] CRITICAL: Failed to create Encrypted Data processor task!");
    }

    Serial.println("[BLE] Server configuration complete.");
}

// ========== STOP ==========
void BleServer::stop() {
    Serial.println("[BLE] Stopping BleServer...");
    if (_tokenProcessorTask != nullptr) {
        Serial.println("[BLE] Signaling processor task to shut down...");
        _shutdownRequested = true;
        vTaskDelay(pdMS_TO_TICKS(200));
        TaskHandle_t tempHandle = _tokenProcessorTask;
        _tokenProcessorTask = nullptr;
        vTaskDelete(tempHandle);
    }

    if (_encryptedProcessorTask != nullptr) {
        Serial.println("[BLE] Waiting for Encrypted Data processor task to shut down...");
        vTaskDelay(pdMS_TO_TICKS(200));  // Give task time to see flag
        TaskHandle_t tempHandle = _encryptedProcessorTask;
        _encryptedProcessorTask = nullptr;  // Clear before delete
        vTaskDelete(tempHandle);
        Serial.println("[BLE] Encrypted Data processor task deleted.");
    }

    if (_multiAdvertiserPtr) {
        Serial.println("[BLE] Stopping and clearing multi-advertiser instances...");
        _multiAdvertiserPtr->stop(NUM_ADV_INSTANCES, 0);
        _multiAdvertiserPtr->clear();
    }

    if (_tokenQueue != nullptr) {
        vQueueDelete(_tokenQueue);
        _tokenQueue = nullptr;
    }

    if (_encryptedQueue != nullptr) {
        vQueueDelete(_encryptedQueue);
        _encryptedQueue = nullptr;
    }

    if (BLEDevice::getInitialized()) {
        BLEDevice::deinit(true);
    }

    _pServer = nullptr;
}

// ========== QUEUE TOKEN REQUEST ==========
void BleServer::queueTokenRequest(const uint8_t* data, size_t len) {
    if (!_tokenQueue) {
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

    TokenRequestMessage msg;
    memcpy(msg.data, data, len);
    msg.len = len;

    if (xQueueSend(_tokenQueue, &msg, pdMS_TO_TICKS(10)) != pdTRUE) {
        Serial.println("[BLE] Request queue full, dropping request.");
    }
}

// ========== QUEUE ENCRYPTED REQUEST ==========
void BleServer::queueEncryptedRequest(const uint8_t* data, size_t len) {
    if (!_encryptedQueue) {
        Serial.println("[BLE Enc] Encrypted queue not initialized, dropping request.");
        return;
    }
    if (len == 0) {
        Serial.println("[BLE Enc] Empty encrypted request received, dropping.");
        return;
    }
    if (len > 512) {
        Serial.printf("[BLE Enc] Encrypted request too large (%zu bytes, max %zu). Dropping.\n",
                      len, 512);
        return;
    }

    EncryptedRequestMessage msg;
    memcpy(msg.data, data, len);
    msg.len = len;

    if (xQueueSend(_encryptedQueue, &msg, pdMS_TO_TICKS(10)) != pdTRUE) {
        Serial.println("[BLE Enc] Encrypted request queue full, dropping request.");
    }
}

// ========== TOKEN PROCESSOR TASK ==========
void BleServer::tokenProcessorTask(void* pvParameters) {
    BleServer* serverInstance = static_cast<BleServer*>(pvParameters);
    if (serverInstance) {
        serverInstance->processTokenRequests();
    }
    vTaskDelete(NULL);
}

void BleServer::processTokenRequests() {
    Serial.println("[BLE] PoL processor task started.");
    TokenRequestMessage msg;
    while (!_shutdownRequested) {
        if (xQueueReceive(_tokenQueue, &msg, pdMS_TO_TICKS(100)) == pdTRUE) {
            if (_tokenRequestProcessor) {
                _tokenRequestProcessor->process(msg.data, msg.len);
            } else {
                Serial.println("[BLE] No request processor set, request ignored.");
            }
        }
    }
}

// ========== ENCRYPTED DATA PROCESSOR TASK ==========
void BleServer::encryptedProcessorTask(void* pvParameters) {
    BleServer* serverInstance = static_cast<BleServer*>(pvParameters);
    if (serverInstance) {
        serverInstance->processEncryptedRequests();
    }
    vTaskDelete(NULL);
}

void BleServer::processEncryptedRequests() {
    Serial.println("[BLE] Encrypted Data processor task started.");
    EncryptedRequestMessage msg;
    while (!_shutdownRequested) {
        if (xQueueReceive(_encryptedQueue, &msg, pdMS_TO_TICKS(100)) == pdTRUE) {
            if (_encryptedDataProcessor) {
                _encryptedDataProcessor->process(msg.data, msg.len);
            } else {
                Serial.println("[BLE Enc] No Encrypted Data processor set, request ignored.");
            }
        }
    }
    Serial.println("[BLE] Encrypted Data processor task shutting down.");
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

void BleServer::setTokenRequestProcessor(std::unique_ptr<ITokenRequestProcessor> processor) {
    _tokenRequestProcessor = std::move(processor);
}

BLECharacteristic* BleServer::getCharacteristicByUUID(const BLEUUID& uuid) const {
    auto it = std::find_if(_polServiceChars.begin(), _polServiceChars.end(),
                           [&](const std::unique_ptr<ICharacteristic>& ptr) {
                               if (ptr) {
                                   return const_cast<BLEUUID&>(ptr->getUUID()).equals(uuid);
                               }
                               return false;
                           });

    if (it != _polServiceChars.end()) {
        return (*it)->getRawCharacteristic();
    }

    Serial.println("[BLE] Characteristic with UUID not found in managed list.");
    return nullptr;
}

void BleServer::setEncryptedDataProcessor(std::unique_ptr<IEncryptedDataProcessor> processor) {
    _encryptedDataProcessor = std::move(processor);
}

BLEMultiAdvertising* BleServer::getMultiAdvertiser() {
    return _multiAdvertiserPtr.get();
}

bool BleServer::configureTokenSrvcAdvertisement(const std::string& deviceName, uint8_t instanceNum,
                                                const char* serviceUuid) {
    esp_ble_gap_ext_adv_params_t legacy_params = {
        .type = ESP_BLE_GAP_SET_EXT_ADV_PROP_LEGACY_IND,  // Connectable,
        .interval_min = 0x50,
        .interval_max = 0x50,
        .channel_map = ADV_CHNL_ALL,
        .own_addr_type = BLE_ADDR_TYPE_PUBLIC,
        .filter_policy = ADV_FILTER_ALLOW_SCAN_ANY_CON_ANY,
        .tx_power = EXT_ADV_TX_PWR_NO_PREFERENCE,
        .primary_phy = ESP_BLE_GAP_PHY_1M,
        .secondary_phy = ESP_BLE_GAP_PHY_1M,
        .sid = instanceNum,
        .scan_req_notif = false,
    };

    if (!_multiAdvertiserPtr->setAdvertisingParams(instanceNum, &legacy_params)) {
        Serial.println("[BLE] Failed to set legacy advertising parameters.");
        return false;
    }

    // Advertising Data
    BLEAdvertisementData advData;
    advData.setFlags(ESP_BLE_ADV_FLAG_GEN_DISC | ESP_BLE_ADV_FLAG_BREDR_NOT_SPT);

    advData.setCompleteServices(BLEUUID(serviceUuid));

    std::string advPayload = advData.getPayload();
    if (advPayload.length() > ESP_BLE_ADV_DATA_LEN_MAX) {
        Serial.printf("[BLE] WARNING: Legacy adv payload too long (%zu bytes, max 31).\n",
                      advPayload.length());
    }
    if (!_multiAdvertiserPtr->setAdvertisingData(
            instanceNum, advPayload.length(),
            reinterpret_cast<const uint8_t*>(advPayload.data()))) {
        Serial.println("[BLE] Failed to set legacy advertising data.");
        return false;
    }

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
                instanceNum, scanRspPayload.length(),
                reinterpret_cast<const uint8_t*>(scanRspPayload.data()))) {
            Serial.println("[BLE] Failed to set legacy scan response data.");
            return false;
        }
    }

    _multiAdvertiserPtr->setDuration(instanceNum, 0, 0);
    Serial.printf("[BLE] Legacy Advertisement (Instance %u) configured successfully.\n",
                  instanceNum);
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
        .sid = EXTENDED_BROADCAST_ADV_INSTANCE,
        .scan_req_notif = false,
    };

    if (!_multiAdvertiserPtr->setAdvertisingParams(EXTENDED_BROADCAST_ADV_INSTANCE, &ext_params)) {
        Serial.println("[BLE] Failed to set extended advertising parameters.");
        return false;
    }

    // The BeaconAdvertiser will overwrite this with the actual dynamic data.
    uint8_t placeholder_data[] = {0x02, 0x01, 0x06};

    if (!_multiAdvertiserPtr->setAdvertisingData(EXTENDED_BROADCAST_ADV_INSTANCE,
                                                 sizeof(placeholder_data), placeholder_data)) {
        Serial.println("[BLE] Failed to set extended advertising data.");
        return false;
    }

    _multiAdvertiserPtr->setDuration(EXTENDED_BROADCAST_ADV_INSTANCE, 0, 0);
    Serial.printf("[BLE] Extended Advertisement (Instance %u) configured successfully.\n",
                  EXTENDED_BROADCAST_ADV_INSTANCE);
    return true;
}