#include "encrypted_message_handler.h"

#include <ArduinoJson.h>
#include <HardwareSerial.h>
#include <Ticker.h>

#include <vector>

#include "../crypto.h"
#include "commands/command_factory.h"

EncryptedMessageHandler::EncryptedMessageHandler(const CryptoService& cryptoService,
                                                 const BeaconCounter& beaconEventCounter,
                                                 Preferences& prefs, IMessageTransport& transport,
                                                 CommandFactory& commandFactory,
                                                 OutgoingMessageService& outgoingMessageService,
                                                 KeyManager& keyManager)
    : _cryptoService(cryptoService),
      _beaconEventCounter(beaconEventCounter),
      _prefs(prefs),
      _transport(transport),
      _commandFactory(commandFactory),
      _outgoingMessageService(outgoingMessageService),
      _keyManager(keyManager),
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
        Serial.printf(
            "%s Wrong beacon id (received id = %u) in received encrypted message, dropping.\n", TAG,
            receivedMsg.beaconIdAd);
        return;
    }

    InnerPlaintext innerPtReceived;
    bool decryptionSuccess = false;
    if (!receivedMsg.unseal(innerPtReceived)) {
        Serial.printf("%s Unseal failed. Trying with pending key...\n", TAG);

        // Derive a temp shared key with the pending key
        uint8_t tempAeadKey[SHARED_KEY_SIZE];
        if (_keyManager.deriveAEADSharedKeyWithPendingKey(tempAeadKey)) {
            // Try to unseal with the temp shared key
            if (receivedMsg.unseal(innerPtReceived, tempAeadKey)) {
                // Success, it's the end of the key rotation
                Serial.printf("%s Alternate decryption successful!\n", TAG);

                // The type MUST be RotateKeyFinish
                if (static_cast<OperationType>(innerPtReceived.opType) !=
                    OperationType::RotateKeyFinish) {
                    Serial.printf("%s SECURITY ALERT: Decrypted with pending key, but opType is "
                                  "not RotateKeyFinish!\n",
                                  TAG);
                    // Do nothing, drop the message
                    return;
                }

                // The message is valid, we can activate the pending key
                if (!_keyManager.activateNewX25519KeyPair()) {
                    Serial.printf("%s CRITICAL: Failed to activate new key pair after successful "
                                  "alternate decryption.\n",
                                  TAG);
                    return;
                }
                decryptionSuccess = true;
            }
        }
    }

    Serial.printf("%s Decrypted: msgId=%u, msgType=%u, opType=%u, beaconCnt=%u, payloadLen=%u\n",
                  TAG, innerPtReceived.msgId, innerPtReceived.msgType, innerPtReceived.opType,
                  innerPtReceived.beaconCnt, innerPtReceived.actualPayloadLength);

    if (innerPtReceived.msgType == MSG_TYPE_REQ) {
        Serial.printf("%s REQ received. opType: %u, msgId: %u. Processing...\n", TAG,
                      innerPtReceived.opType, innerPtReceived.msgId);

        // Handles the command and the ack / err as well
        handleIncomingCommand(innerPtReceived);
    } else if (innerPtReceived.msgType == MSG_TYPE_ACK) {
        Serial.printf("%s ACK for our msgId %u. (PayloadLen: %u)\n", TAG, innerPtReceived.msgId,
                      innerPtReceived.actualPayloadLength);
        _outgoingMessageService.handleAck(innerPtReceived.msgId);

    } else if (innerPtReceived.msgType == MSG_TYPE_ERR) {
        Serial.printf("%s ERR for our msgId %u. (PayloadLen: %u)\n", TAG, innerPtReceived.msgId,
                      innerPtReceived.actualPayloadLength);
        if (innerPtReceived.actualPayloadLength > 0) {
            Serial.printf("%s Error code from server: %02X\n", TAG, innerPtReceived.payload[0]);
            _outgoingMessageService.handleAck(innerPtReceived.msgId);
        }

    } else {
        Serial.printf("%s Unknown opType %u. Ignoring.\n", TAG, innerPtReceived.opType);
        // Optionally send an ERR
        sendErr(innerPtReceived.msgId, innerPtReceived.opType, 0x01);
    }
}

void EncryptedMessageHandler::sendAck(uint32_t originalMsgId, uint8_t originalOpType,
                                      const uint8_t* payload, size_t payloadLen) {
    InnerPlaintext ackInnerPt;
    ackInnerPt.msgId = _nextResponseMsgId;  // Use beacon own unique msgId for this response
    ackInnerPt.msgType = MSG_TYPE_ACK;
    ackInnerPt.opType = originalOpType;  // Echo opType of the request it's ACKing
    ackInnerPt.beaconCnt = _beaconEventCounter.getValue();

    if (payload != nullptr && payloadLen > 0) {
        if (payloadLen <= sizeof(ackInnerPt.payload)) {
            memcpy(ackInnerPt.payload, payload, payloadLen);
            ackInnerPt.actualPayloadLength = payloadLen;
        } else {
            Serial.printf("%s ERROR: ACK payload is too large (%zu bytes)!\n", TAG, payloadLen);
            // Send ERR instead ? for the moment let's drop
            return;
        }
    } else {
        ackInnerPt.actualPayloadLength = 0;
    }

    EncryptedMessage ackMsgToSend(_cryptoService);
    if (ackMsgToSend.seal(ackInnerPt, _beaconIdForAd)) {
        size_t ackLen = ackMsgToSend.packedSize();
        if (ackLen > 0 && ackLen < MAX_BLE_PAYLOAD_SIZE) {
            std::vector<uint8_t> ackBuffer(ackLen);
            ackMsgToSend.toBytes(ackBuffer.data(), ackBuffer.size());

            // Delegate sending the full message to the transport layer
            if (!_transport.sendMessage(ackBuffer.data(), ackBuffer.size())) {
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

void EncryptedMessageHandler::handleIncomingCommand(const InnerPlaintext& pt) {
    JsonObject params;
    JsonDocument doc;

    // Only try to parse JSON if there is a payload
    if (pt.actualPayloadLength > 2) {
        DeserializationError error = deserializeJson(doc, pt.payload, pt.actualPayloadLength);
        if (error) {
            Serial.printf("%s Failed to parse JSON payload: %s. Ignoring command.\n", TAG,
                          error.c_str());
            sendErr(pt.msgId, pt.opType, 0x02);  // 0x02: Bad Payload
            return;
        }
        params = doc["params"].as<JsonObject>();
    }

    // Cast the raw byte to our strongly-typed enum
    OperationType opType = static_cast<OperationType>(pt.opType);

    // Use the factory to create a command object
    auto command = _commandFactory.createCommand(opType, params, _keyManager);

    if (command) {
        CommandResult result = command->execute();
        if (result.success) {
            // Envoyer un ACK, avec le payload s'il y en a un
            sendAck(pt.msgId, pt.opType, result.responsePayload.data(),
                    result.responsePayload.size());
        } else {
            // Envoyer un ERR si la commande a échoué
            sendErr(pt.msgId, pt.opType, 0x04);  // 0x04: Command execution failed
        }
    } else {
        // Handle unknown command
        Serial.printf("%s Unknown opType %u received.\n", TAG, pt.opType);
        sendErr(pt.msgId, pt.opType, 0x03);  // Unknown opType
    }
}