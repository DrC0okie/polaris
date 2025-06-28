package ch.drcookie.polaris_sdk.util

/**
 * Utility extension functions for byte array conversions.
 * Handles Little-Endian number conversions and Hex string representations.
 */
@OptIn(ExperimentalUnsignedTypes::class)
internal object ByteConversionUtils {

    /** Converts a [UByteArray] to a [ULong] assuming little-endian byte order. */
    internal fun UByteArray.toULongLE(): ULong {
        var value = 0uL
        for (i in indices.reversed()) { // Little-endian: LSB first
            value = (value shl 8) or this[i].toULong()
        }
        // Correct for typical little-endian where array index 0 is LSB
        var correctedValue = 0uL
        for (i in 0 until minOf(8, this.size)) {
            correctedValue = correctedValue or (this[i].toULong() shl (i * 8))
        }
        return correctedValue
    }

    /** Converts a [ULong] to a [UByteArray] of a given size using little-endian byte order. */
    internal fun ULong.toUByteArrayLE(size: Int = 8): UByteArray {
        val bytes = UByteArray(size)
        for (i in 0 until size) {
            bytes[i] = ((this shr (i * 8)) and 0xFFuL).toUByte()
        }
        return bytes
    }

    /** Converts a [UByteArray] to a [UInt] assuming little-endian byte order. */
    internal fun UByteArray.toUIntLE(): UInt {
        var value = 0u
        for (i in 0 until minOf(4, this.size)) {
            value = value or (this[i].toUInt() shl (i * 8))
        }
        return value
    }

    /** Converts a [UInt] to a [UByteArray] of a given size using little-endian byte order. */
    internal fun UInt.toUByteArrayLE(size: Int = 4): UByteArray {
        val bytes = UByteArray(size)
        for (i in 0 until size) {
            bytes[i] = ((this shr (i * 8)) and 0xFFu).toUByte()
        }
        return bytes
    }

    /** Converts a [UByteArray] to its hexadecimal string representation. */
    internal fun UByteArray.toHexString(): String {
        return joinToString("") { it.toString(16).padStart(2, '0') }
    }

    /** Converts a [ByteArray] to its hexadecimal string representation. */
    internal fun ByteArray.toHexString(): String = asUByteArray().toHexString()

    /**
     * Converts a hexadecimal string to a [UByteArray].
     * @throws IllegalStateException if the hex string has an odd number of characters.
     */
    internal fun hexStringToUByteArray(hex: String): UByteArray {
        check(hex.length % 2 == 0) { "Must have an even length" }
        return hex.chunked(2).map { it.toInt(16).toUByte() }.toUByteArray()
    }
}