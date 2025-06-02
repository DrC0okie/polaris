package ch.heigvd.iict.util

@OptIn(ExperimentalUnsignedTypes::class)
object PoLUtils {
    // Helper extension functions for UByteArray <-> Number conversions (Little Endian)
    fun UByteArray.toULongLE(): ULong {
        var value: ULong = 0uL
        for (i in indices.reversed()) { // Little-endian: LSB first
            value = (value shl 8) or this[i].toULong()
        }
        // Correct for typical little-endian where array index 0 is LSB
        var correctedValue: ULong = 0uL
        for (i in 0 until minOf(8, this.size)) {
            correctedValue = correctedValue or (this[i].toULong() shl (i * 8))
        }
        return correctedValue
    }

    fun ULong.toUByteArrayLE(size: Int = 8): UByteArray {
        val bytes = UByteArray(size)
        for (i in 0 until size) {
            bytes[i] = ((this shr (i * 8)) and 0xFFuL).toUByte()
        }
        return bytes
    }

    fun UByteArray.toUIntLE(): UInt {
        var value: UInt = 0u
        for (i in 0 until minOf(4, this.size)) {
            value = value or (this[i].toUInt() shl (i * 8))
        }
        return value
    }

    fun UInt.toUByteArrayLE(size: Int = 4): UByteArray {
        val bytes = UByteArray(size)
        for (i in 0 until size) {
            bytes[i] = ((this shr (i * 8)) and 0xFFu).toUByte()
        }
        return bytes
    }

    fun UByteArray.toHexString(): String = joinToString("") { it.toString(16).padStart(2, '0') }

    fun ByteArray.toHexString(): String = asUByteArray().toHexString()

    fun hexStringToUByteArray(hex: String): UByteArray {
        check(hex.length % 2 == 0) { "Must have an even length" }
        return hex.chunked(2).map { it.toInt(16).toUByte() }.toUByteArray()
    }
}