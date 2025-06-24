package ch.drcookie.polaris_app.domain.interactor

import ch.drcookie.polaris_app.domain.model.ScanCallbackType
import ch.drcookie.polaris_app.domain.model.ScanConfig
import ch.drcookie.polaris_app.domain.model.ScanMode
import ch.drcookie.polaris_app.domain.model.VerifiedBroadcast
import ch.drcookie.polaris_app.domain.repository.AuthRepository
import ch.drcookie.polaris_app.domain.repository.BleDataSource
import ch.drcookie.polaris_app.domain.repository.ProtocolRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class MonitorBroadcastsInteractor(
    private val bleDataSource: BleDataSource,
    private val authRepository: AuthRepository,
    private val protocolRepo: ProtocolRepository
) {
    @OptIn(ExperimentalUnsignedTypes::class)
    fun startMonitoring(): kotlinx.coroutines.flow.Flow<VerifiedBroadcast> {
        if (authRepository.knownBeacons.isEmpty()) {
            // Return an empty flow if we have no keys to verify with.
            return kotlinx.coroutines.flow.emptyFlow()
        }

        // Configure the scan for continuous, low-latency monitoring
        val scanConfig = ScanConfig(
            scanMode = ScanMode.LOW_LATENCY,
            callbackType = ScanCallbackType.ALL_MATCHES,
            scanLegacyOnly = false, // Must be false for extended advertisements
            useAllSupportedPhys = true
        )

        // Returns a Flow that the ViewModel will collect.
        return bleDataSource.monitorBroadcasts(scanConfig)
            .distinctUntilChanged()
            .map { payload ->
                val beaconPublicKey = authRepository.knownBeacons
                    .find { it.beaconId == payload.beaconId }?.publicKey

                val isVerified = if (beaconPublicKey != null) {
                    protocolRepo.verifyBroadcast(payload, beaconPublicKey)
                } else {
                    false // We don't have a key for this beacon
                }

                VerifiedBroadcast(payload, isVerified)
            }
    }
}