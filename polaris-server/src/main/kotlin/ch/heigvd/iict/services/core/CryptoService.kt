package ch.heigvd.iict.services.core

import io.quarkus.logging.Log
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalUnsignedTypes::class)
@ApplicationScoped
class CryptoService {

    fun onStart(@Observes ev: StartupEvent) {
        Log.info("CryptoService: Initializing LibsodiumBridge...")
        runBlocking {
            LibsodiumBridge.initialize(
                logInfo = { message -> Log.info("LibsodiumBridge: $message") },
                logError = { message, throwable -> Log.error("LibsodiumBridge: $message", throwable) }
            )
        }
        if (!LibsodiumBridge.isInitialized) {
            throw RuntimeException("Failed to initialize LibsodiumBridge at startup.")
        }
    }

    private fun ensureCoreInitialized() {
        if (!LibsodiumBridge.isInitialized) {
            Log.warn("LibsodiumBridge was not initialized prior to use. This should not happen if onStart worked.")
            runBlocking { LibsodiumBridge.initialize(logInfo = {Log.info(it)}, logError = {msg,t -> Log.error(msg,t)}) }
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
