// src/protocol/transport/fragmentation.h
#ifndef FRAGMENTATION_HEADER_H
#define FRAGMENTATION_HEADER_H

#include <stddef.h>
#include <stdint.h>

/**
 * @namespace fragmentation
 * @brief Defines the constants and structures for a simple application-layer fragmentation
 * protocol.
 *
 * This protocol prepends a 1-byte control header to each BLE packet to manage
 * the fragmentation and reassembly of larger logical messages.
 */
namespace fragmentation {

// Control Byte Flags (bits 7-6)
/// @brief Flag for a message that fits entirely within a single packet.
constexpr uint8_t FLAG_UNFRAGMENTED = 0b11000000;

/// @brief Flag for the first packet of a fragmented message.
constexpr uint8_t FLAG_START = 0b00000000;

/// @brief Flag for a middle packet of a fragmented message.
constexpr uint8_t FLAG_MIDDLE = 0b01000000;

/// @brief Flag for the final packet of a fragmented message.
constexpr uint8_t FLAG_END = 0b10000000;

/// @brief Bitmask to extract the packet type flags from the control byte.
constexpr uint8_t MASK_TYPE = 0b11000000;

/// @brief Bitmask to extract the transaction ID from the control byte.
constexpr uint8_t MASK_TRANSACTION_ID = 0b00111111;

/**
 * @struct Header
 * @brief The 1-byte control header for the fragmentation protocol.
 */
struct Header {
    /// @brief Contains the 2-bit packet type flag and a 6-bit transaction ID.
    uint8_t control;

    /// @brief The size of the protocol header in bytes.
    static constexpr size_t SIZE = sizeof(control);
};

}  // namespace fragmentation

#endif  // FRAGMENTATION_HEADER_H