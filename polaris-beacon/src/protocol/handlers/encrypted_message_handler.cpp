#include "encrypted_message_handler.h"

#include <HardwareSerial.h>

#include "../crypto.h"
#include "../messages/encrypted_message.h"

EncryptedMessageHandler::EncryptedMessageHandler(const CryptoService& cryptoService,
                                                 const MinuteCounter& beaconEventCounter,
                                                 Preferences& prefs, IMessageTransport& transport)
    : _cryptoService(cryptoService),
      _beaconEventCounter(beaconEventCounter),
      _prefs(prefs),  // Store NVS reference
      _transport(transport),
      _beaconIdForAd(BEACON_ID),
      _nextResponseMsgId(0) {
    loadNextResponseMsgId();  // Load from NVS or initialize
}

void EncryptedMessageHandler::loadNextResponseMsgId() {
    _nextResponseMsgId = _prefs.getUInt(NVS_ENC_MSG_ID_COUNTER, 0);
    if (_nextResponseMsgId == 0) {
        Serial.printf("%s No prior msgId found in NVS or was zero, starting/using %u.\n", TAG,
                      _nextResponseMsgId);
    }
}

void EncryptedMessageHandler::saveNextResponseMsgId() {
    if (!_prefs.putUInt(NVS_ENC_MSG_ID_COUNTER, _nextResponseMsgId)) {
        Serial.printf("%s ERROR: Failed to save nextResponseMsgId %u to NVS!\n", TAG,
                      _nextResponseMsgId);
    }
}

void EncryptedMessageHandler::process(const uint8_t* data, size_t len) {
    if (len == 0 || len >= MAX_BLE_PAYLOAD_SIZE) {
        Serial.printf("%s Invalid length: %zu\n", TAG, len);
        return;
    }
    Serial.printf("%s Received %zu encrypted bytes.\n", TAG, len);

    EncryptedMessage receivedMsg(_cryptoService);
    if (!receivedMsg.fromBytes(data, len)) {
        Serial.printf("%s Failed to parse EncryptedMessage structure.\n", TAG);
        return;
    }

    // Check if beacon id is correct
    if (receivedMsg.beaconIdAd != _beaconIdForAd) {
        Serial.printf("%s Wrong beacon id in received encrypted message, dropping.\n", TAG);
        return;
    }

    InnerPlaintext innerPtReceived;
    if (!receivedMsg.unseal(innerPtReceived)) {
        Serial.printf("%s Failed to unseal/decrypt message.\n", TAG);
        return;
    }

    Serial.printf("%s Decrypted: msgId=%u, msgType=%u, opType=%u, beaconCnt=%u, payloadLen=%u\n",
                  TAG, innerPtReceived.msgId, innerPtReceived.msgType, innerPtReceived.opType,
                  innerPtReceived.beaconCnt, innerPtReceived.actualPayloadLength);

    if (innerPtReceived.opType == MSG_TYPE_REQ) {
        Serial.printf("%s REQ, msgId %u. Processing...\n", TAG, innerPtReceived.msgId);
        // TODO: Process logic. For the moment we just send an ACK
        sendAck(innerPtReceived.msgId, innerPtReceived.opType);
    } else if (innerPtReceived.opType == MSG_TYPE_ACK) {
        Serial.printf("%s ACK for our msgId %u. (PayloadLen: %u)\n", TAG, innerPtReceived.msgId,
                      innerPtReceived.actualPayloadLength);
        // TODO: Handle ACK (e.g., confirm command completion)
    } else if (innerPtReceived.opType == MSG_TYPE_ERR) {
        Serial.printf("%s ERR for our msgId %u. (PayloadLen: %u)\n", TAG, innerPtReceived.msgId,
                      innerPtReceived.actualPayloadLength);
        if (innerPtReceived.actualPayloadLength > 0) {
            Serial.printf("%s Error code from server: %02X\n", TAG, innerPtReceived.payload[0]);
        }
        // TODO: Handle ERR
    } else {
        Serial.printf("%s Unknown opType %u. Ignoring.\n", TAG, innerPtReceived.opType);
        // Optionally send an ERR
        // sendErr(innerPtReceived.msgId, innerPtReceived.opType, ERROR_CODE_UNKNOWN_OP_TYPE);
    }
}

