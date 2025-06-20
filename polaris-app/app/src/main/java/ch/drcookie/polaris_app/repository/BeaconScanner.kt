package ch.drcookie.polaris_app.repository

import ch.drcookie.polaris_app.data.ble.BleDataSource
import ch.drcookie.polaris_app.data.model.FoundBeacon
import ch.drcookie.polaris_app.data.model.dto.BeaconProvisioningDto
import ch.drcookie.polaris_app.util.PoLConstants
import ch.drcookie.polaris_app.util.Utils.toUIntLE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

class BeaconScanner(private val bleDataSource: BleDataSource) {

    /**
     * Scans for BLE devices and returns a continuous Flow of known beacons as they are found.
     * The collector of this flow is responsible for stopping collection (e.g., after a timeout or after finding a specific beacon).
     * The flow will automatically stop the BLE scan when collection ceases.
     *
     * @param knownBeacons The list of beacons provisioned for this user.
     * @return A Flow emitting a [FoundBeacon] for each known beacon discovered.
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    fun findKnownBeacons(knownBeacons: List<BeaconProvisioningDto>): Flow<FoundBeacon> {
        return bleDataSource.scanForBeacons().mapNotNull { scanResult ->
            // Extract the beacon ID from the advertisement's manufacturer data
            val manufData = scanResult.scanRecord?.getManufacturerSpecificData(PoLConstants.MANUFACTURER_ID)
            if (manufData != null && manufData.size >= 4) {
                val detectedId = manufData.toUByteArray().toUIntLE()
                // Check if this detected ID matches any of our known beacons
                val matchedBeaconInfo = knownBeacons.find { it.beaconId == detectedId }
                if (matchedBeaconInfo != null) {
                    // If it's a match, emit a FoundBeacon object
                    return@mapNotNull FoundBeacon(matchedBeaconInfo, scanResult)
                }
            }
            null // Not a known beacon, ignore.
        }
    }
}