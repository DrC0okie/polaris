#ifndef ENCRYPTED_MESSAGE_HANDLER_H
#define ENCRYPTED_MESSAGE_HANDLER_H

#include <BLECharacteristic.h>

#include "../../utils/counter.h"
#include "../pol_constants.h"
#include "imessage_handler.h"

class EncryptedMessageHandler : public IMessageHandler {
public:
    EncryptedMessageHandler(BLECharacteristic* indicationChar);

    void process(const uint8_t* encryptedData, size_t len) override;

private:
    BLECharacteristic* _indicateChar;
};

#endif  // ENCRYPTED_MESSAGE_HANDLER_H