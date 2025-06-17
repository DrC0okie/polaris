// src/protocol/transport/fragmentation_transport.h
#ifndef FRAGMENTATION_TRANSPORT_H
#define FRAGMENTATION_TRANSPORT_H

#include <BLECharacteristic.h>

#include <functional>
#include <memory>
#include <vector>

#include "imessage_transport.h"
#include "protocol/handlers/imessage_handler.h"

class FragmentationTransport : public IMessageHandler, public IMessageTransport {
public:
    // Factory used to inject the ownership to the BLE server
    using HandlerFactory = std::function<std::unique_ptr<IMessageHandler>(IMessageTransport&)>;

    FragmentationTransport(BLECharacteristic* indicateChar, HandlerFactory factory);

    // This method receives raw chunks from the BLE stack.
    void process(const uint8_t* chunkData, size_t len) override;

    // This method receives a full message from the wrapped handler.
    bool sendMessage(const uint8_t* fullMessageData, size_t len) override;

    // Called by BleServer when the connection MTU changes.
    void onMtuChanged(uint16_t newMtu);

private:
    enum class ReassemblyState { IDLE, REASSEMBLING };

    void resetReassembly();
    void handleFragment(const uint8_t* chunkData, size_t len);

    static constexpr const char* TAG = "[FragTransport]";
    static constexpr uint16_t GATT_HEADER_SIZE = 3;  // Opcode (1) + Handle (2)
    static constexpr uint32_t REASSEMBLY_TIMEOUT_MS = 5000;

    std::unique_ptr<IMessageHandler> _wrappedHandler;
    BLECharacteristic* _indicateChar;

    // State for reassembly (incoming)
    ReassemblyState _reassemblyState = ReassemblyState::IDLE;
    std::vector<uint8_t> _reassemblyBuffer;
    uint8_t _currentTransactionId = 0;
    unsigned long _lastPacketTimestamp = 0;

    // State for fragmentation (outgoing)
    uint16_t _maxChunkPayloadSize = 20;  // Default: 23 (MTU) - 3 (GATT) - 1 (ALFP) = 19
    uint8_t _outgoingTransactionId = 0;
};

#endif  // FRAGMENTATION_TRANSPORT_H