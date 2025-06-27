#include "ble_manager.h"

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
BleManager::ServerCallbacks::ServerCallbacks(BleManager* mngr) : _mngr(mngr) {
}

void BleManager::ServerCallbacks::onMtuChanged(BLEServer* _, esp_ble_gatts_cb_param_t* param) {
    Serial.printf("[BLE] Negotiated MTU: %u bytes\n", param->mtu.mtu);
    if (_mngr) {
        _mngr->updateMtu(param->mtu.mtu);
    }
}

void BleManager::ServerCallbacks::onConnect(BLEServer* _) {
    if (!_mngr)
        return;

    Serial.println("[BLE] Client connected.");
    
    // Stop the legacy advertising instance
    if (_mngr->getMultiAdvertiser()) {
        _mngr->getMultiAdvertiser()->stop(1, &LEGACY_TOKEN_ADV_INSTANCE);
    }
    // Reset the "data pending" flag via the ConnectableAdvertiser
    if (_mngr->_connectableAdvertiser) {
        _mngr->_connectableAdvertiser->setHasDataPending(false);
    }
    // Check for and send any queued outgoing messages
    if (_mngr->_outgoingMessageService && _mngr->_outgoingMessageService->hasPendingMessages()) {
        Serial.println("[BLE] Pending messages detected. Triggering send.");
        std::vector<uint8_t> message = _mngr->_outgoingMessageService->getNextMessageForSending();

        // Send the message using the transport layer's `sendMessage` method
        if (!message.empty() && _mngr->_encryptedDataTransport) {
            _mngr->_encryptedDataTransport->sendMessage(message.data(), message.size());
        }
    }
}

void BleManager::ServerCallbacks::onDisconnect(BLEServer* _) {
    if (_mngr && _mngr->getMultiAdvertiser()) {
        Serial.println("[BLE] Client disconnected. Restarting legacy "
                       "advertising instance.");
        if (!_mngr->getMultiAdvertiser()->start(1, LEGACY_TOKEN_ADV_INSTANCE)) {
            Serial.printf("[BLE] Failed to restart legacy advertising instance %d.\n",
                          LEGACY_TOKEN_ADV_INSTANCE);
        }
    }
}

