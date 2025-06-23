package ch.drcookie.polaris_app.domain.interactor

import ch.drcookie.polaris_app.domain.interactor.logic.BeaconDataParser
import ch.drcookie.polaris_app.domain.model.FoundBeacon
import ch.drcookie.polaris_app.domain.model.ScanConfig
import ch.drcookie.polaris_app.domain.model.dto.BeaconProvisioningDto
import ch.drcookie.polaris_app.domain.repository.AuthRepository
import ch.drcookie.polaris_app.domain.repository.BleDataSource
import ch.drcookie.polaris_app.domain.repository.startConnectableScan
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withTimeoutOrNull

class ScanConnectableBeaconInteractor(
    private val bleDataSource: BleDataSource,
    private val authRepository: AuthRepository,
    private val beaconDataParser: BeaconDataParser
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

        return withTimeoutOrNull(timeoutMillis) {
            bleDataSource.startConnectableScan(ScanConfig())
                .mapNotNull { commonScanResult ->
                    val beaconId = beaconDataParser.parseConnectableBeaconId(commonScanResult)
                    if (beaconId != null) {
                        val matchedInfo = beaconsToFind.find { it.beaconId == beaconId }
                        if (matchedInfo != null) {
                            return@mapNotNull FoundBeacon(
                                provisioningInfo = matchedInfo,
                                address = commonScanResult.deviceAddress
                            )
                        }
                    }
                    null
                }
                .firstOrNull()
        }
    }
}