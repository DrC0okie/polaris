package ch.drcookie.polaris_sdk.data.datasource.ble

@OptIn(ExperimentalUnsignedTypes::class)
object FragmentationHeader {
    // Header flags
    const val FLAG_UNFRAGMENTED: UByte = 0b11000000u
    const val FLAG_START: UByte = 0b00000000u
    const val FLAG_MIDDLE: UByte = 0b01000000u
    const val FLAG_END: UByte = 0b10000000u

    // Masks
    const val MASK_TYPE: UByte = 0b11000000u
    const val MASK_TRANSACTION_ID: UByte = 0b00111111u

    const val HEADER_SIZE = 1
}