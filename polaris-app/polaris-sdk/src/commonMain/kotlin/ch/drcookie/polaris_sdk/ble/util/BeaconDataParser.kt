package ch.drcookie.polaris_sdk.ble.util

import ch.drcookie.polaris_sdk.util.ByteConversionUtils.toUIntLE
import ch.drcookie.polaris_sdk.protocol.model.BroadcastPayload
import ch.drcookie.polaris_sdk.ble.model.CommonBleScanResult
import ch.drcookie.polaris_sdk.protocol.model.broadcastPayloadFromBytes

@OptIn(ExperimentalUnsignedTypes::class)
internal object BeaconDataParser {
    /**
     * Attempts to parse the beacon ID from a legacy advertisement (connectable beacon).
     *
     * @param scanResult The raw scan result from the BLE stack.
     * @return The parsed beacon ID as a [UInt], and the flag signalling that a payload is available
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
     * Attempts to parse a full [BroadcastPayload] from an extended advertisement.
     *
     * @param scanResult The raw scan result from the BLE stack.
     * @return A parsed [BroadcastPayload] object, or null if the data is not present or malformed.
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