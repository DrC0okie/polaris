// src/protocol/transport/imessage_transport.h
#ifndef IMESSAGE_TRANSPORT_H
#define IMESSAGE_TRANSPORT_H

#include <cstddef>
#include <cstdint>

// An interface for sending a complete application-level message.
// The implementation will handle the underlying transport details (like fragmentation).
class IMessageTransport {
public:
    virtual ~IMessageTransport() = default;

    // Sends a complete message. Returns true if the sending process was successfully initiated.
    virtual bool sendMessage(const uint8_t* data, size_t len) = 0;
};

#endif  // IMESSAGE_TRANSPORT_H