package ch.drcookie.polaris_sdk.ble.util

import ch.drcookie.polaris_sdk.util.ByteConversionUtils.toUIntLE
import ch.drcookie.polaris_sdk.protocol.model.BroadcastPayload
import ch.drcookie.polaris_sdk.ble.model.CommonBleScanResult

@OptIn(ExperimentalUnsignedTypes::class)
internal object BeaconDataParser {
    /**
     * Attempts to parse the beacon ID from a legacy advertisement (connectable beacon).
     *
     * @param scanResult The raw scan result from the BLE stack.
     * @return The parsed beacon ID as a [UInt], or null if the data is not present or malformed.
     */
    internal fun parseConnectableBeaconId(scanResult: CommonBleScanResult, legacyManufId: Int): UInt? {
        val manufData = scanResult.manufacturerData[legacyManufId]
        if (manufData != null && manufData.size >= 4) {
            return manufData.toUByteArray().toUIntLE()
        }
        return null
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
            BroadcastPayload.Companion.fromBytes(manufData.toUByteArray())
        } else {
            null
        }
    }
}