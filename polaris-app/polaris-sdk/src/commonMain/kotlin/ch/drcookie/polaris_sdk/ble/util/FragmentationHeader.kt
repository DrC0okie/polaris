package ch.drcookie.polaris_sdk.ble.util

@OptIn(ExperimentalUnsignedTypes::class)
internal object FragmentationHeader {
    // Header flags
    internal const val FLAG_UNFRAGMENTED: UByte = 0b11000000u
    internal const val FLAG_START: UByte = 0b00000000u
    internal const val FLAG_MIDDLE: UByte = 0b01000000u
    internal const val FLAG_END: UByte = 0b10000000u

    // Masks
    internal const val MASK_TYPE: UByte = 0b11000000u
    internal const val MASK_TRANSACTION_ID: UByte = 0b00111111u

    internal const val HEADER_SIZE = 1
}