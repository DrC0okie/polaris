package ch.heigvd.iict.services.crypto

import io.quarkus.logging.Log
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.*

@OptIn(ExperimentalUnsignedTypes::class)
@ApplicationScoped
class CryptoService {

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

    private fun ensureCoreInitialized() {
        if (!LibsodiumBridge.isInitialized) {
            Log.warn("LibsodiumBridge was not initialized prior to use. This should not happen if onStart worked.")
            runBlocking { LibsodiumBridge.initialize({Log.info(it)}, {msg,t -> Log.error(msg,t)}) }
            if (!LibsodiumBridge.isInitialized) {
                throw IllegalStateException("LibsodiumBridge not initialized. Cryptographic operations cannot proceed.")
            }
        }
    }

    fun verifyEd25519Signature(signature: UByteArray, message: UByteArray, publicKey: UByteArray): Boolean {
        ensureCoreInitialized()
        val isValid = LibsodiumBridge.verifyDetached(signature, message, publicKey)
        if (!isValid) {
            Log.warn("Ed25519 signature verification failed.")
        }
        return isValid
    }
}
