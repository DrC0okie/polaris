package ch.drcookie.polaris_app.repository

import ch.drcookie.polaris_app.data.ble.BleDataSource
import ch.drcookie.polaris_app.data.ble.ConnectionState
import ch.drcookie.polaris_app.data.local.UserPreferences
import ch.drcookie.polaris_app.data.model.FoundBeacon
import ch.drcookie.polaris_app.data.model.PoLRequest
import ch.drcookie.polaris_app.data.model.PoLToken
import ch.drcookie.polaris_app.util.Crypto
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.io.IOException

/**
 * Manages the connection and data transactions for a single, specific beacon.
 * An instance of this class should be created for each interaction session.
 *
 * @param foundBeacon The specific beacon (with its address) to interact with.
 * @param bleDataSource The shared data source for all BLE communication.
 * @param userPrefs The shared source for user-specific data like phone ID.
 */
class BeaconInteractor(
    private val foundBeacon: FoundBeacon,
    private val bleDataSource: BleDataSource,
    private val userPrefs: UserPreferences
) {
    val connectionState: StateFlow<ConnectionState> = bleDataSource.connectionState

    suspend fun connect() {
        // Only attempt to connect if not already connected/connecting
        if (connectionState.value is ConnectionState.Disconnected || connectionState.value is ConnectionState.Failed) {
            bleDataSource.connect(foundBeacon.address)
        }
        // Wait until the connection is fully established and ready for communication
        connectionState.first { it is ConnectionState.Ready }
    }

    fun disconnect() {
        bleDataSource.disconnect()
    }

    /**
     * Performs the full Proof-of-Location transaction.
     * Assumes a connection is already established and ready.
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun performPoLTransaction(): PoLToken {
        // Create and sign the PoLRequest
        val (phonePk, phoneSk) = Crypto.getOrGeneratePhoneKeyPair()
        val phoneId = userPrefs.phoneId.toULong()
        if (phoneId == 0uL) throw IllegalStateException("Phone ID not available. Please register first.")

        var request = PoLRequest(
            flags = 0u,
            phoneId = phoneId,
            beaconId = foundBeacon.provisioningInfo.beaconId,
            nonce = Crypto.generateNonce(),
            phonePk = phonePk
        )
        request = Crypto.signPoLRequest(request, phoneSk)

        // Send the request and get the response via the BleDataSource
        val response = bleDataSource.requestPoL(request)

        // Verify the response signature (can be optional)
        val isValid = Crypto.verifyPoLResponse(response, request, foundBeacon.provisioningInfo.publicKey)
        if (!isValid) throw SecurityException("Invalid beacon signature during PoL transaction!")

        // Create and return the final token
        return PoLToken.create(request, response, foundBeacon.provisioningInfo.publicKey)
    }

    /**
     * Delivers a secure, encrypted payload to the beacon and returns its ACK/ERR response.
     * Assumes a connection is already established and ready.
     *
     * @param encryptedBlob The raw, encrypted data to send.
     * @return The raw, encrypted ACK/ERR data received from the beacon.
     */
    suspend fun deliverSecurePayload(encryptedBlob: ByteArray): ByteArray {
        return try {
            bleDataSource.deliverSecurePayload(encryptedBlob)
        } catch (e: Exception) {
            throw IOException("Failed to deliver secure payload to beacon ${foundBeacon.address}. Reason: ${e.message}", e)
        }
    }
}