// ========== CONSTRUCTOR & DESTRUCTOR ==========
BleManager::BleManager() {
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

BleManager::~BleManager() {
    stop();
}

void BleManager::begin(const std::string& deviceName) {
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
        Serial.printf("[BLE] Adding %s characteristic...\n", charWrapper->getName().c_str());
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
    BaseType_t taskRes = xTaskCreatePinnedToCore(tokenProcessorTask, "PoLProc", 6144, this, 1,
                                                 &_tokenProcessorTask, tskNO_AFFINITY);
    if (taskRes != pdPASS) {
        Serial.println("[BLE] CRITICAL: Failed to create PoL processor task!");
    }

    Serial.println("[BLE] Starting Encrypted Data processor task...");
    BaseType_t encTaskRes = xTaskCreatePinnedToCore(encryptedProcessorTask, "EncProc", 6144, this,
                                                    1, &_encryptedProcessorTask, tskNO_AFFINITY);
    if (encTaskRes != pdPASS) {
        Serial.println("[BLE] CRITICAL: Failed to create Encrypted Data processor task!");
    }

    Serial.println("[BLE] Configuration complete.");
}

// ========== STOP ==========
void BleManager::stop() {
    Serial.println("[BLE] Stopping BleManager...");
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
void BleManager::queueTokenRequest(const uint8_t* data, size_t len) {
    if (!_tokenQueue) {
        Serial.println("[BLE] Queue not initialized, dropping request.");
        return;
    }
    if (len == 0) {
        Serial.println("[BLE] Empty request received, dropping.");
        return;
    }

    if (len > sizeof(TokenRequestMessage::data)) {
        Serial.printf("[BLE] Data chunk is too large for queue buffer (%zu bytes, max "
                      "%zu). Dropping.\n",
                      len, sizeof(TokenRequestMessage::data));
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
void BleManager::queueEncryptedRequest(const uint8_t* data, size_t len) {
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
void BleManager::tokenProcessorTask(void* pvParameters) {
    BleManager* managerInstance = static_cast<BleManager*>(pvParameters);
    if (managerInstance) {
        managerInstance->processTokenRequests();
    }
    vTaskDelete(NULL);
}

void BleManager::processTokenRequests() {
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
void BleManager::encryptedProcessorTask(void* pvParameters) {
    BleManager* managerInstance = static_cast<BleManager*>(pvParameters);
    if (managerInstance) {
        managerInstance->processEncryptedRequests();
    }
    vTaskDelete(NULL);
}

void BleManager::processEncryptedRequests() {
    Serial.println("[BLE] Encrypted Data processor task started.");
    EncryptedRequestMessage msg;
    while (!_shutdownRequested) {
        if (xQueueReceive(_encryptedQueue, &msg, pdMS_TO_TICKS(100)) == pdTRUE) {
            if (_encryptedDataTransport) {
                _encryptedDataTransport->process(msg.data, msg.len);
            } else {
                Serial.println("[BLE Enc] No Encrypted Data processor set, request ignored.");
            }
        }
    }
    Serial.println("[BLE] Encrypted Data processor task shutting down.");
}

// ========== UTILS ==========

BLECharacteristic* BleManager::getCharacteristicByUUID(const BLEUUID& uuid) const {
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

void BleManager::setTokenRequestProcessor(IMessageHandler* processor) {
    _tokenRequestProcessor = processor;
}

void BleManager::setEncryptedDataProcessor(FragmentationTransport* transport) {
    _encryptedDataTransport = transport;
}

void BleManager::setConnectableAdvertiser(ConnectableAdvertiser* advertiser) {
    _connectableAdvertiser = advertiser;
}

void BleManager::setOutgoingMessageService(OutgoingMessageService* service) {
    _outgoingMessageService = service;
}

void BleManager::registerTransportForMtuUpdates(FragmentationTransport* transport) {
    _transportsForMtuUpdate.push_back(transport);
}

BLEMultiAdvertising* BleManager::getMultiAdvertiser() {
    return _multiAdvertiserPtr.get();
}

bool BleManager::configureTokenSrvcAdvertisement(const std::string& deviceName, uint8_t instanceNum,
                                                 const char* serviceUuid) {
    esp_ble_gap_ext_adv_params_t legacyParams = {
        .type = ESP_BLE_GAP_SET_EXT_ADV_PROP_LEGACY_IND,  // Legacy advertising (BLE 4.2)
        .interval_min = 0x190,  // 500ms (intervals must be multiplied by 1.25 to get seconds)
        .interval_max = 0x190,  // 500ms (intervals must be multiplied by 1.25 to get seconds)
        .channel_map = ADV_CHNL_ALL,
        .own_addr_type = BLE_ADDR_TYPE_PUBLIC,               // Same address as the device
        .filter_policy = ADV_FILTER_ALLOW_SCAN_ANY_CON_ANY,  // Allows scan & connection
        .tx_power = EXT_ADV_TX_PWR_NO_PREFERENCE,            // No preference for tx power
        .primary_phy = ESP_BLE_GAP_PHY_1M,                   // 1Mb/s
        .secondary_phy = ESP_BLE_GAP_PHY_1M,                 // 1Mb/s
        .sid = instanceNum,
        .scan_req_notif = false,
    };

    if (!_multiAdvertiserPtr->setAdvertisingParams(instanceNum, &legacyParams)) {
        Serial.println("[BLE] Failed to set legacy advertising parameters.");
        return false;
    }

    // Default advertising data that can be overridden by the connectable advertiser
    BLEAdvertisementData advData;
    advData.setFlags(ESP_BLE_ADV_FLAG_GEN_DISC | ESP_BLE_ADV_FLAG_BREDR_NOT_SPT);

    uint8_t manufDataPayload[6];
    uint16_t manufId = MANUFACTURER_ID;
    // Manufacturer id for development/testing
    memcpy(manufDataPayload, &manufId, sizeof(manufId));

    // Here we specify the beacon id in the adv data
    memcpy(manufDataPayload + sizeof(manufId), &BEACON_ID, sizeof(BEACON_ID));
    advData.setManufacturerData(
        std::string(reinterpret_cast<const char*>(manufDataPayload), sizeof(manufDataPayload)));
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
    return true;
}

bool BleManager::configureExtendedAdvertisement() {
    esp_ble_gap_ext_adv_params_t extParams = {
        // Non-Connectable and Non-Scannable Undirected advertising
        .type = ESP_BLE_GAP_SET_EXT_ADV_PROP_NONCONN_NONSCANNABLE_UNDIRECTED,
        .interval_min = 0x320,  // 1s (intervals must be multiplied by 1.25 to get seconds)
        .interval_max = 0x320,  // 1s (intervals must be multiplied by 1.25 to get seconds)
        .channel_map = ADV_CHNL_ALL,
        .own_addr_type = BLE_ADDR_TYPE_PUBLIC,
        .filter_policy = ADV_FILTER_ALLOW_SCAN_ANY_CON_ANY,
        .tx_power = EXT_ADV_TX_PWR_NO_PREFERENCE,
        .primary_phy = ESP_BLE_GAP_PHY_1M,       // 1Mb/s
        .secondary_phy = ESP_BLE_GAP_PHY_CODED,  // 500 kb/s but longer range
        .sid = EXTENDED_BROADCAST_ADV_INSTANCE,
        .scan_req_notif = false,
    };

    if (!_multiAdvertiserPtr->setAdvertisingParams(EXTENDED_BROADCAST_ADV_INSTANCE, &extParams)) {
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
    return true;
}

//  Informs the transport layers
void BleManager::updateMtu(uint16_t newMtu) {
    for (auto transport : _transportsForMtuUpdate) {
        if (transport) {
            transport->onMtuChanged(newMtu);
        }
    }
}