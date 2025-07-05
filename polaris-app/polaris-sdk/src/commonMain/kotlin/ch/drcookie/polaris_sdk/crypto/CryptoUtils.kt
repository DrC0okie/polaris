package ch.drcookie.polaris_sdk.crypto

import ch.drcookie.polaris_sdk.ble.model.Beacon
import io.github.oshai.kotlinlogging.KotlinLogging
import ch.drcookie.polaris_sdk.protocol.model.BroadcastPayload
import ch.drcookie.polaris_sdk.protocol.model.Constants
import ch.drcookie.polaris_sdk.protocol.model.PoLRequest
import ch.drcookie.polaris_sdk.protocol.model.PoLResponse
import ch.drcookie.polaris_sdk.protocol.model.getEffectivelySignedData
import ch.drcookie.polaris_sdk.protocol.model.getSignedData
import com.ionspin.kotlin.crypto.signature.InvalidSignatureException
import com.ionspin.kotlin.crypto.signature.Signature
import com.ionspin.kotlin.crypto.signature.SignatureKeyPair
import com.ionspin.kotlin.crypto.util.LibsodiumRandom

private val Log = KotlinLogging.logger {}

/**
 * Centralizes all low-level cryptographic operations.
 *
 * This object wraps libsodium to provide simple functions for key generation, signing, and verification.
 */
@OptIn(ExperimentalUnsignedTypes::class)
internal object CryptoUtils {

    /** Generates a new, random Ed25519 key pair. */
    internal fun generateKeyPair(): Pair<UByteArray, UByteArray> {
        val keyPair: SignatureKeyPair = Signature.keypair()
        return keyPair.publicKey to keyPair.secretKey
    }

    /** Generates a cryptographically secure random nonce. */
    internal fun generateNonce(): UByteArray {
        return LibsodiumRandom.buf(Constants.PROTOCOL_NONCE)
    }

    /** Creates a signature for a [PoLRequest] and returns a new, signed instance. */
    internal fun signPoLRequest(request: PoLRequest, sk: UByteArray): PoLRequest {
        val dataToSign = request.getSignedData()
        val signature = Signature.detached(dataToSign, sk)
        return request.copy(phoneSig = signature)
    }

    /** Verifies a [PoLResponse], checking both the nonce and the signature. */
    internal fun verifyPoLResponse(resp: PoLResponse, originalSignedReq: PoLRequest, beaconPk: UByteArray): Boolean {
        if (!resp.nonce.contentEquals(originalSignedReq.nonce)) {
            Log.error { "Nonce mismatch in response verification!" }
            return false
        }
        val dataActuallySignedByBeacon = resp.getEffectivelySignedData(originalSignedReq)
        return try {
            // verifyDetached throws on failure, returns Unit on success
            Signature.verifyDetached(resp.beaconSig, dataActuallySignedByBeacon, beaconPk)
            true // Signature is valid
        } catch (e: InvalidSignatureException) {
            Log.error(e) { "Response signature verification failed: Invalid signature" }
            false
        } catch (e: Exception) {
            Log.error(e) { "Response signature verification failed: Other error" }
            false
        }
    }

    /** Verifies the signature of a [BroadcastPayload]. */
    internal fun verifyBeaconBroadcast(payload: BroadcastPayload, knownBeacon: Beacon): Boolean {

        // Verify freshness
        if (payload.counter < knownBeacon.lastKnownCounter){
            return false
        }

        return try {
            // The verifyDetached function will throw an exception if the signature is invalid.
            Signature.verifyDetached(payload.signature, payload.getSignedData(), knownBeacon.publicKey)
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