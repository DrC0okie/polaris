// src/protocol/transport/imessage_transport.h
#ifndef IMESSAGE_TRANSPORT_H
#define IMESSAGE_TRANSPORT_H

#include <cstddef>
#include <cstdint>

/**
 * @interface IMessageTransport
 * @brief An interface for sending a complete application-level message.
 *
 * Defines the contract for a transport layer that can send a full message payload. The
 * implementation is responsible for handling any underlying details, such as fragmentation.
 */
class IMessageTransport {
public:
    virtual ~IMessageTransport() = default;

    /**
     * @brief Sends a complete message.
     *
     * The implementation will handle fragmenting the message if it exceeds the
     * transport max payload size.
     *
     * @param data Pointer to the buffer containing the full message.
     * @param len The total length of the message in the buffer.
     * @return True if the sending process was successfully initiated, false otherwise.
     */
    virtual bool sendMessage(const uint8_t* data, size_t len) = 0;
};

#endif  // IMESSAGE_TRANSPORT_H