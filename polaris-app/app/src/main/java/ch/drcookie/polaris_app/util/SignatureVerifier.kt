package ch.drcookie.polaris_app.util

import ch.drcookie.polaris_app.data.model.BroadcastPayload

object SignatureVerifier {
    /**
     * Verifies the signature of a parsed broadcast payload.
     *
     * @param payload The parsed payload containing the data and signature.
     * @param beaconPublicKey The public key of the beacon that allegedly sent the broadcast.
     * @return True if the signature is valid, false otherwise.
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    fun verifyBroadcast(payload: BroadcastPayload, beaconPublicKey: UByteArray): Boolean {
        // We can just call the method on the Crypto object.
        // This class acts as a clean, intention-revealing layer.
        return Crypto.verifyBeaconBroadcast(payload, beaconPublicKey)
    }
}