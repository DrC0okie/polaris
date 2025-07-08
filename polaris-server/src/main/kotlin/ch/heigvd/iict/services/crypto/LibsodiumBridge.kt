package ch.heigvd.iict.services.crypto

import com.ionspin.kotlin.crypto.LibsodiumInitializer
import com.ionspin.kotlin.crypto.aead.AuthenticatedEncryptionWithAssociatedData
import com.ionspin.kotlin.crypto.scalarmult.ScalarMultiplication
import com.ionspin.kotlin.crypto.signature.Signature
import com.ionspin.kotlin.crypto.signature.SignatureKeyPair
import com.ionspin.kotlin.crypto.util.LibsodiumRandom
import com.ionspin.kotlin.crypto.signature.InvalidSignatureException

/**
 * Singleton bridge providing direct, access to the libsodium cryptographic library.
 *
 * This object wraps the `multiplatform-crypto-libsodium-bindings` library to offer a simplified,
 * consistent API for all cryptographic primitives used in the Polaris project. It ensures that
 * the native libsodium library is initialized exactly once.
 */
@OptIn(ExperimentalUnsignedTypes::class)
object LibsodiumBridge {

    /** Indicates whether the underlying native libsodium library has been successfully initialized. */
    var isInitialized = false
        private set


    /**
     * Initializes the native libsodium library. Must be called once from a coroutine context before any other
     * cryptographic function is used.
     *
     * @param logInfo A lambda for logging informational messages.
     * @param logError A lambda for logging errors.
     */
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

    /** Generates a new Ed25519 key pair for signing. */
    fun generateEd25519KeyPair(): SignatureKeyPair {
        ensureInitialized()
        return Signature.keypair()
    }

    /** Generates a random nonce of a specified size. */
    fun generateNonce(size: Int): UByteArray {
        ensureInitialized()
        return LibsodiumRandom.buf(size)
    }

    /** Creates an Ed25519 detached signature for a message. */
    fun signDetached(message: UByteArray, secretKey: UByteArray): UByteArray {
        ensureInitialized()
        return Signature.detached(message, secretKey)
    }

    /** Verifies an Ed25519 detached signature. Returns `false` on failure instead of throwing. */
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

    /**
     * Computes a public key from a secret key using Curve25519 base point multiplication.
     * Used for generating an X25519 public key.
     */
    fun scalarMultBase(secretKey: UByteArray): UByteArray {
        ensureInitialized()
        return ScalarMultiplication.scalarMultiplicationBase(secretKey)
    }

    /**
     * Performs a Diffie-Hellman key exchange using X25519.
     * Computes a shared secret from one party's secret key and another party's public key.
     */
    fun scalarMult(secretKey: UByteArray, publicKey: UByteArray): UByteArray {
        ensureInitialized()
        return ScalarMultiplication.scalarMultiplication(secretKey, publicKey)
    }

    /**
     * Encrypts and authenticates a message using ChaCha20-Poly1305 (IETF variant).
     * @return The combined ciphertext and authentication tag.
     */
    fun aeadEncrypt(message: UByteArray, associatedData: UByteArray, nonce: UByteArray, key: UByteArray): UByteArray {
        ensureInitialized()
        // This function from the lib returns ciphertext and tag combined
        return AuthenticatedEncryptionWithAssociatedData.chaCha20Poly1305IetfEncrypt(message, associatedData, nonce, key)
    }

    /**
     * Decrypts and verifies a ChaCha20-Poly1305 (IETF variant) ciphertext.
     * @return The original plaintext if decryption and authentication succeed.
     * @throws com.ionspin.kotlin.crypto.aead.AeadCorrupedOrTamperedDataException if the ciphertext is invalid.
     */
    fun aeadDecrypt(ciphertextWithTag: UByteArray, associatedData: UByteArray, nonce: UByteArray, key: UByteArray): UByteArray {
        ensureInitialized()
        return AuthenticatedEncryptionWithAssociatedData.chaCha20Poly1305IetfDecrypt(ciphertextWithTag, associatedData, nonce, key)
    }

    private fun ensureInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("LibsodiumBridge not initialized. Call initialize() first from a coroutine context.")
        }
    }
}