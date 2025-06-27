#ifndef OUTGOING_MESSAGE_SERVICE_H
#define OUTGOING_MESSAGE_SERVICE_H

#include <ArduinoJson.h>

#include <functional>
#include <map>
#include <queue>
#include <vector>

#include "protocol/messages/encrypted_message.h"
#include "utils/crypto_service.h"

class OutgoingMessageService {
public:
    using QueueStateChangeCallback = std::function<void(bool hasData)>;

    OutgoingMessageService();

    void begin(CryptoService* cryptoService, Preferences* prefs, QueueStateChangeCallback callback);
    void queueMessage(OperationType opType, const JsonObject& params);
    bool hasPendingMessages() const;
    std::vector<uint8_t> getNextMessageForSending();
    void handleAck(uint32_t msgId);

private:
    static constexpr const char* TAG = "[OutgoingSvc]";

    struct OutgoingMessage {
        InnerPlaintext plaintext;
    };

    CryptoService* _cryptoService = nullptr;
    Preferences* _prefs = nullptr;
    QueueStateChangeCallback _onQueueStateChange;

    std::queue<OutgoingMessage> _messageQueue;
    std::map<uint32_t, OutgoingMessage> _pendingAckMessages;
    uint32_t _nextMsgId = 1;

    void loadNextMsgId();
    void saveNextMsgId();
};
#endif  // OUTGOING_MESSAGE_SERVICE_H