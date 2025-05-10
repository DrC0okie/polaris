#ifndef IPOL_REQUEST_PROCESSOR_H
#define IPOL_REQUEST_PROCESSOR_H

#include <cstddef>
#include <cstdint>

class ITokenRequestProcessor {
public:
    virtual ~ITokenRequestProcessor() = default;

    // Accepts raw request data, performs validation & cryptographic processing,
    // and sends back response
    virtual void process(const uint8_t* requestData, size_t len) = 0;
};

#endif