// src/protocol/transport/fragmentation_transport.h
#ifndef FRAGMENTATION_TRANSPORT_H
#define FRAGMENTATION_TRANSPORT_H

#include <BLECharacteristic.h>

#include <functional>
#include <memory>
#include <vector>

#include "imessage_transport.h"
#include "protocol/handlers/imessage_handler.h"

/**
 * @class FragmentationTransport
 * @brief Implements message fragmentation and reassembly over a BLE characteristic.
 *
 * As an `IMessageHandler`, it receives raw data chunks from the BLE stack,
 * reassembles them into a complete message, and passes the result to a wrapped
 * protocol-level handler.
 * As an `IMessageTransport`, it receives a complete message from a handler,
 * fragments it into suitably sized chunks, and sends them via BLE indications.
 */
class FragmentationTransport : public IMessageHandler, public IMessageTransport {
public:
    /**
     * @brief A factory function type used to create and inject the protocol-level message handler.
     */
    using HandlerFactory = std::function<std::unique_ptr<IMessageHandler>(IMessageTransport&)>;

    /**
     * @brief Constructs the FragmentationTransport layer.
     * @param indicateChar The BLE characteristic used for sending outgoing (indicated) data.
     * @param factory A factory function that creates the message handler.
     */
    FragmentationTransport(BLECharacteristic* indicateChar, HandlerFactory factory);

    /**
     * @brief Processes an incoming raw data chunk from the BLE stack.
     *
     * This method handles the reassembly logic. Once a full message is reassembled,
     * it is passed to the wrapped handler.
     * @param chunkData Pointer to the incoming data chunk.
     * @param len The length of the chunk.
     */
    void process(const uint8_t* chunkData, size_t len) override;

    /**
     * @brief Sends a full message by fragmenting it and sending it via indications.
     * @param fullMessageData Pointer to the complete message to be sent.
     * @param len The total length of the message.
     * @return True if the message sending was successfully initiated.
     */
    bool sendMessage(const uint8_t* fullMessageData, size_t len) override;

    /**
     * @brief Updates the transport layer with the new MTU size.
     *
     * This should be called when the BLE MTU changes to recalculate the maximum
     * chunk payload size.
     * @param newMtu The new MTU size for the connection.
     */
    void onMtuChanged(uint16_t newMtu);

private:
    /**
     * @brief The state of the incoming message reassembly process.
     */
    enum class ReassemblyState { IDLE, REASSEMBLING };

    /**
     * @brief Resets the reassembly state machine, clearing any buffered data.
     */
    void resetReassembly();

    /// @brief A tag used for logging from this class.
    static constexpr const char* TAG = "[FragTransport]";

    /// @brief The size of the GATT header (opcode + handle) for ATT operations.
    static constexpr uint16_t GATT_HEADER_SIZE = 3;

    /// @brief Timeout in milliseconds to discard a partial message reassembly.
    static constexpr uint32_t REASSEMBLY_TIMEOUT_MS = 5000;

    /// @brief The wrapped protocol handler that processes complete messages.
    std::unique_ptr<IMessageHandler> _wrappedHandler;

    /// @brief The BLE characteristic for sending outgoing data.
    BLECharacteristic* _indicateChar;

    /// @brief The current state of the reassembly process.
    ReassemblyState _reassemblyState = ReassemblyState::IDLE;

    /// @brief Buffer to store incoming fragments of a message.
    std::vector<uint8_t> _reassemblyBuffer;

    /// @brief The transaction ID of the message currently being reassembled.
    uint8_t _currentTransactionId = 0;

    /// @brief Timestamp of the last received fragment to detect timeouts.
    unsigned long _lastPacketTimestamp = 0;

    /// @brief The maximum payload size for an outgoing fragment, derived from the MTU.
    uint16_t _maxChunkPayloadSize = 20;  // Default: 23 (MTU) - 3 (GATT) - 1 (frag header) = 19

    /// @brief The transaction ID for the next outgoing fragmented message.
    uint8_t _outgoingTransactionId = 0;
};

#endif  // FRAGMENTATION_TRANSPORT_H