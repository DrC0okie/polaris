// src/protocol/transport/fragmentation.h
#ifndef FRAGMENTATION_HEADER_H
#define FRAGMENTATION_HEADER_H

#include <stddef.h>
#include <stdint.h>

namespace fragmentation {

// Using the 2 most significant bits for flags
constexpr uint8_t FLAG_UNFRAGMENTED = 0b11000000;
constexpr uint8_t FLAG_START = 0b00000000;
constexpr uint8_t FLAG_MIDDLE = 0b01000000;
constexpr uint8_t FLAG_END = 0b10000000;

// Masks to extract parts of the header
constexpr uint8_t MASK_TYPE = 0b11000000;
constexpr uint8_t MASK_TRANSACTION_ID = 0b00111111;

struct Header {
    uint8_t control;  // Contains flags and transaction ID
    static constexpr size_t SIZE = sizeof(control);
};

}  // namespace fragmentation

#endif  // FRAGMENTATION_HEADER_H