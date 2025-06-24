package ch.drcookie.polaris_sdk.data.repository

import ch.drcookie.polaris_sdk.domain.interactor.logic.CryptoManager
import ch.drcookie.polaris_sdk.domain.model.*
import ch.drcookie.polaris_sdk.domain.repository.ProtocolRepository

@OptIn(ExperimentalUnsignedTypes::class)
class ProtocolRepositoryImpl(private val cryptoManager: CryptoManager) : ProtocolRepository {
    override fun signPoLRequest(request: PoLRequest, secretKey: UByteArray): PoLRequest {
        return cryptoManager.signPoLRequest(request, secretKey)
    }

    override fun verifyPoLResponse(
        response: PoLResponse,
        signedRequest: PoLRequest,
        beaconPublicKey: UByteArray
    ): Boolean {
        return cryptoManager.verifyPoLResponse(response, signedRequest, beaconPublicKey)
    }

    override fun verifyBroadcast(payload: BroadcastPayload, beaconPublicKey: UByteArray): Boolean {
        return cryptoManager.verifyBeaconBroadcast(payload, beaconPublicKey)
    }

    override fun generateNonce(): UByteArray {
        return cryptoManager.generateNonce()
    }
}