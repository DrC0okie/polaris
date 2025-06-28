#ifndef OUTGOING_MESSAGE_SERVICE_H
#define OUTGOING_MESSAGE_SERVICE_H

#include <ArduinoJson.h>

#include <functional>
#include <map>
#include <queue>
#include <vector>

#include "protocol/messages/encrypted_message.h"
#include "utils/crypto_service.h"

/**
 * @class OutgoingMessageService
 * @brief Manages a queue of beacon messages to be sent to the server.
 */
class OutgoingMessageService {
public:
    /**
     * @brief A function pointer type for the queue state change callback.
     * @param hasData True if the queue now has data, false if it has become empty.
     */
    using QueueStateChangeCallback = std::function<void(bool hasData)>;

    /**
     * @brief Constructs the OutgoingMessageService.
     */
    OutgoingMessageService();

    /**
     * @brief Initializes the service with its dependencies.
     * @param cryptoService Pointer to the service for sealing messages.
     * @param prefs Pointer to the NVS storage for persisting message IDs.
     * @param callback Function to call when the queue transitions between empty and not-empty.
     */
    void begin(CryptoService* cryptoService, Preferences* prefs, QueueStateChangeCallback callback);

    /**
     * @brief Adds a new message to the queue.
     *
     * If the queue was previously empty, this will trigger the state change callback.
     * @param opType The operation type of the message to be sent.
     * @param params A JsonObject containing the payload for the message.
     */
    void queueMessage(OperationType opType, const JsonObject& params);

    /**
     * @brief Checks if there are any messages in the outgoing queue.
     * @return True if the queue is not empty, false otherwise.
     */
    bool hasPendingMessages() const;

    /**
     * @brief Retrieves the next message from the queue for sending.
     * @return A vector of bytes containing the encrypted message ready for transport.
     *         Returns an empty vector if the queue was empty or if sealing failed.
     */
    std::vector<uint8_t> getNextMessageForSending();

    /**
     * @brief Processes an ACK from the server for a previously sent message.
     *
     * This removes the corresponding message from the pending-ACK list.
     * @param msgId The message ID of the beacon original message that is being acknowledged.
     */
    void handleAck(uint32_t msgId);

private:
    /// @brief A tag used for logging from this class
    static constexpr const char* TAG = "[OutgoingSvc]";

    /**
     * @struct OutgoingMessage
     * @brief A simple struct to hold a message waiting in the queue.
     */
    struct OutgoingMessage {
        InnerPlaintext plaintext;
    };

    /// @brief Pointer to the cryptographic service provider.
    CryptoService* _cryptoService = nullptr;

    /// @brief Pointer to the NVS storage utility.
    Preferences* _prefs = nullptr;

    /// @brief Callback function to notify listeners of queue state changes.
    QueueStateChangeCallback _onQueueStateChange;

    /// @brief A FIFO queue of messages waiting to be sent.
    std::queue<OutgoingMessage> _messageQueue;

    /// @brief A map of messages that have been sent and are now awaiting an ACK from the server.
    std::map<uint32_t, OutgoingMessage> _pendingAckMessages;

    /// @brief A persistent counter for the `msgId` of beacon-originated messages.
    uint32_t _nextMsgId = 1;

    /** @brief Loads the outgoing message ID counter from NVS. */
    void loadNextMsgId();

    /** @brief Saves the outgoing message ID counter to NVS. */
    void saveNextMsgId();
};
#endif  // OUTGOING_MESSAGE_SERVICE_H