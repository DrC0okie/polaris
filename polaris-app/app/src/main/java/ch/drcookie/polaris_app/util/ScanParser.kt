package ch.drcookie.polaris_app.util


import android.bluetooth.le.ScanResult
import ch.drcookie.polaris_app.data.model.BroadcastPayload
import ch.drcookie.polaris_app.util.Utils.toUIntLE

@OptIn(ExperimentalUnsignedTypes::class)
object ScanParser {
    /**
     * Attempts to parse the beacon ID from a legacy advertisement (connectable beacon).
     *
     * @param scanResult The raw scan result from the BLE stack.
     * @return The parsed beacon ID as a [UInt], or null if the data is not present or malformed.
     */
    fun parseConnectableBeaconId(scanResult: ScanResult): UInt? {
        val manufData = scanResult.scanRecord?.getManufacturerSpecificData(PoLConstants.LEGACY_MANUFACTURER_ID)
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
    fun parseBroadcastPayload(scanResult: ScanResult): BroadcastPayload? {
        val manufData = scanResult.scanRecord?.getManufacturerSpecificData(PoLConstants.EXTENDED_MANUFACTURER_ID)
        return if (manufData != null) {
            BroadcastPayload.fromBytes(manufData.toUByteArray())
        } else {
            null
        }
    }
}