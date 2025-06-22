package ch.drcookie.polaris_app.util

import android.util.Log
import ch.drcookie.polaris_app.data.model.BroadcastPayload
import ch.drcookie.polaris_app.data.model.PoLRequest
import ch.drcookie.polaris_app.data.model.PoLResponse
import ch.drcookie.polaris_app.util.Utils.toHexString
import com.ionspin.kotlin.crypto.LibsodiumInitializer
import com.ionspin.kotlin.crypto.signature.InvalidSignatureException
import com.ionspin.kotlin.crypto.signature.Signature
import com.ionspin.kotlin.crypto.signature.SignatureKeyPair
import com.ionspin.kotlin.crypto.util.LibsodiumRandom

@OptIn(ExperimentalUnsignedTypes::class)
object Crypto {
    private val tag = this::class.simpleName
    private var phoneSk: UByteArray? = null
    private var phonePk: UByteArray? = null
    var isInitialized = false   // Made public for easier checking in MainActivity
        private set             // but only settable from within this object

    suspend fun initialize() {
        if (!isInitialized) {
            try {
                LibsodiumInitializer.initialize()
                isInitialized = true
                Log.d(tag, "Libsodium initialized successfully.")
            } catch (e: Exception) {
                Log.e(tag, "Libsodium initialization failed", e)
                // Handle initialization failure
            }
        }
    }

    private fun ensureInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("Libsodium not initialized. Call Crypto.initialize() first from a coroutine context.")
        }
    }

    fun getOrGeneratePhoneKeyPair(): Pair<UByteArray, UByteArray> {
        ensureInitialized()
        if (phonePk != null && phoneSk != null) {
            return Pair(phonePk!!, phoneSk!!)
        }

        // TODO load from Keystore/secure prefs, or generate and save.
        val keyPair: SignatureKeyPair = Signature.keypair()

        phonePk = keyPair.publicKey
        phoneSk = keyPair.secretKey
        Log.d(tag, "Generated new phone Ed25519 key pair.")
        Log.d(tag, "Phone PK: ${phonePk!!.toHexString()}")
        return Pair(phonePk!!, phoneSk!!)
    }

    fun generateNonce(): UByteArray {
        ensureInitialized()
        return LibsodiumRandom.buf(PoLConstants.PROTOCOL_NONCE_SIZE)
    }

    fun signPoLRequest(request: PoLRequest, sk: UByteArray): PoLRequest {
        ensureInitialized()
        val dataToSign = request.getSignedData()
        val signature = Signature.detached(dataToSign, sk)
        return request.copy(phoneSig = signature)
    }

    fun verifyPoLResponse(resp: PoLResponse, originalReq: PoLRequest, beaconPk: UByteArray): Boolean {
        ensureInitialized()
        if (!resp.nonce.contentEquals(originalReq.nonce)) {
            Log.e(tag, "Nonce mismatch in response verification!")
            return false
        }
        val dataActuallySignedByBeacon = resp.getEffectivelySignedData(originalReq)
        return try {
            // verifyDetached throws on failure, returns Unit on success
            Signature.verifyDetached(
                signature = resp.beaconSig,
                message = dataActuallySignedByBeacon,
                publicKey = beaconPk
            )
            true // Signature is valid
        } catch (e: InvalidSignatureException) {
            Log.e(tag, "Response signature verification failed: Invalid signature", e)
            false
        } catch (e: Exception) {
            Log.e(tag, "Response signature verification failed: Other error", e)
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
        ensureInitialized()
        // Reconstruct the exact data that was signed on the beacon
        val dataToVerify = payload.getSignedData()

        return try {
            // The verifyDetached function will throw an exception if the signature is invalid.
            Signature.verifyDetached(
                signature = payload.signature,
                message = dataToVerify,
                publicKey = beaconPk
            )
            // If no exception is thrown, the signature is valid.
            true
        } catch (e: InvalidSignatureException) {
            // Log the failure for debugging purposes
            Log.w(tag, "Broadcast signature verification failed for beacon #${payload.beaconId}")
            false
        } catch (e: Exception) {
            // Catch any other potential errors from the crypto library
            Log.e(tag, "An unexpected error occurred during broadcast verification", e)
            false
        }
    }

}