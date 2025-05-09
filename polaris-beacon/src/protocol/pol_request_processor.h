#ifndef POLREQUEST_PROCESSOR_H
#define POLREQUEST_PROCESSOR_H

#include <BLECharacteristic.h>

#include "../utils/counter.h"
#include "pol_constants.h"
#include "ipol_request_processor.h"

class PoLRequestProcessor : public IPolRequestProcessor {
public:
    PoLRequestProcessor(uint32_t beacon_id, const uint8_t sk[POL_SK_SIZE], MinuteCounter& counter,
                        BLECharacteristic* indicationChar);

    void process(const uint8_t* requestData, size_t len) override;

private:
    uint32_t _beacon_id;
    uint8_t _sk[POL_SK_SIZE];
    MinuteCounter& _counter;
    BLECharacteristic* _indicateChar;
};

#endif