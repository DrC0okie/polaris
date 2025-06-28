#ifndef IMESSAGE_HANDLER_H
#define IMESSAGE_HANDLER_H

#include <cstddef>
#include <cstdint>

/**
 * @interface IMessageHandler
 * @brief An interface for any class that processes a complete, reassembled message.
 */
class IMessageHandler {
public:
    virtual ~IMessageHandler() = default;

    /**
     * @brief Processes a complete, reassembled message payload.
     *
     * @param requestData Pointer to the buffer containing the full message.
     * @param len The length of the message in the buffer.
     */
    virtual void process(const uint8_t* requestData, size_t len) = 0;
};

#endif  // IMESSAGE_HANDLER_H