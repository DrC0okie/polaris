package ch.drcookie.polaris_sdk.protocol

import ch.drcookie.polaris_sdk.crypto.CryptoUtils
import ch.drcookie.polaris_sdk.protocol.model.BroadcastPayload
import ch.drcookie.polaris_sdk.protocol.model.PoLRequest
import ch.drcookie.polaris_sdk.protocol.model.PoLResponse

/**
 * The default implementation of the [ProtocolHandler] interface.
 *
 * Simple facade, delegating all cryptographic operations to [CryptoUtils].
 *
 * @property cryptoUtils The utility object that performs the actual cryptographic computations.
 */
@OptIn(ExperimentalUnsignedTypes::class)
internal class DefaultProtocolHandler(private val cryptoUtils: CryptoUtils) : ProtocolHandler {
    override fun signPoLRequest(request: PoLRequest, secretKey: UByteArray): PoLRequest {
        return cryptoUtils.signPoLRequest(request, secretKey)
    }

    override fun verifyPoLResponse(
        response: PoLResponse,
        signedRequest: PoLRequest,
        beaconPublicKey: UByteArray,
    ): Boolean {
        return cryptoUtils.verifyPoLResponse(response, signedRequest, beaconPublicKey)
    }

    override fun verifyBroadcast(payload: BroadcastPayload, beaconPublicKey: UByteArray): Boolean {
        return cryptoUtils.verifyBeaconBroadcast(payload, beaconPublicKey)
    }

    override fun generateNonce(): UByteArray {
        return cryptoUtils.generateNonce()
    }
}