package ch.drcookie.polaris_app.domain.interactor

import ch.drcookie.polaris_app.data.datasource.ble.ConnectionState
import ch.drcookie.polaris_app.domain.interactor.logic.CryptoManager
import ch.drcookie.polaris_app.domain.model.FoundBeacon
import ch.drcookie.polaris_app.domain.model.PoLRequest
import ch.drcookie.polaris_app.domain.model.PoLToken
import ch.drcookie.polaris_app.domain.repository.BleDataSource
import ch.drcookie.polaris_app.domain.repository.LocalPreferences
import kotlinx.coroutines.flow.first

class PolTransactionInteractor(
    private val bleDataSource: BleDataSource,
    private val localPreferences: LocalPreferences,
    private val cryptoManager: CryptoManager
) {
    // The 'invoke' operator allows calling the class like a function
    @OptIn(ExperimentalUnsignedTypes::class)
    suspend operator fun invoke(foundBeacon: FoundBeacon): PoLToken {
        try {
            // Connect
            bleDataSource.connect(foundBeacon.address)
            bleDataSource.connectionState.first { it is ConnectionState.Ready }

            // get necessary data
            val (phonePk, phoneSk) = cryptoManager.getOrGeneratePhoneKeyPair()
            val phoneId = localPreferences.phoneId.toULong()
            if (phoneId == 0uL) throw IllegalStateException("Phone ID not available. Please register first.")

            // Construct the request
            var request = PoLRequest(
                flags = 0u,
                phoneId = phoneId,
                beaconId = foundBeacon.provisioningInfo.beaconId,
                nonce = CryptoManager.generateNonce(),
                phonePk = phonePk
            )

            // Sign the request
            val signedRequest = cryptoManager.signPoLRequest(request, phoneSk)

            // Send and receive the response
            val response = bleDataSource.requestPoL(signedRequest)

            // Verify response signatue
            val isValid = CryptoManager.verifyPoLResponse(response, request, foundBeacon.provisioningInfo.publicKey)
            if (!isValid) throw SecurityException("Invalid beacon signature during PoL transaction!")
            return PoLToken.create(request, response, foundBeacon.provisioningInfo.publicKey)

        } finally {
            bleDataSource.disconnect()
        }
    }
}