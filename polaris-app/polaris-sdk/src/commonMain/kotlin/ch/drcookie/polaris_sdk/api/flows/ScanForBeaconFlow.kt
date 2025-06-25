package ch.drcookie.polaris_sdk.api.flows

import ch.drcookie.polaris_sdk.ble.model.FoundBeacon
import ch.drcookie.polaris_sdk.ble.model.ScanConfig
import ch.drcookie.polaris_sdk.network.dto.BeaconProvisioningDto
import ch.drcookie.polaris_sdk.network.ApiClient
import ch.drcookie.polaris_sdk.ble.BleController
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

class ScanForBeaconFlow(
    private val bleController: BleController,
    private val apiClient: ApiClient
) {

    /**
     * Scans for a limited time to find the first available beacon from a target list.
     * @param timeoutMillis The duration to scan before giving up.
     * @param beaconsToFind The list of beacons we are interested in. Defaults to all known beacons.
     * @return The [FoundBeacon] if one is found within the timeout, otherwise null.
     */
    suspend operator fun invoke(
        timeoutMillis: Long = 10000L,
        beaconsToFind: List<BeaconProvisioningDto> = apiClient.knownBeacons
    ): FoundBeacon? {

        if (beaconsToFind.isEmpty()) {
            return null
        }

        val scanConfig = ScanConfig()

        return withTimeoutOrNull(timeoutMillis) {
            bleController.findConnectableBeacons(scanConfig, beaconsToFind).firstOrNull()
        }
    }
}