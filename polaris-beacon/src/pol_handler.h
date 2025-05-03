#ifndef POL_HANDLER_H
#define POL_HANDLER_H

#include "pol_request.h"
#include "pol_response.h"
#include "counter.h"

class PoLRequestHandler {
public:
    PoLRequestHandler(uint32_t beacon_id, const uint8_t sk[32], const uint8_t pk[32], MinuteCounter& counter);

    // Handles request and outputs a response if valid
    bool handle(const uint8_t* data, size_t len, PoLResponse& outResponse);

private:
    uint32_t _beacon_id;
    uint8_t _sk[32];
    uint8_t _pk[32];
    MinuteCounter& _counter;
};

#endif
