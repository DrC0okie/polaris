// iencrypted_data_processor.h
#ifndef IENCRYPTED_DATA_PROCESSOR_H
#define IENCRYPTED_DATA_PROCESSOR_H

#include <cstddef>
#include <cstdint>

class IEncryptedDataProcessor {
public:
    virtual ~IEncryptedDataProcessor() = default;

    // Accepts raw encrypted data, performs decryption & processing,
    virtual void process(const uint8_t* encryptedData, size_t len) = 0;
};

#endif  // IENCRYPTED_DATA_PROCESSOR_H