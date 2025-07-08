package ch.heigvd.iict.util

/**
 * A utility object providing extension functions for common byte and string manipulations
 * required by the Polaris protocol.
 */
@OptIn(ExperimentalUnsignedTypes::class)
object PoLUtils {
    /** Converts a [UByteArray] to a [ULong] assuming little-endian byte order. */
    fun UByteArray.toULongLE(): ULong {
        var value: ULong = 0uL
        for (i in indices.reversed()) { // Little-endian: LSB first
            value = (value shl 8) or this[i].toULong()
        }
        var correctedValue: ULong = 0uL
        for (i in 0 until minOf(8, this.size)) {
            correctedValue = correctedValue or (this[i].toULong() shl (i * 8))
        }
        return correctedValue
    }

    /** Converts a [ULong] to a little-endian [UByteArray] of a specified size. */
    fun ULong.toUByteArrayLE(size: Int = 8): UByteArray {
        val bytes = UByteArray(size)
        for (i in 0 until size) {
            bytes[i] = ((this shr (i * 8)) and 0xFFuL).toUByte()
        }
        return bytes
    }

    /** Converts a [UByteArray] to a [UInt] assuming little-endian byte order. */
    fun UByteArray.toUIntLE(): UInt {
        var value: UInt = 0u
        for (i in 0 until minOf(4, this.size)) {
            value = value or (this[i].toUInt() shl (i * 8))
        }
        return value
    }

    /** Converts a [UInt] to a little-endian [UByteArray] of a specified size. */
    fun UInt.toUByteArrayLE(size: Int = 4): UByteArray {
        val bytes = UByteArray(size)
        for (i in 0 until size) {
            bytes[i] = ((this shr (i * 8)) and 0xFFu).toUByte()
        }
        return bytes
    }

    /** Converts a [UShort] to a little-endian [UByteArray] of a specified size. */
    fun UShort.toUByteArrayLE(size: Int = 2): UByteArray {
        val bytes = UByteArray(size)
        for (i in 0 until size) {
            bytes[i] = ((this.toUInt() shr (i * 8)) and 0xFFu).toUByte()
        }
        return bytes
    }

    /** Converts a [UByteArray] to its hexadecimal string representation (e.g., "0A1B2C"). */
    fun UByteArray.toHexString(): String = joinToString("") { it.toString(16).padStart(2, '0') }

    /** Converts a [ByteArray] to its hexadecimal string representation. */
    fun ByteArray.toHexString(): String = asUByteArray().toHexString()

    /**
     * Converts a hexadecimal string into a [UByteArray].
     * @throws IllegalStateException if the hex string has an odd number of characters.
     */
    fun hexStringToUByteArray(hex: String): UByteArray {
        check(hex.length % 2 == 0) { "Must have an even length" }
        return hex.chunked(2).map { it.toInt(16).toUByte() }.toUByteArray()
    }
}