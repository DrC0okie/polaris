package ch.drcookie.polaris_app.data.repository

import ch.drcookie.polaris_app.domain.interactor.logic.CryptoManager
import ch.drcookie.polaris_app.domain.model.*
import ch.drcookie.polaris_app.domain.repository.ProtocolRepository

@OptIn(ExperimentalUnsignedTypes::class)
class ProtocolRepositoryImpl(private val cryptoManager: CryptoManager) : ProtocolRepository {
    override fun signPoLRequest(request: PoLRequest, secretKey: UByteArray): PoLRequest {
        return cryptoManager.signPoLRequest(request, secretKey)
    }

    override fun verifyPoLResponse(response: PoLResponse, signedRequest: PoLRequest, beaconPK: UByteArray): Boolean {
        return cryptoManager.verifyPoLResponse(response, signedRequest, beaconPK)
    }

    override fun verifyBroadcast(payload: BroadcastPayload, beaconPublicKey: UByteArray): Boolean {
        return cryptoManager.verifyBeaconBroadcast(payload, beaconPublicKey)
    }

    override fun generateNonce(): UByteArray {
        return cryptoManager.generateNonce()
    }
}