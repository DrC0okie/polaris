#ifndef TOKEN_HANDLER_H
#define TOKEN_HANDLER_H

#include <BLECharacteristic.h>

#include "../../utils/counter.h"
#include "../pol_constants.h"
#include "imessage_handler.h"

class TokenMessageHandler : public IMessageHandler {
public:
    TokenMessageHandler(uint32_t beacon_id, const uint8_t sk[Ed25519_SK_SIZE],
                        MinuteCounter& counter, BLECharacteristic* indicationChar);

    void process(const uint8_t* requestData, size_t len) override;

private:
    uint32_t _beacon_id;
    uint8_t _sk[Ed25519_SK_SIZE];
    MinuteCounter& _counter;
    BLECharacteristic* _indicateChar;
};

#endif  // TOKEN_HANDLER_H