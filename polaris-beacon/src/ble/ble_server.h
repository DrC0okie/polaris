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

#include "../ipol_request_processor.h"
#include "../protocol/pol_request.h"

static constexpr const char* SERVICE_UUID = "f44dce36-ffb2-565b-8494-25fa5a7a7cd6";
static constexpr const char* WRITE_UUID = "8e8c14b7-d9f0-5e5c-9da8-6961e1f33d6b";
static constexpr const char* INDICATE_UUID = "d234a7d8-ea1f-5299-8221-9cf2f942d3df";

static constexpr uint8_t LEGACY_ADV_INSTANCE = 0;
static constexpr uint8_t EXTENDED_ADV_INSTANCE = 1;
static constexpr uint8_t NUM_ADV_INSTANCES = 2;  // Total number of advertising instances

static constexpr uint8_t LEGACY_ADV_SID = 0;
static constexpr uint8_t EXTENDED_ADV_SID = 1;

class BleServer {
public:
    explicit BleServer(size_t maxRequestSize);
    ~BleServer();

    void begin(const std::string& deviceName);
    void stop();
    void queueRequest(const uint8_t* data, size_t len);
    BLECharacteristic* getIndicationCharacteristic() const;
    void setRequestProcessor(std::unique_ptr<IPolRequestProcessor> processor);
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

    class WriteHandler : public BLECharacteristicCallbacks {
    public:
        explicit WriteHandler(BleServer* server);
        void onWrite(BLECharacteristic* pChar) override;

    private:
        BleServer* _server;
    };

    struct RequestMessage {
        uint8_t data[PoLRequest::packedSize()];
        size_t len;
    };

    size_t _maxRequestSize;
    BLEServer* _pServer = nullptr;
    std::unique_ptr<IPolRequestProcessor> _requestProcessor;
    std::unique_ptr<ServerCallbacks> _serverCallbacks;
    std::unique_ptr<WriteHandler> _writeHandler;
    QueueHandle_t _queue = nullptr;
    TaskHandle_t _processorTaskHandle = nullptr;
    volatile bool _shutdownRequested = false;
    std::unique_ptr<BLEMultiAdvertising> _multiAdvertiserPtr;
    BLECharacteristic* _indicateCharacteristic = nullptr;

    static void processorTaskTrampoline(void* pvParameters);
    void processRequests();
    void addUserDescription(BLECharacteristic* characteristic, const std::string& description);
    bool configureLegacyAdvertisement(const std::string& deviceName);
    bool configureExtendedAdvertisement();
};

#endif  // BLE_SERVER_H