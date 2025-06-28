package ch.drcookie.polaris_sdk.api.flows

import ch.drcookie.polaris_sdk.api.SdkError
import ch.drcookie.polaris_sdk.api.SdkResult
import ch.drcookie.polaris_sdk.ble.model.ScanCallbackType
import ch.drcookie.polaris_sdk.ble.model.ScanConfig
import ch.drcookie.polaris_sdk.ble.model.ScanMode
import ch.drcookie.polaris_sdk.model.VerifiedBroadcast
import ch.drcookie.polaris_sdk.network.NetworkClient
import ch.drcookie.polaris_sdk.ble.BleController
import ch.drcookie.polaris_sdk.protocol.ProtocolHandler
import ch.drcookie.polaris_sdk.protocol.model.BroadcastPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * A high-level use case for monitoring non-connectable BLE broadcast advertisements.
 *
 * This flow initiates a continuous scan and, for each advertisement, verifies its signature.
 *
 * @property bleController For the underlying BLE scan.
 * @property networkClient To access the list of `knownBeacons` for public keys.
 * @property protocolHandler For the signature verification.
 */
public class MonitorBroadcastsFlow(
    private val bleController: BleController,
    private val networkClient: NetworkClient,
    private val protocolHandler: ProtocolHandler,
) {
    /**
     * Starts the monitoring process and returns a [Flow] of results.
     *
     * The returned Flow will stay active, emitting values for each broadcast received, until the
     * collecting coroutine is cancelled.
     *
     * @return A [Flow] that emits [SdkResult]s, each containing either a [VerifiedBroadcast] or an [SdkError].
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    public fun startMonitoring(): Flow<SdkResult<VerifiedBroadcast, SdkError>> {
        if (networkClient.knownBeacons.isEmpty()) {
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

        val broadcastResult = bleController.monitorBroadcasts(scanConfig)

        // Check the result. If it's a failure, propagate it.
        val beaconFlow = when (broadcastResult) {
            is SdkResult.Success -> broadcastResult.value
            is SdkResult.Failure -> return flowOf(broadcastResult)
        }
        return beaconFlow.distinctUntilChanged()
            .map<BroadcastPayload, SdkResult<VerifiedBroadcast, SdkError>> { payload ->
                val beaconPublicKey = networkClient.knownBeacons.find { it.id == payload.beaconId }?.publicKey

                val isVerified = if (beaconPublicKey != null) {
                    protocolHandler.verifyBroadcast(payload, beaconPublicKey)
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