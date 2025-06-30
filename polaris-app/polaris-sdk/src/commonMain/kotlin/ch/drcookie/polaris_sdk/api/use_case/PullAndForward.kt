package ch.drcookie.polaris_sdk.api.use_case

import ch.drcookie.polaris_sdk.api.SdkError
import ch.drcookie.polaris_sdk.api.SdkResult
import ch.drcookie.polaris_sdk.ble.BleController
import ch.drcookie.polaris_sdk.ble.model.ConnectionState
import ch.drcookie.polaris_sdk.ble.model.FoundBeacon
import ch.drcookie.polaris_sdk.network.NetworkClient
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * A high-level use case that handles the full beacon-to-server data use case.
 *
 * Performs the following sequence:
 * 1. Connects to a beacon that has its "data pending" flag set.
 * 2. Pulls the encrypted data from the beacon.
 * 3. Forwards that data to the server.
 * 4. Receives an encrypted ACK from the server.
 * 5. Relays the server's ACK back to the beacon to complete the transaction.
 * 6. Disconnects from the beacon.
 *
 * @property bleController The controller for all BLE operations.
 * @property networkClient The client for forwarding the data to the server.
 */
public class PullAndForward(
    private val bleController: BleController,
    private val networkClient: NetworkClient
) {
    /**
     * Executes the full data pull and forward use case.
     * @param foundBeacon The specific beacon to interact with. It should have `hasDataPending` set to true.
     * @return A `Success(Unit)` on a complete successful transaction, or a `Failure` at any step.
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    public suspend operator fun invoke(foundBeacon: FoundBeacon): SdkResult<Unit, SdkError> {
        // Precondition check
        if (!foundBeacon.hasDataPending) {
            return SdkResult.Failure(SdkError.PreconditionError("Beacon does not have the 'data pending' flag set."))
        }

        try {
            // Connect to the beacon (includes waiting for Ready state)
            val connectResult = connectToReadyState(foundBeacon)
            if (connectResult is SdkResult.Failure) return connectResult

            // Pull data from the beacon
            val beaconData = when (val pullResult = bleController.pullEncryptedData()) {
                is SdkResult.Success -> pullResult.value
                is SdkResult.Failure -> return pullResult
            }

            // Forward data to the server
            val id = foundBeacon.provisioningInfo.id
            val serverAck = when (val forwardResult = networkClient.forwardBeaconPayload(id, beaconData)) {
                is SdkResult.Success -> forwardResult.value
                is SdkResult.Failure -> return forwardResult
            }

            // Relay the server's ACK back to the beacon
            return when (val relayResult = bleController.postSecurePayload(serverAck)) {
                is SdkResult.Success -> SdkResult.Success(Unit)
                is SdkResult.Failure -> relayResult
            }
        } finally {
            // Guarantees disconnection regardless of the outcome
            bleController.disconnect()
        }
    }

    // A private helper to encapsulate the connect-and-wait logic
    private suspend fun connectToReadyState(foundBeacon: FoundBeacon): SdkResult<Unit, SdkError> {
        when (val result = bleController.connect(foundBeacon.address)) {
            is SdkResult.Failure -> return result
            is SdkResult.Success -> { /* proceed */ }
        }

        val finalState = withTimeoutOrNull(12000L) {
            bleController.connectionState
                .filter { it is ConnectionState.Ready || it is ConnectionState.Failed }
                .first()
        }

        return when (finalState) {
            is ConnectionState.Ready -> SdkResult.Success(Unit)
            is ConnectionState.Failed -> SdkResult.Failure(SdkError.BleError("Connection failed: ${finalState.error}"))
            null -> SdkResult.Failure(SdkError.BleError("Connection timed out."))
            else -> SdkResult.Failure(SdkError.BleError("Unexpected connection state: $finalState"))
        }
    }
}