#ifndef IMESSAGE_HANDLER_H
#define IMESSAGE_HANDLER_H

#include <cstddef>
#include <cstdint>

class IMessageHandler {
public:
    virtual ~IMessageHandler() = default;

    // Accepts raw request data, performs validation & cryptographic processing,
    // and sends back response
    virtual void process(const uint8_t* requestData, size_t len) = 0;
};

#endif  // IMESSAGE_HANDLER_H