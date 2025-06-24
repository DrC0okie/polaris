package ch.drcookie.polaris_app.domain.repository

import ch.drcookie.polaris_app.domain.model.BroadcastPayload
import ch.drcookie.polaris_app.domain.model.PoLRequest
import ch.drcookie.polaris_app.domain.model.PoLResponse

/**
 * A repository for handling the cryptographic operations of the PoL and Broadcast protocols.
 */
@OptIn(ExperimentalUnsignedTypes::class)
interface ProtocolRepository {
    fun signPoLRequest(request: PoLRequest, secretKey: UByteArray): PoLRequest
    fun verifyPoLResponse(response: PoLResponse, signedRequest: PoLRequest, beaconPublicKey: UByteArray): Boolean
    fun verifyBroadcast(payload: BroadcastPayload, beaconPublicKey: UByteArray): Boolean
    fun generateNonce(): UByteArray
}