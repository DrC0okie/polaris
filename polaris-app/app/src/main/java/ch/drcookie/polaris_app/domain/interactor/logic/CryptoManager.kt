package ch.drcookie.polaris_app.domain.interactor.logic

import io.github.oshai.kotlinlogging.KotlinLogging
import ch.drcookie.polaris_app.domain.model.BroadcastPayload
import ch.drcookie.polaris_app.domain.model.PoLRequest
import ch.drcookie.polaris_app.domain.model.PoLResponse
import ch.drcookie.polaris_app.domain.model.Constants
import com.ionspin.kotlin.crypto.signature.InvalidSignatureException
import com.ionspin.kotlin.crypto.signature.Signature
import com.ionspin.kotlin.crypto.signature.SignatureKeyPair
import com.ionspin.kotlin.crypto.util.LibsodiumRandom

private val Log = KotlinLogging.logger {}

@OptIn(ExperimentalUnsignedTypes::class)
object CryptoManager {

    /**
     * Generates a new, random Ed25519 key pair. This is a stateless function.
     * It does not store or remember the key.
     */
    fun generateKeyPair(): Pair<UByteArray, UByteArray> {
        val keyPair: SignatureKeyPair = Signature.keypair()
        return keyPair.publicKey to keyPair.secretKey
    }

    fun generateNonce(): UByteArray {
        return LibsodiumRandom.buf(Constants.PROTOCOL_NONCE_SIZE)
    }


    fun signPoLRequest(request: PoLRequest, sk: UByteArray): PoLRequest {
        val dataToSign = request.getSignedData()
        val signature = Signature.detached(dataToSign, sk)
        return request.copy(phoneSig = signature)
    }

    fun verifyPoLResponse(resp: PoLResponse, originalSignedReq: PoLRequest, beaconPk: UByteArray): Boolean {
        if (!resp.nonce.contentEquals(originalSignedReq.nonce)) {
            Log.error { "Nonce mismatch in response verification!" }
            return false
        }
        val dataActuallySignedByBeacon = resp.getEffectivelySignedData(originalSignedReq)
        return try {
            // verifyDetached throws on failure, returns Unit on success
            Signature.verifyDetached(
                signature = resp.beaconSig,
                message = dataActuallySignedByBeacon,
                publicKey = beaconPk
            )
            true // Signature is valid
        } catch (e: InvalidSignatureException) {
            Log.error(e) { "Response signature verification failed: Invalid signature" }
            false
        } catch (e: Exception) {
            Log.error(e) { "Response signature verification failed: Other error" }
            false
        }
    }

    /**
     * Verifies the signature of a broadcast payload.
     *
     * @param payload The broadcast payload containing the data and signature to verify.
     * @param beaconPk The public key of the beacon that should have signed this payload.
     * @return True if the signature is valid for the given data and public key, false otherwise.
     */
    fun verifyBeaconBroadcast(payload: BroadcastPayload, beaconPk: UByteArray): Boolean {
        // Reconstruct the exact data that was signed on the beacon
        val dataToVerify = payload.getSignedData()

        return try {
            // The verifyDetached function will throw an exception if the signature is invalid.
            Signature.verifyDetached(
                signature = payload.signature,
                message = dataToVerify,
                publicKey = beaconPk
            )
            true
        } catch (e: InvalidSignatureException) {
            Log.warn(e) { "Broadcast signature verification failed for beacon #${payload.beaconId}" }
            false
        } catch (e: Exception) {
            Log.error(e) { "An unexpected error occurred during broadcast verification" }
            false
        }
    }
}