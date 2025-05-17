package ch.drcookie.polaris_app

import android.util.Log
import com.ionspin.kotlin.crypto.LibsodiumInitializer
import com.ionspin.kotlin.crypto.signature.InvalidSignatureException
import com.ionspin.kotlin.crypto.signature.Signature
import com.ionspin.kotlin.crypto.signature.SignatureKeyPair
import com.ionspin.kotlin.crypto.util.LibsodiumRandom
import ch.drcookie.polaris_app.Utils.toHexString

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
}