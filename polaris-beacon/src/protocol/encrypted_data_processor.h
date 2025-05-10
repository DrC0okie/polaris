#ifndef ENCRYPTED_DATA_PROCESSOR_H
#define ENCRYPTED_DATA_PROCESSOR_H

#include <BLECharacteristic.h>

#include "../iencrypted_data_processor.h"
#include "../utils/counter.h"
#include "pol_constants.h"

class EncryptedDataProcessor : public IEncryptedDataProcessor {
public:
    EncryptedDataProcessor(BLECharacteristic* indicationChar);

    void process(const uint8_t* encryptedData, size_t len) override;

private:
    BLECharacteristic* _indicateChar;
};

#endif