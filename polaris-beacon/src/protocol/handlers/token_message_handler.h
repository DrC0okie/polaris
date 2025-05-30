#ifndef TOKEN_HANDLER_H
#define TOKEN_HANDLER_H

#include <BLECharacteristic.h>

#include "../../utils/counter.h"
#include "../../utils/crypto_service.h"
#include "../pol_constants.h"
#include "imessage_handler.h"

class TokenMessageHandler : public IMessageHandler {
public:
    TokenMessageHandler(const CryptoService& cryptoService, const MinuteCounter& counter,
                        BLECharacteristic* indicationChar);

    void process(const uint8_t* requestData, size_t len) override;

private:
    const CryptoService& _cryptoService;
    const MinuteCounter& _counter;
    BLECharacteristic* _indicateChar;
};

#endif  // TOKEN_HANDLER_H