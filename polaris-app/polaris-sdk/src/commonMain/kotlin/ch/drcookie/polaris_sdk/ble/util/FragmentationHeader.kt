package ch.drcookie.polaris_sdk.ble.util

/**
 * Defines the constants and bitmasks for the 1-byte fragmentation header.
 *
 * The header is structured as follows:
 * - Bits 7-6: Packet type (Start, Middle, End, Unfragmented)
 * - Bits 5-0: Transaction ID, a rolling counter to associate chunks of the same message.
 */
@OptIn(ExperimentalUnsignedTypes::class)
internal object FragmentationHeader {
    // Packet Type Flags (Bits 7-6)
    internal const val FLAG_UNFRAGMENTED: UByte = 0b11000000u
    internal const val FLAG_START: UByte = 0b00000000u
    internal const val FLAG_MIDDLE: UByte = 0b01000000u
    internal const val FLAG_END: UByte = 0b10000000u

    // Bitmasks
    internal const val MASK_TYPE: UByte = 0b11000000u
    internal const val MASK_TRANSACTION_ID: UByte = 0b00111111u

    internal const val HEADER_SIZE = 1
}