package ch.drcookie.polaris_sdk.api.flows

import ch.drcookie.polaris_sdk.api.SdkError
import ch.drcookie.polaris_sdk.api.SdkResult
import ch.drcookie.polaris_sdk.ble.model.ConnectionState
import ch.drcookie.polaris_sdk.network.ApiClient
import ch.drcookie.polaris_sdk.ble.BleController
import ch.drcookie.polaris_sdk.ble.model.EncryptedPayload
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Handles the entire flow of delivering a single secure payload to a beacon.
 *
 * @return The raw ACK/ERR blob received from the beacon, or null if the flow failed
 *         before receiving the blob (e.g., beacon not found).
 */
public class DeliverPayloadFlow(
    private val bleController: BleController,
    private val apiClient: ApiClient,
    private val scanForBeacon: ScanForBeaconFlow
) {
    @OptIn(ExperimentalUnsignedTypes::class)
    public suspend operator fun invoke(payload: EncryptedPayload): SdkResult<ByteArray, SdkError> {
        // Find the specific beacon required for this job
        val targetBeaconInfo = apiClient.knownBeacons.find { it.id == payload.beaconId }
            ?: return SdkResult.Failure(
                SdkError.PreconditionError("Beacon #${payload.beaconId} is not in the known list.")
            )

        // Scan for the beacon
        val scanResult = scanForBeacon(beaconsToFind = listOf(targetBeaconInfo))
        val foundBeacon = when (scanResult) {
            is SdkResult.Success -> scanResult.value
            is SdkResult.Failure -> return scanResult
        }

        if (foundBeacon == null) {
            return SdkResult.Failure(SdkError.BleError("Beacon #${payload.beaconId} not found within timeout."))
        }

        // Connect to the beacon
        try {
            when (val connectResult = bleController.connect(foundBeacon.address)) {
                is SdkResult.Failure -> return connectResult
                is SdkResult.Success -> { /* Continue */ }
            }

            // Await a "Ready" or "Failed" state, with a timeout.
            val status = withTimeoutOrNull(10000L) { // 15-second timeout
                bleController.connectionState
                    .filter { it is ConnectionState.Ready || it is ConnectionState.Failed }
                    .first()
            }

            // Handle the outcome of the connection attempt.
            when (status) {
                is ConnectionState.Ready -> { /* Connection successful, continue*/ }
                is ConnectionState.Failed -> return SdkResult.Failure(SdkError.BleError("Connection failed: ${status.error}"))
                null -> return SdkResult.Failure(SdkError.BleError("Connection timed out."))
                else -> return SdkResult.Failure(SdkError.BleError("Unexpected connection state: $status"))
            }

            return bleController.deliverSecurePayload(payload.blob.asByteArray())

        } finally {
            bleController.disconnect()
        }
    }
}