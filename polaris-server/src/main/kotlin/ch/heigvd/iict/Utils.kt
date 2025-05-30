package ch.heigvd.iict

// Helper extension functions for UByteArray <-> Number conversions (Little Endian)
object Utils {
    @OptIn(ExperimentalUnsignedTypes::class)
    fun ULong.toUByteArrayLE(size: Int = 8): UByteArray {
        val bytes = UByteArray(size)
        for (i in 0 until size) {
            bytes[i] = ((this shr (i * 8)) and 0xFFuL).toUByte()
        }
        return bytes
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun UInt.toUByteArrayLE(size: Int = 4): UByteArray {
        val bytes = UByteArray(size)
        for (i in 0 until size) {
            bytes[i] = ((this shr (i * 8)) and 0xFFu).toUByte()
        }
        return bytes
    }
}