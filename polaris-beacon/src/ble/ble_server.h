#ifndef BLE_SERVER_H
#define BLE_SERVER_H

#include <BLECharacteristic.h>
#include <BLEServer.h>
#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>
#include <freertos/task.h>

#include <memory>
#include <string>

// Forward declaration for BLEMultiAdvertising
class BLEMultiAdvertising;

#include "../iencrypted_data_processor.h"
#include "../itoken_request_processor.h"
#include "../protocol/encrypted_message.h"
#include "../protocol/pol_request.h"

static constexpr const char* POL_SERVICE = "f44dce36-ffb2-565b-8494-25fa5a7a7cd6";
static constexpr const char* TOKEN_WRITE = "8e8c14b7-d9f0-5e5c-9da8-6961e1f33d6b";
static constexpr const char* TOKEN_INDICATE = "d234a7d8-ea1f-5299-8221-9cf2f942d3df";
static constexpr const char* ENCRYPTED_WRITE = "8ed72380-5adb-4d2d-81fb-ae6610122ee8";
static constexpr const char* ENCRYPTED_INDICATE = "079b34dd-2310-4b61-89bb-494cc67e097f";

static constexpr uint8_t LEGACY_TOKEN_ADV_INSTANCE = 0;
static constexpr uint8_t EXTENDED_BROADCAST_ADV_INSTANCE = 1;
static constexpr uint8_t NUM_ADV_INSTANCES = 2;

class BleServer {
public:
    explicit BleServer();
    ~BleServer();

    void begin(const std::string& deviceName);
    void stop();
    void queueTokenRequest(const uint8_t* data, size_t len);
    void queueEncryptedRequest(const uint8_t* data, size_t len);

    BLECharacteristic* getTokenIndicationCharacteristic() const;
    void setTokenRequestProcessor(std::unique_ptr<ITokenRequestProcessor> processor);

    BLECharacteristic* getEncryptedIndicationCharacteristic() const;
    void setEncryptedDataProcessor(std::unique_ptr<IEncryptedDataProcessor> processor);

    BLEMultiAdvertising* getMultiAdvertiser();

private:
    BleServer(const BleServer&) = delete;
    BleServer& operator=(const BleServer&) = delete;
    BleServer(BleServer&&) = delete;
    BleServer& operator=(BleServer&&) = delete;

    class ServerCallbacks : public BLEServerCallbacks {
    public:
        explicit ServerCallbacks(BleServer* parentServer);
        void onConnect(BLEServer* pServer) override;
        void onDisconnect(BLEServer* pServer) override;
        void onMtuChanged(BLEServer* pServer, esp_ble_gatts_cb_param_t* param) override;

    private:
        BleServer* _parentServer;
    };

    class TokenWriteHandler : public BLECharacteristicCallbacks {
    public:
        explicit TokenWriteHandler(BleServer* server);
        void onWrite(BLECharacteristic* pChar) override;

    private:
        BleServer* _server;
    };

    class EncryptedWriteHandler : public BLECharacteristicCallbacks {
    public:
        explicit EncryptedWriteHandler(BleServer* server);
        void onWrite(BLECharacteristic* pChar) override;

    private:
        BleServer* _server;
    };

    struct TokenRequestMessage {
        uint8_t data[PoLRequest::packedSize()];
        size_t len;
    };

    struct EncryptedRequestMessage {
        uint8_t data[512];
        size_t len;
    };

    std::unique_ptr<ITokenRequestProcessor> _tokenRequestProcessor;
    std::unique_ptr<TokenWriteHandler> _tokenWriteHandler;
    TaskHandle_t _tokenProcessorTask = nullptr;
    QueueHandle_t _tokenQueue = nullptr;
    BLECharacteristic* _tokenIndicateCharacteristic = nullptr;

    std::unique_ptr<IEncryptedDataProcessor> _encryptedDataProcessor;
    std::unique_ptr<EncryptedWriteHandler> _encryptedWriteHandler;
    TaskHandle_t _encryptedProcessorTaskHandle = nullptr;
    QueueHandle_t _encryptedQueue = nullptr;
    BLECharacteristic* _encryptedIndicateCharacteristic = nullptr;

    BLEServer* _pServer = nullptr;
    std::unique_ptr<ServerCallbacks> _serverCallbacks;
    volatile bool _shutdownRequested = false;
    std::unique_ptr<BLEMultiAdvertising> _multiAdvertiserPtr;

    static void tokenProcessorTask(void* pvParameters);
    void processTokenRequests();

    static void encryptedProcessorTask(void* pvParameters);
    void processEncryptedRequests();

    void addUserDescription(BLECharacteristic* characteristic, const std::string& description);
    bool configureTokenSrvcAdvertisement(const std::string& deviceName, uint8_t instanceNum,
                                         const char* serviceUuid);
    bool configureExtendedAdvertisement();
};

#endif  // BLE_SERVER_H