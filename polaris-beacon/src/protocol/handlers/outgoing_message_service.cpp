#include "outgoing_message_service.h"

#include <HardwareSerial.h>

OutgoingMessageService::OutgoingMessageService() {
}

void OutgoingMessageService::begin(CryptoService* cryptoService, Preferences* prefs,
                                   QueueStateChangeCallback callback) {
    _cryptoService = cryptoService;
    _prefs = prefs;
    _onQueueStateChange = callback;
    loadNextMsgId();
}

void OutgoingMessageService::loadNextMsgId() {
    _nextMsgId = _prefs->getUInt("out_msg_id_ctr", 1);
    Serial.printf("%s Loaded next outgoing message ID: %u\n", TAG, _nextMsgId);
}

void OutgoingMessageService::saveNextMsgId() {
    if (!_prefs->putUInt("out_msg_id_ctr", _nextMsgId)) {
        Serial.printf("%s ERROR: Failed to save outgoing message ID %u to NVS!\n", TAG, _nextMsgId);
    }
}

void OutgoingMessageService::queueMessage(OperationType opType, const JsonObject& params) {
    bool wasEmpty = _messageQueue.empty();

    OutgoingMessage msg;
    msg.plaintext.msgId = _nextMsgId++;
    saveNextMsgId();
    msg.plaintext.msgType = MSG_TYPE_REQ;
    msg.plaintext.opType = static_cast<uint8_t>(opType);
    msg.plaintext.beaconCnt = 0;

    if (!params.isNull()) {
        std::string payloadStr;
        serializeJson(params, payloadStr);
        size_t len = payloadStr.length();
        if (len > sizeof(msg.plaintext.payload)) {
            Serial.printf("%s ERROR: Payload for opType %u is too large.\n", TAG,
                          msg.plaintext.opType);
            _nextMsgId--;  // Roll back ID on failure
            saveNextMsgId();
            return;
        }
        memcpy(msg.plaintext.payload, payloadStr.c_str(), len);
        msg.plaintext.actualPayloadLength = len;
    } else {
        msg.plaintext.actualPayloadLength = 0;
    }

    _messageQueue.push(msg);
    Serial.printf("%s Queued message with ID %u, opType %u. Queue size: %zu\n", TAG,
                  msg.plaintext.msgId, msg.plaintext.opType, _messageQueue.size());

    if (wasEmpty && _onQueueStateChange) {
        _onQueueStateChange(true);
    }
}

bool OutgoingMessageService::hasPendingMessages() const {
    return !_messageQueue.empty();
}

std::vector<uint8_t> OutgoingMessageService::getNextMessageForSending() {
    if (_messageQueue.empty()) {
        return {};
    }

    OutgoingMessage msg = _messageQueue.front();
    _messageQueue.pop();

    EncryptedMessage encryptedMsg(*_cryptoService);
    if (!encryptedMsg.seal(msg.plaintext, BEACON_ID)) {
        Serial.printf("%s Failed to seal message ID %u.\n", TAG, msg.plaintext.msgId);
        return {};
    }

    _pendingAckMessages[msg.plaintext.msgId] = msg;
    Serial.printf("%s Encrypted and moved message ID %u to pending ACK list.\n", TAG,
                  msg.plaintext.msgId);

    if (_messageQueue.empty() && _onQueueStateChange) {
        _onQueueStateChange(false);
    }

    std::vector<uint8_t> buffer(encryptedMsg.packedSize());
    encryptedMsg.toBytes(buffer.data(), buffer.size());
    return buffer;
}

void OutgoingMessageService::handleAck(uint32_t msgId) {
    auto it = _pendingAckMessages.find(msgId);
    if (it != _pendingAckMessages.end()) {
        _pendingAckMessages.erase(it);
        Serial.printf("%s Received ACK for message ID %u. Removed from pending list.\n", TAG,
                      msgId);
    } else {
        Serial.printf("%s Received stray ACK for unknown message ID %u.\n", TAG, msgId);
    }
}