#ifndef BLE_SERVER_H
#define BLE_SERVER_H

#include <BLECharacteristic.h>
#include <BLEServer.h>
#include <freertos/FreeRTOS.h>  // For QueueHandle_t, TaskHandle_t
#include <freertos/queue.h>
#include <freertos/task.h>

#include <string>  // For std::string

#include "../ipol_request_processor.h"
#include "../protocol/pol_request.h"

static constexpr const char* SERVICE_UUID =
    "f44dce36-ffb2-565b-8494-25fa5a7a7cd6";
static constexpr const char* WRITE_UUID =
    "8e8c14b7-d9f0-5e5c-9da8-6961e1f33d6b";
static constexpr const char* INDICATE_UUID =
    "d234a7d8-ea1f-5299-8221-9cf2f942d3df";

class BleServer {
public:
    explicit BleServer(size_t maxRequestSize);
    ~BleServer();  // Will call stop()

    void begin();
    void stop();  // For graceful shutdown

    void queueRequest(const uint8_t* data, size_t len);  // Called on write
    BLECharacteristic* getIndicationCharacteristic() const;

    void setRequestProcessor(IPolRequestProcessor* processor);

private:
    class ServerCallbacks;
    class WriteHandler;

    struct RequestMessage {
        uint8_t data[PoLRequest::packedSize()];
        size_t len;
    };

    size_t _maxRequestSize;
    IPolRequestProcessor* _requestProcessor = nullptr;
    ServerCallbacks* _serverCallbacks = nullptr;
    WriteHandler* _writeHandler = nullptr;
    BLECharacteristic* _indicateCharacteristic = nullptr;
    BLEServer* _pServer = nullptr;
    QueueHandle_t _queue = nullptr;
    TaskHandle_t _processorTaskHandle = nullptr;
    volatile bool _shutdownRequested = false;
    static void processorTaskTrampoline(void* pvParameters);
    void processRequests();  // FreeRTOS task
    void addUserDescription(BLECharacteristic* characteristic,
                            const std::string& description);
};

#endif  // BLE_SERVER_H
