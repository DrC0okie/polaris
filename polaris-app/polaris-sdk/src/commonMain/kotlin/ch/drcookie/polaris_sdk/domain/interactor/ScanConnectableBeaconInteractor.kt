package ch.drcookie.polaris_sdk.domain.interactor

import ch.drcookie.polaris_sdk.domain.model.FoundBeacon
import ch.drcookie.polaris_sdk.domain.model.ScanConfig
import ch.drcookie.polaris_sdk.domain.model.dto.BeaconProvisioningDto
import ch.drcookie.polaris_sdk.domain.repository.AuthRepository
import ch.drcookie.polaris_sdk.domain.repository.BleDataSource
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

class ScanConnectableBeaconInteractor(
    private val bleDataSource: BleDataSource,
    private val authRepository: AuthRepository
) {

    /**
     * Scans for a limited time to find the first available beacon from a target list.
     * @param timeoutMillis The duration to scan before giving up.
     * @param beaconsToFind The list of beacons we are interested in. Defaults to all known beacons.
     * @return The [FoundBeacon] if one is found within the timeout, otherwise null.
     */
    suspend operator fun invoke(
        timeoutMillis: Long = 10000L,
        beaconsToFind: List<BeaconProvisioningDto> = authRepository.knownBeacons
    ): FoundBeacon? {

        if (beaconsToFind.isEmpty()) {
            return null
        }

        val scanConfig = ScanConfig()

        return withTimeoutOrNull(timeoutMillis) {
            bleDataSource.findConnectableBeacons(scanConfig, beaconsToFind).firstOrNull()
        }
    }
}