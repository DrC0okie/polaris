package ch.drcookie.polaris_sdk.api.flows

import ch.drcookie.polaris_sdk.ble.model.ConnectionState
import ch.drcookie.polaris_sdk.ble.model.FoundBeacon
import ch.drcookie.polaris_sdk.protocol.model.PoLRequest
import ch.drcookie.polaris_sdk.model.PoLToken
import ch.drcookie.polaris_sdk.network.ApiClient
import ch.drcookie.polaris_sdk.ble.BleController
import ch.drcookie.polaris_sdk.storage.KeyStore
import ch.drcookie.polaris_sdk.protocol.ProtocolHandler
import kotlinx.coroutines.flow.first

public class PolTransactionFlow(
    private val bleController: BleController,
    private val apiClient: ApiClient,
    private val keyStore: KeyStore,
    private val protocolRepo: ProtocolHandler
) {
    // The 'invoke' operator allows calling the class like a function
    @OptIn(ExperimentalUnsignedTypes::class)
    public suspend operator fun invoke(foundBeacon: FoundBeacon): PoLToken {
        try {
            // Connect
            bleController.connect(foundBeacon.address)
            bleController.connectionState.first { it is ConnectionState.Ready }

            // get necessary data
            val (phonePk, phoneSk) = keyStore.getOrCreateSignatureKeyPair()
            val phoneId = apiClient.getPhoneId().toULong()
            if (phoneId == 0uL) throw IllegalStateException("Phone ID not available. Please register first.")

            // Construct the request
            val request = PoLRequest(
                flags = 0u,
                phoneId = phoneId,
                beaconId = foundBeacon.provisioningInfo.beaconId,
                nonce = protocolRepo.generateNonce(),
                phonePk = phonePk
            )

            // Sign the request
            val signedRequest = protocolRepo.signPoLRequest(request, phoneSk)

            // Send and receive the response
            val response = bleController.requestPoL(signedRequest)

            // Verify response signatue
            val isValid = protocolRepo.verifyPoLResponse(response, signedRequest, foundBeacon.provisioningInfo.publicKey)
            if (!isValid) throw Exception("Invalid beacon signature during PoL transaction!")
            return PoLToken.create(signedRequest, response, foundBeacon.provisioningInfo.publicKey)

        } finally {
            bleController.disconnect()
        }
    }
}