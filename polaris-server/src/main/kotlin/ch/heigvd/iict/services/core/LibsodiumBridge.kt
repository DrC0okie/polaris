package ch.heigvd.iict.services.core

import com.ionspin.kotlin.crypto.LibsodiumInitializer
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
}