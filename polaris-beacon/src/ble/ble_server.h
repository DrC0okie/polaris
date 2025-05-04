#ifndef BLE_SERVER_H
#define BLE_SERVER_H

#include <BLECharacteristic.h>

#include "../ipol_request_processor.h"

class BleServer {
public:
    explicit BleServer(size_t maxRequestSize);
    ~BleServer();
    void begin();
    void queueRequest(const uint8_t* data, size_t len);  // Called on write
    BLECharacteristic* getIndicationCharacteristic() const;
    void setRequestProcessor(IPolRequestProcessor* processor);

private:
    class ServerCallbacks;
    class WriteHandler;
    struct RequestMessage {
        uint8_t* data;
        size_t len;
    };
    size_t _maxRequestSize;
    IPolRequestProcessor* _requestProcessor = nullptr;
    ServerCallbacks* _serverCallbacks = nullptr;
    WriteHandler* _writeHandler = nullptr;
    BLECharacteristic* _indicateCharacteristic = nullptr;
    QueueHandle_t _queue;
    void processRequests();  // FreeRTOS task
    void addUserDescription(BLECharacteristic* characteristic,
                            const std::string& description);
};

#endif  // BLE_SERVER_H
