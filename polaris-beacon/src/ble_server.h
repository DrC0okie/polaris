#ifndef BLE_SERVER_H
#define BLE_SERVER_H

#include "counter.h"
#include <pol_request.h>
#include "pol_handler.h"

class BleServer {
public:
    BleServer(uint32_t beacon_id, const uint8_t sk[32], const uint8_t pk[32], MinuteCounter& counter);
    void begin();
    void sendResponse(const uint8_t* data, size_t len); // Called on write

private:
    struct RequestMessage {
        uint8_t data[PoLRequest::packedSize()];
        size_t len;
    };
    uint32_t _beacon_id;
    uint8_t _sk[32];
    uint8_t _pk[32];
    MinuteCounter& _counter;
    PoLRequestHandler _handler;
    QueueHandle_t _queue;
    void handlePoLRequest(const uint8_t* data, size_t len);
    void processRequests(); // FreeRTOS task
};

#endif // BLE_SERVER_H
