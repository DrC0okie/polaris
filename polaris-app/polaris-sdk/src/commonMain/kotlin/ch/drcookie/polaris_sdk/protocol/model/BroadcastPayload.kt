package ch.drcookie.polaris_sdk.protocol.model

import ch.drcookie.polaris_sdk.util.ByteConversionUtils.toUByteArrayLE
import ch.drcookie.polaris_sdk.util.ByteConversionUtils.toUIntLE
import ch.drcookie.polaris_sdk.util.ByteConversionUtils.toULongLE
import ch.drcookie.polaris_sdk.util.Constants

/**
 * Represents the data structure of a non-connectable extended advertisement broadcast.
 */
@OptIn(ExperimentalUnsignedTypes::class)
data class BroadcastPayload(
    val beaconId: UInt,
    val counter: ULong,
    val signature: UByteArray
) {
    init {
        // Enforce size constraint for the signature
        require(signature.size == Constants.SIG_SIZE) {
            "Invalid signature size. Expected ${Constants.SIG_SIZE}, got ${signature.size}"
        }
    }

    companion object {
        // Total size of the payload: 4 (id) + 8 (counter) + 64 (sig) = 76 bytes
        const val PACKED_SIZE = 4 + 8 + Constants.SIG_SIZE

        /**
         * Attempts to parse a [BroadcastPayload] from a raw UByteArray.
         * This is used to decode the manufacturer-specific data from a BLE scan result.
         *
         * @param data The raw byte array from the advertisement.
         * @return A parsed [BroadcastPayload] object, or null if the data is malformed or too short.
         */
        fun fromBytes(data: UByteArray): BroadcastPayload? {
            // Ensure the data is at least the expected size
            if (data.size < PACKED_SIZE) {
                return null
            }

            var offset = 0

            // Parse beaconId (4 bytes, Little Endian)
            val beaconId = data.sliceArray(offset until offset + 4).toUIntLE()
            offset += 4

            // Parse counter (8 bytes, Little Endian)
            val counter = data.sliceArray(offset until offset + 8).toULongLE()
            offset += 8

            // Parse signature (64 bytes)
            val signature = data.sliceArray(offset until offset + Constants.SIG_SIZE)

            return BroadcastPayload(beaconId, counter, signature)
        }
    }

    /**
     * Returns the portion of the data that was signed by the beacon.
     * On the beacon, the signature is calculated over the concatenation of `beaconId` and `counter`.
     * We must reconstruct this exact byte sequence to verify the signature.
     *
     * @return A [UByteArray] containing the signed data (beaconId + counter).
     */
    fun getSignedData(): UByteArray {
        // Concatenate the little-endian byte representations of beaconId and counter
        return beaconId.toUByteArrayLE() + counter.toUByteArrayLE()
    }

    override fun hashCode(): Int {
        var result = beaconId.hashCode()
        result = 31 * result + counter.hashCode()
        result = 31 * result + signature.contentHashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BroadcastPayload

        if (beaconId != other.beaconId) return false
        if (counter != other.counter) return false
        if (!signature.contentEquals(other.signature)) return false

        return true
    }
}