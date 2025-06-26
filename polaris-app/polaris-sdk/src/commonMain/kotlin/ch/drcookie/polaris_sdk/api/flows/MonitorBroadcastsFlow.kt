package ch.drcookie.polaris_sdk.api.flows

import ch.drcookie.polaris_sdk.api.SdkError
import ch.drcookie.polaris_sdk.api.SdkResult
import ch.drcookie.polaris_sdk.ble.model.ScanCallbackType
import ch.drcookie.polaris_sdk.ble.model.ScanConfig
import ch.drcookie.polaris_sdk.ble.model.ScanMode
import ch.drcookie.polaris_sdk.model.VerifiedBroadcast
import ch.drcookie.polaris_sdk.network.ApiClient
import ch.drcookie.polaris_sdk.ble.BleController
import ch.drcookie.polaris_sdk.protocol.ProtocolHandler
import ch.drcookie.polaris_sdk.protocol.model.BroadcastPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

public class MonitorBroadcastsFlow(
    private val bleController: BleController,
    private val apiClient: ApiClient,
    private val protocolRepo: ProtocolHandler,
) {
    @OptIn(ExperimentalUnsignedTypes::class)
    public fun startMonitoring(): Flow<SdkResult<VerifiedBroadcast, SdkError>> {
        if (apiClient.knownBeacons.isEmpty()) {
            // Return an empty flow if we have no keys to verify with.
            return flowOf(SdkResult.Failure(SdkError.PreconditionError("Cannot monitor broadcasts: No known beacons have been registered.")))
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
            .map<BroadcastPayload, SdkResult<VerifiedBroadcast, SdkError>> { payload ->
                val beaconPublicKey = apiClient.knownBeacons.find { it.id == payload.beaconId }?.publicKey

                val isVerified = if (beaconPublicKey != null) {
                    protocolRepo.verifyBroadcast(payload, beaconPublicKey)
                } else {
                    false // We don't have a key for this beacon
                }

                SdkResult.Success(VerifiedBroadcast(payload, isVerified))
            }
            .catch { e ->
                // If the underlying BLE scan itself throws an exception, catch it...
                emit(SdkResult.Failure(SdkError.BleError("Broadcast monitoring failed: ${e.message}")))
            }
    }
}