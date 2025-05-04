#ifndef IPOL_REQUEST_PROCESSOR_H
#define IPOL_REQUEST_PROCESSOR_H

#include <cstddef>
#include <cstdint>

class IPolRequestProcessor {
public:
    virtual ~IPolRequestProcessor() = default;

    // Accepts raw request data, performs validation & cryptographic processing,
    // and sends back response
    virtual void process(const uint8_t* requestData, size_t len) = 0;
};

#endif