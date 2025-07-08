package ch.heigvd.iict.services.crypto

import io.quarkus.logging.Log
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.*

/**
 * Service providing cryptographic functionalities.
 *
 * This service acts as a facade over the [LibsodiumBridge], ensuring that the underlying
 * native library is initialized before use and providing simplified methods for common
 * cryptographic tasks like signature verification.
 */
@OptIn(ExperimentalUnsignedTypes::class)
@ApplicationScoped
class CryptoService {

    /**
     * Observes the Quarkus startup event to initialize the libsodium native library asynchronously.
     * This ensures that cryptographic operations are available without blocking the application's main thread.
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun onStart(@Observes ev: StartupEvent) {
        // Launch on a worker thread, so we don’t block the Vert.x event loop or Quarkus start
        GlobalScope.launch(Dispatchers.IO) {
            try {
                LibsodiumBridge.initialize(
                    logInfo  = { msg   -> Log.info("LibsodiumBridge: $msg") },
                    logError = { m, t -> Log.error("LibsodiumBridge: $m", t) }
                )
                Log.info("LibsodiumBridge initialized successfully.")
            } catch (e: Exception) {
                Log.error("LibsodiumBridge failed to initialize at startup.", e)
            }
        }
    }

    /**
     * Verifies an Ed25519 detached signature.
     *
     * @param signature The signature to verify.
     * @param message The original message that was signed.
     * @param publicKey The public key of the signer.
     * @return `true` if the signature is valid for the given message and public key, `false` otherwise.
     */
    fun verifyEd25519Signature(signature: UByteArray, message: UByteArray, publicKey: UByteArray): Boolean {
        ensureCoreInitialized()
        val isValid = LibsodiumBridge.verifyDetached(signature, message, publicKey)
        if (!isValid) {
            Log.warn("Ed25519 signature verification failed.")
        }
        return isValid
    }

    private fun ensureCoreInitialized() {
        if (!LibsodiumBridge.isInitialized) {
            Log.warn("LibsodiumBridge was not initialized prior to use. This should not happen if onStart worked.")
            runBlocking { LibsodiumBridge.initialize({Log.info(it)}, {msg,t -> Log.error(msg,t)}) }
            if (!LibsodiumBridge.isInitialized) {
                throw IllegalStateException("LibsodiumBridge not initialized. Cryptographic operations cannot proceed.")
            }
        }
    }
}
