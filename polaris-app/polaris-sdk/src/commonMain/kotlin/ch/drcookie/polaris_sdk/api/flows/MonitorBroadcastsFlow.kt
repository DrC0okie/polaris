package ch.drcookie.polaris_sdk.api.flows

import ch.drcookie.polaris_sdk.ble.model.ScanCallbackType
import ch.drcookie.polaris_sdk.ble.model.ScanConfig
import ch.drcookie.polaris_sdk.ble.model.ScanMode
import ch.drcookie.polaris_sdk.model.VerifiedBroadcast
import ch.drcookie.polaris_sdk.network.ApiClient
import ch.drcookie.polaris_sdk.ble.BleController
import ch.drcookie.polaris_sdk.protocol.ProtocolHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

class MonitorBroadcastsFlow(
    private val bleController: BleController,
    private val apiClient: ApiClient,
    private val protocolRepo: ProtocolHandler
) {
    @OptIn(ExperimentalUnsignedTypes::class)
    fun startMonitoring(): Flow<VerifiedBroadcast> {
        if (apiClient.knownBeacons.isEmpty()) {
            // Return an empty flow if we have no keys to verify with.
            return emptyFlow()
        }

        // Configure the scan for continuous, low-latency monitoring
        val scanConfig = ScanConfig(
            scanMode = ScanMode.LOW_LATENCY,
            callbackType = ScanCallbackType.ALL_MATCHES,
            scanLegacyOnly = false, // Must be false for extended advertisements
            useAllSupportedPhys = true
        )

        // Returns a Flow that the ViewModel will collect.
        return bleController.monitorBroadcasts(scanConfig)
            .distinctUntilChanged()
            .map { payload ->
                val beaconPublicKey = apiClient.knownBeacons
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