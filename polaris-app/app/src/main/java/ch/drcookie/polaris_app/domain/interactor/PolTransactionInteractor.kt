package ch.drcookie.polaris_app.domain.interactor

import ch.drcookie.polaris_app.data.datasource.ble.ConnectionState
import ch.drcookie.polaris_app.domain.model.FoundBeacon
import ch.drcookie.polaris_app.domain.model.PoLRequest
import ch.drcookie.polaris_app.domain.model.PoLToken
import ch.drcookie.polaris_app.domain.repository.AuthRepository
import ch.drcookie.polaris_app.domain.repository.BleDataSource
import ch.drcookie.polaris_app.domain.repository.KeyRepository
import ch.drcookie.polaris_app.domain.repository.ProtocolRepository
import kotlinx.coroutines.flow.first

class PolTransactionInteractor(
    private val bleDataSource: BleDataSource,
    private val authRepository: AuthRepository,
    private val keyRepository: KeyRepository,
    private val protocolRepo: ProtocolRepository
) {
    // The 'invoke' operator allows calling the class like a function
    @OptIn(ExperimentalUnsignedTypes::class)
    suspend operator fun invoke(foundBeacon: FoundBeacon): PoLToken {
        try {
            // Connect
            bleDataSource.connect(foundBeacon.address)
            bleDataSource.connectionState.first { it is ConnectionState.Ready }

            // get necessary data
            val (phonePk, phoneSk) = keyRepository.getOrCreateSignatureKeyPair()
            val phoneId = authRepository.getPhoneId().toULong()
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
            val response = bleDataSource.requestPoL(signedRequest)

            // Verify response signatue
            val isValid = protocolRepo.verifyPoLResponse(response, signedRequest, foundBeacon.provisioningInfo.publicKey)
            if (!isValid) throw SecurityException("Invalid beacon signature during PoL transaction!")
            return PoLToken.create(signedRequest, response, foundBeacon.provisioningInfo.publicKey)

        } finally {
            bleDataSource.disconnect()
        }
    }
}