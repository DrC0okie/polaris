package ch.drcookie.polaris_sdk.ble.util

import ch.drcookie.polaris_sdk.util.ByteConversionUtils.toUIntLE
import ch.drcookie.polaris_sdk.protocol.model.BroadcastPayload
import ch.drcookie.polaris_sdk.ble.model.CommonBleScanResult
import ch.drcookie.polaris_sdk.protocol.model.broadcastPayloadFromBytes

/** Used for parsing raw BLE advertisements data into SDK models. */
@OptIn(ExperimentalUnsignedTypes::class)
internal object BeaconDataParser {
    /**
     * Parses a connectable beacon's advertisement data.
     *
     * @param scanResult The raw scan result from the BLE stack.
     * @param legacyManufId The manufacturer ID to look for in the advertisement data.
     * @return A [Pair] containing the parsed beacon ID and status byte.
     */
    internal fun parseConnectableBeaconAd(scanResult: CommonBleScanResult, legacyManufId: Int): Pair<UInt?, Byte?> {
        val manufData = scanResult.manufacturerData[legacyManufId] ?: return null to null

        if (manufData.size < 4) return null to null

        val beaconId = manufData.sliceArray(0 until 4).toUByteArray().toUIntLE()

        // The status byte is optional
        val statusByte = if (manufData.size >= 5) manufData[4] else null

        return beaconId to statusByte
    }

    /**
     * Parses a full [BroadcastPayload] from an extended advertisement.
     *
     * @param scanResult The raw scan result from the BLE stack.
     * @param extendedManufId The manufacturer ID to look for in the advertisement data.
     * @return A parsed [BroadcastPayload] object, or `null` if the data is not present or malformed.
     */
    internal fun parseBroadcastPayload(scanResult: CommonBleScanResult, extendedManufId: Int): BroadcastPayload? {
        val manufData = scanResult.manufacturerData[extendedManufId]
        return if (manufData != null) {
            broadcastPayloadFromBytes(manufData.toUByteArray())
        } else {
            null
        }
    }
}