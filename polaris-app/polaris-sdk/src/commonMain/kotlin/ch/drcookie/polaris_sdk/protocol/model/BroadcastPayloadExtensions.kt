package ch.drcookie.polaris_sdk.protocol.model

import ch.drcookie.polaris_sdk.util.ByteConversionUtils.toUByteArrayLE
import ch.drcookie.polaris_sdk.util.ByteConversionUtils.toUIntLE
import ch.drcookie.polaris_sdk.util.ByteConversionUtils.toULongLE

/**
 * Attempts to parse a [BroadcastPayload] from a raw byte array.
 *
 * @param data The raw byte array from the advertisement.
 * @return A parsed [BroadcastPayload] object, or `null` if the data is malformed or has an incorrect length.
 */
@OptIn(ExperimentalUnsignedTypes::class)
public fun broadcastPayloadFromBytes(data: UByteArray): BroadcastPayload? {
    // Ensure the data is at least the expected size
    if (data.size < BroadcastPayload.PACKED_SIZE) {
        return null
    }
    var offset = 0

    // Parse beaconId (4 bytes, Little Endian)
    val beaconId = data.sliceArray(offset until offset + Constants.BEACON_ID).toUIntLE()
    offset += Constants.BEACON_ID

    // Parse counter (8 bytes, Little Endian)
    val counter = data.sliceArray(offset until offset + Constants.BEACON_COUNTER).toULongLE()
    offset += Constants.BEACON_COUNTER

    // Parse signature (64 bytes)
    val signature = data.sliceArray(offset until offset + Constants.SIG)

    return BroadcastPayload(beaconId, counter, signature)
}

/**
 * Reconstructs the portion of the data that was originally signed by the beacon.
 *
 * @return A [UByteArray] containing the signed data (beaconId + counter).
 */
@OptIn(ExperimentalUnsignedTypes::class)
public fun BroadcastPayload.getSignedData(): UByteArray {
    // Concatenate the little-endian byte representations of beaconId and counter
    return beaconId.toUByteArrayLE() + counter.toUByteArrayLE()
}