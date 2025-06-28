#ifndef DATA_PULL_HANDLER_H
#define DATA_PULL_HANDLER_H

#include "imessage_handler.h"
#include "outgoing_message_service.h"
#include "protocol/transport/fragmentation_transport.h"

/**
 * @class DataPullHandler
 * @brief Handles an explicit request from a client to pull queued data.
 *
 * This handler is triggered when a client writes to the `PULL_DATA_WRITE`
 * characteristic. Its responsibility is to check the `OutgoingMessageService`
 * for a pending message and, if one exists, send it over the provided transport layer.
 */
class DataPullHandler : public IMessageHandler {
public:
    /**
     * @brief Constructs the DataPullHandler.
     * @param service Reference to the service that manages the outgoing message queue.
     * @param transport Reference to the transport layer used for sending the message.
     */
    DataPullHandler(OutgoingMessageService& service, FragmentationTransport& transport);

    /**
     * @brief Processes the pull request trigger.
     *
     * The content of the request does not matter; the call itself is the trigger.
     * @param requestData Not used.
     * @param len Not used.
     */
    void process(const uint8_t* requestData, size_t len) override;

private:
    /// @brief A tag used for logging from this class.
    static constexpr const char* TAG = "[DataPullHandler]";

    /// @brief Reference to the outgoing message queue service.
    OutgoingMessageService& _service;

    /// @brief Reference to the transport layer for sending the message.
    FragmentationTransport& _transport;
};

#endif  // DATA_PULL_HANDLER_H