void EncryptedMessageHandler::sendAck(uint32_t originalMsgId, uint8_t originalOpType) {
    InnerPlaintext ackInnerPt;
    ackInnerPt.msgId = _nextResponseMsgId;  // Use beacon's own unique msgId for this response
    ackInnerPt.msgType = MSG_TYPE_ACK;
    ackInnerPt.opType = originalOpType;  // Echo opType of the request it's ACKing
    ackInnerPt.beaconCnt = _beaconEventCounter.getValue();
    ackInnerPt.actualPayloadLength = 0;  // No payload for a simple ACK

    EncryptedMessage ackMsgToSend(_cryptoService);
    if (ackMsgToSend.seal(ackInnerPt, _beaconIdForAd)) {
        size_t ackLen = ackMsgToSend.packedSize();
        if (ackLen > 0 && ackLen < MAX_BLE_PAYLOAD_SIZE) {
            uint8_t ackBuffer[MAX_BLE_PAYLOAD_SIZE];                // Use max size buffer
            ackMsgToSend.toBytes(ackBuffer, MAX_BLE_PAYLOAD_SIZE);  // Pass buffer size

            // Delegate sending the full message to the transport layer
            if (!_transport.sendMessage(ackBuffer, ackLen)) {
                Serial.println("[Processor] Failed to send response via transport layer.");
            }
            Serial.printf("%s ACK sent (msgId %u for original req_msgId %u).\n", TAG,
                          ackInnerPt.msgId, originalMsgId);

            _nextResponseMsgId++;     // Increment for next response
            saveNextResponseMsgId();  // Save to NVS
        } else {
            Serial.printf("%s ACK msg too large/empty. Len: %zu\n", TAG, ackLen);
        }
    } else {
        Serial.printf("%s Failed to seal ACK for req_msgId %u.\n", TAG, originalMsgId);
    }
}

void EncryptedMessageHandler::sendErr(uint32_t originalMsgId, uint8_t originalOpType,
                                      uint8_t errorCode) {
    InnerPlaintext errInnerPt;
    errInnerPt.msgId = _nextResponseMsgId;
    errInnerPt.msgType = MSG_TYPE_ERR;
    errInnerPt.opType = originalOpType;
    errInnerPt.beaconCnt = _beaconEventCounter.getValue();
    errInnerPt.payload[0] = errorCode;  // Simple error code as payload
    errInnerPt.actualPayloadLength = 1;

    EncryptedMessage errMsgToSend(_cryptoService);
    if (errMsgToSend.seal(errInnerPt, _beaconIdForAd)) {
        size_t errLen = errMsgToSend.packedSize();
        if (errLen > 0 && errLen < MAX_BLE_PAYLOAD_SIZE) {
            uint8_t errBuffer[MAX_BLE_PAYLOAD_SIZE];
            errMsgToSend.toBytes(errBuffer, MAX_BLE_PAYLOAD_SIZE);

            if (!_transport.sendMessage(errBuffer, errLen)) {
                Serial.println("[Processor] Failed to send response via transport layer.");
            }
            Serial.printf("%s ERR (code %u) sent for req_msgId %u.\n", TAG, errorCode,
                          originalMsgId);

            _nextResponseMsgId++;     // Increment for next response
            saveNextResponseMsgId();  // Save to NVS
        } else {
            Serial.printf("%s ERR msg too large/empty. Len: %zu\n", TAG, errLen);
        }
    } else {
        Serial.printf("%s Failed to seal ERR for req_msgId %u.\n", TAG, originalMsgId);
    }
}