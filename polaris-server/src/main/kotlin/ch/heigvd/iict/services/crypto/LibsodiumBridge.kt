package ch.heigvd.iict.services.crypto

import com.ionspin.kotlin.crypto.LibsodiumInitializer
import com.ionspin.kotlin.crypto.aead.AuthenticatedEncryptionWithAssociatedData
import com.ionspin.kotlin.crypto.scalarmult.ScalarMultiplication
import com.ionspin.kotlin.crypto.signature.Signature
import com.ionspin.kotlin.crypto.signature.SignatureKeyPair
import com.ionspin.kotlin.crypto.util.LibsodiumRandom
import com.ionspin.kotlin.crypto.signature.InvalidSignatureException


@OptIn(ExperimentalUnsignedTypes::class)
object LibsodiumBridge {
    var isInitialized = false   // Made public for easier checking in MainActivity
        private set


    suspend fun initialize(logInfo: (String) -> Unit = {}, logError: (String, Throwable?) -> Unit = { _, _ ->}) {
        if (!isInitialized) {
            try {
                LibsodiumInitializer.initialize()
                isInitialized = true
                logInfo("Libsodium initialized successfully via LibsodiumBridge.")
            } catch (e: Exception) {
                logError("LibsodiumBridge initialization failed.", e)
                throw e
            }
        }
    }

    private fun ensureInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("LibsodiumBridge not initialized. Call initialize() first from a coroutine context.")
        }
    }

    fun generateEd25519KeyPair(): SignatureKeyPair {
        ensureInitialized()
        return Signature.keypair()
    }

    fun generateNonce(size: Int): UByteArray {
        ensureInitialized()
        return LibsodiumRandom.buf(size)
    }

    fun signDetached(message: UByteArray, secretKey: UByteArray): UByteArray {
        ensureInitialized()
        return Signature.detached(message, secretKey)
    }

    fun verifyDetached(signature: UByteArray, message: UByteArray, publicKey: UByteArray): Boolean {
        ensureInitialized()
        return try {
            Signature.verifyDetached(signature, message, publicKey)
            true
        } catch (_: InvalidSignatureException) {
            false
        } catch (e: Exception) {
            throw e
        }
    }

    // Key Exchange & Scalar Multiplication
    fun scalarMultBase(secretKey: UByteArray): UByteArray {
        ensureInitialized()
        return ScalarMultiplication.scalarMultiplicationBase(secretKey)
    }

    fun scalarMult(secretKey: UByteArray, publicKey: UByteArray): UByteArray {
        ensureInitialized()
        return ScalarMultiplication.scalarMultiplication(secretKey, publicKey)
    }

    // AEAD Encryption (ChaCha20-Poly1305 IETF)
    fun aeadEncrypt(message: UByteArray, associatedData: UByteArray, nonce: UByteArray, key: UByteArray): UByteArray {
        ensureInitialized()
        // This function from the lib returns ciphertext and tag combined
        return AuthenticatedEncryptionWithAssociatedData.chaCha20Poly1305IetfEncrypt(message, associatedData, nonce, key)
    }

    fun aeadDecrypt(ciphertextWithTag: UByteArray, associatedData: UByteArray, nonce: UByteArray, key: UByteArray): UByteArray {
        ensureInitialized()
        return AuthenticatedEncryptionWithAssociatedData.chaCha20Poly1305IetfDecrypt(ciphertextWithTag, associatedData, nonce, key)
    }
}