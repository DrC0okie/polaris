// src/protocol/transport/fragmentation_transport.cpp
#include "fragmentation_transport.h"

#include <HardwareSerial.h>

#include "alfp.h"

FragmentationTransport::FragmentationTransport(BLECharacteristic* indicateChar,
                                               HandlerFactory factory)
    : _indicateChar(indicateChar) {
    _reassemblyBuffer.reserve(512);  // Pre-allocate some memory

    // We call the factory to create the wrapped handler. So we pass a reference to ourselves to the
    // factory.
    if (factory) {
        _wrappedHandler = factory(*this);
    }
}

void FragmentationTransport::onMtuChanged(uint16_t newMtu) {
    // New MTU - GATT Header - Our ALFP Header
    _maxChunkPayloadSize = newMtu - GATT_HEADER_SIZE - alfp::Header::SIZE;
    Serial.printf("%s MTU updated to %u, max chunk payload is now %u bytes.\n", TAG, newMtu,
                  _maxChunkPayloadSize);
}

// --- INCOMING DATA LOGIC ---
void FragmentationTransport::resetReassembly() {
    _reassemblyBuffer.clear();
    _reassemblyState = ReassemblyState::IDLE;
    Serial.printf("%s Reassembly state reset.\n", TAG);
}

void FragmentationTransport::process(const uint8_t* chunkData, size_t len) {
    if (len < alfp::Header::SIZE) {
        Serial.printf("%s Chunk too small (%zu bytes), ignoring.\n", TAG, len);
        return;
    }

    // Check for reassembly timeout
    if (_reassemblyState == ReassemblyState::REASSEMBLING &&
        millis() - _lastPacketTimestamp > REASSEMBLY_TIMEOUT_MS) {
        Serial.printf("%s Reassembly timed out. Discarding partial message.\n", TAG);
        resetReassembly();
    }

    alfp::Header header;
    header.control = chunkData[0];
    const uint8_t* payload = chunkData + alfp::Header::SIZE;
    size_t payloadLen = len - alfp::Header::SIZE;
    uint8_t packetType = header.control & alfp::MASK_TYPE;
    uint8_t transactionId = header.control & alfp::MASK_TRANSACTION_ID;

    if (packetType == alfp::FLAG_UNFRAGMENTED) {
        if (_reassemblyState != ReassemblyState::IDLE) {
            Serial.printf("%s WARNING: Received UNFRAGMENTED packet while reassembling. Discarding "
                          "old data.\n",
                          TAG);
            resetReassembly();
        }
        Serial.printf("%s Received unfragmented message (%zu bytes).\n", TAG, payloadLen);
        _wrappedHandler->process(payload, payloadLen);
        return;
    }

    // Handle fragmented packets
    switch (packetType) {
        case alfp::FLAG_START:
            if (_reassemblyState == ReassemblyState::REASSEMBLING) {
                Serial.printf(
                    "%s WARNING: Received START while already reassembling. Starting over.\n", TAG);
            }
            resetReassembly();
            _reassemblyState = ReassemblyState::REASSEMBLING;
            _currentTransactionId = transactionId;
            _reassemblyBuffer.insert(_reassemblyBuffer.end(), payload, payload + payloadLen);
            _lastPacketTimestamp = millis();
            break;

        case alfp::FLAG_MIDDLE:
            if (_reassemblyState != ReassemblyState::REASSEMBLING ||
                transactionId != _currentTransactionId) {
                Serial.printf("%s Received out-of-sequence MIDDLE packet. Ignoring.\n", TAG);
                resetReassembly();  // Safety reset
                return;
            }
            _reassemblyBuffer.insert(_reassemblyBuffer.end(), payload, payload + payloadLen);
            _lastPacketTimestamp = millis();
            break;

        case alfp::FLAG_END:
            if (_reassemblyState != ReassemblyState::REASSEMBLING ||
                transactionId != _currentTransactionId) {
                Serial.printf("%s Received out-of-sequence END packet. Ignoring.\n", TAG);
                resetReassembly();  // Safety reset
                return;
            }
            _reassemblyBuffer.insert(_reassemblyBuffer.end(), payload, payload + payloadLen);
            Serial.printf("%s Reassembly complete. Total size: %zu bytes.\n", TAG,
                          _reassemblyBuffer.size());
            _wrappedHandler->process(_reassemblyBuffer.data(), _reassemblyBuffer.size());
            resetReassembly();
            break;

        default:
            Serial.printf("%s Unknown packet type in ALFP header. Ignoring.\n", TAG);
            break;
    }
}

// --- OUTGOING DATA LOGIC ---
bool FragmentationTransport::sendMessage(const uint8_t* fullMessageData, size_t len) {
    if (!_indicateChar) {
        Serial.printf("%s Cannot send, no client subscribed.\n", TAG);
        return false;
    }

    // Increment transaction ID for this new message transfer
    _outgoingTransactionId = (_outgoingTransactionId + 1) & alfp::MASK_TRANSACTION_ID;

    // Check if message fits in a single packet (Unfragmented optimization)
    if (len <= _maxChunkPayloadSize) {
        std::vector<uint8_t> packet(len + alfp::Header::SIZE);
        packet[0] = alfp::FLAG_UNFRAGMENTED | _outgoingTransactionId;
        memcpy(packet.data() + alfp::Header::SIZE, fullMessageData, len);

        _indicateChar->setValue(packet.data(), packet.size());
        _indicateChar->indicate();
        Serial.printf("%s Sent unfragmented message (%zu bytes).\n", TAG, len);
        return true;
    }

    // Message needs fragmentation
    Serial.printf("%s Fragmenting message of size %zu into chunks of max %u bytes.\n", TAG, len,
                  _maxChunkPayloadSize);
    size_t bytesSent = 0;
    bool isFirst = true;

    while (bytesSent < len) {
        size_t chunkSize = std::min((size_t)_maxChunkPayloadSize, len - bytesSent);

        std::vector<uint8_t> packet(chunkSize + alfp::Header::SIZE);
        uint8_t packetType;

        if (isFirst) {
            packetType = alfp::FLAG_START;
            isFirst = false;
        } else if (bytesSent + chunkSize >= len) {
            packetType = alfp::FLAG_END;
        } else {
            packetType = alfp::FLAG_MIDDLE;
        }

        packet[0] = packetType | _outgoingTransactionId;
        memcpy(packet.data() + alfp::Header::SIZE, fullMessageData + bytesSent, chunkSize);

        _indicateChar->setValue(packet.data(), packet.size());
        _indicateChar->indicate();
        Serial.printf("%s Sent chunk type %02X, size %zu.\n", TAG, packetType, chunkSize);

        bytesSent += chunkSize;
        vTaskDelay(pdMS_TO_TICKS(10));  // Small delay for flow control, can be tuned
    }

    Serial.printf("%s Fragmentation complete for transaction %u.\n", TAG, _outgoingTransactionId);
    return true;
}