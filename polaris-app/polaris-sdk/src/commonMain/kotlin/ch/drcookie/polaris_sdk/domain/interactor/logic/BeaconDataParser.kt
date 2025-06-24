package ch.drcookie.polaris_sdk.domain.interactor.logic

import ch.drcookie.polaris_sdk.domain.model.BroadcastPayload
import ch.drcookie.polaris_sdk.domain.model.Constants
import ch.drcookie.polaris_sdk.core.ByteConversionUtils.toUIntLE
import ch.drcookie.polaris_sdk.domain.model.CommonBleScanResult

@OptIn(ExperimentalUnsignedTypes::class)
object BeaconDataParser {
    /**
     * Attempts to parse the beacon ID from a legacy advertisement (connectable beacon).
     *
     * @param scanResult The raw scan result from the BLE stack.
     * @return The parsed beacon ID as a [UInt], or null if the data is not present or malformed.
     */
    fun parseConnectableBeaconId(scanResult: CommonBleScanResult): UInt? {
        val manufData = scanResult.manufacturerData[Constants.LEGACY_MANUFACTURER_ID]
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
    fun parseBroadcastPayload(scanResult: CommonBleScanResult): BroadcastPayload? {
        val manufData = scanResult.manufacturerData[Constants.EXTENDED_MANUFACTURER_ID]
        return if (manufData != null) {
            BroadcastPayload.fromBytes(manufData.toUByteArray())
        } else {
            null
        }
    }
}