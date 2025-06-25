package ch.drcookie.polaris_sdk.protocol

import ch.drcookie.polaris_sdk.protocol.model.BroadcastPayload
import ch.drcookie.polaris_sdk.protocol.model.PoLRequest
import ch.drcookie.polaris_sdk.protocol.model.PoLResponse

/**
 * A repository for handling the cryptographic operations of the PoL and Broadcast protocols.
 */
@OptIn(ExperimentalUnsignedTypes::class)
public interface ProtocolHandler {
    public fun signPoLRequest(request: PoLRequest, secretKey: UByteArray): PoLRequest
    public fun verifyPoLResponse(response: PoLResponse, signedRequest: PoLRequest, beaconPublicKey: UByteArray): Boolean
    public fun verifyBroadcast(payload: BroadcastPayload, beaconPublicKey: UByteArray): Boolean
    public fun generateNonce(): UByteArray
}