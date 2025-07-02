package ch.drcookie.polaris_sdk.api.use_case

import ch.drcookie.polaris_sdk.api.SdkError
import ch.drcookie.polaris_sdk.api.SdkResult
import ch.drcookie.polaris_sdk.ble.model.ConnectionState
import ch.drcookie.polaris_sdk.ble.model.FoundBeacon
import ch.drcookie.polaris_sdk.protocol.model.PoLRequest
import ch.drcookie.polaris_sdk.protocol.model.PoLToken
import ch.drcookie.polaris_sdk.network.NetworkClient
import ch.drcookie.polaris_sdk.ble.BleController
import ch.drcookie.polaris_sdk.storage.KeyStore
import ch.drcookie.polaris_sdk.protocol.ProtocolHandler
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * A high-level use case that performs a PoL transaction.
 *
 * This manages the entire connection lifecycle. It connects to a
 * beacon, performs the cryptographic challenge-response, verifies the outcome, and disconnects.
 *
 * @property bleController The controller for the BLE connection and data exchange.
 * @property networkClient The client to retrieve the device phone ID.
 * @property keyStore The store for retrieving the device signing keys.
 * @property protocolHandler The handler for signing the request and verifying the response.
 */
public class PolTransaction(
    private val bleController: BleController,
    private val networkClient: NetworkClient,
    private val keyStore: KeyStore,
    private val protocolHandler: ProtocolHandler,
) {
    /**
     * Executes the Proof-of-Location transaction with a given beacon.
     *
     * @param foundBeacon The [FoundBeacon] to connect to.
     * @return An [SdkResult] containing the verified [PoLToken] on success.
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    public suspend operator fun invoke(foundBeacon: FoundBeacon): SdkResult<PoLToken, SdkError> {
        try {
            // Initiate connection
            when (val connectResult = bleController.connect(foundBeacon.address)) {
                is SdkResult.Failure -> return connectResult
                is SdkResult.Success -> { /* Continue */
                }
            }

            // Await a "Ready" or "Failed" state from the connection flow, with a timeout.
            val status = withTimeoutOrNull(10000L) {
                bleController.connectionState
                    .filter { it is ConnectionState.Ready || it is ConnectionState.Failed }
                    .first()
            }

            // Handle the outcome of the connection attempt.
            when (status) {
                is ConnectionState.Ready -> { /* Connection successful, continue */}

                is ConnectionState.Failed -> return SdkResult.Failure(SdkError.BleError("Connection failed: ${status.error}"))
                null -> return SdkResult.Failure(SdkError.BleError("Connection timed out."))
                else -> return SdkResult.Failure(SdkError.BleError("Unexpected connection state: $status"))
            }

            // Get the signature key pair
            val (phonePk, phoneSk) = when (val keyResult = keyStore.getOrCreateSignatureKeyPair()) {
                is SdkResult.Success -> keyResult.value
                is SdkResult.Failure -> return keyResult
            }

            // Check precondition for Phone ID
            val phoneId = networkClient.getPhoneId().toULong()
            if (phoneId == 0uL) {
                return SdkResult.Failure(SdkError.PreconditionError("Phone ID not available. Please register first."))
            }

            // Construct the request
            val request = PoLRequest(
                flags = 0u,
                phoneId = phoneId,
                beaconId = foundBeacon.info.id,
                nonce = protocolHandler.generateNonce(),
                phonePk = phonePk
            )

            // Sign the request
            val signedRequest = protocolHandler.signPoLRequest(request, phoneSk)

            // PoL transaction
            val responseResult = bleController.requestPoL(signedRequest)
            val response = when (responseResult) {
                is SdkResult.Success -> responseResult.value
                is SdkResult.Failure -> return responseResult
            }

            // Step 8: Verify the beacon's response
            val isValid = protocolHandler.verifyPoLResponse(response, signedRequest, foundBeacon.info.publicKey)
            if (!isValid) {
                return SdkResult.Failure(SdkError.ProtocolError("Invalid beacon signature during PoL transaction!"))
            }

            // Create and return the final token
            val token = PoLToken.create(signedRequest, response, foundBeacon.info.publicKey)
            return SdkResult.Success(token)

        } finally {
            bleController.disconnect()
        }
    }
}