package ch.drcookie.polaris_sdk.protocol

import ch.drcookie.polaris_sdk.crypto.CryptoUtils
import ch.drcookie.polaris_sdk.protocol.model.BroadcastPayload
import ch.drcookie.polaris_sdk.protocol.model.PoLRequest
import ch.drcookie.polaris_sdk.protocol.model.PoLResponse

@OptIn(ExperimentalUnsignedTypes::class)
class DefaultProtocolHandler(private val cryptoManager: CryptoUtils) : ProtocolHandler {
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