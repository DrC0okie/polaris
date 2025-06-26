package ch.drcookie.polaris_sdk.api

import ch.drcookie.polaris_sdk.api.config.PolarisConfig
import ch.drcookie.polaris_sdk.ble.AndroidBleController
import ch.drcookie.polaris_sdk.network.KtorApiClient
import ch.drcookie.polaris_sdk.storage.DefaultKeyStore
import ch.drcookie.polaris_sdk.protocol.DefaultProtocolHandler
import ch.drcookie.polaris_sdk.ble.util.BeaconDataParser
import ch.drcookie.polaris_sdk.crypto.CryptoUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import ch.drcookie.polaris_sdk.network.ApiClient
import ch.drcookie.polaris_sdk.ble.BleController
import ch.drcookie.polaris_sdk.network.NoOpApiClient
import ch.drcookie.polaris_sdk.storage.KeyStore
import ch.drcookie.polaris_sdk.protocol.ProtocolHandler
import com.ionspin.kotlin.crypto.LibsodiumInitializer
import com.liftric.kvault.KVault

private val logger = KotlinLogging.logger {}

// Provide the 'actual' implementation for the SdkInitializer
internal actual class SdkInitializer {

    // Holds the initialized repositories.
    private class AndroidSdk(
        override val apiClient: ApiClient,
        override val keyStore: KeyStore,
        override val protocolHandler: ProtocolHandler,
        override val bleController: BleController,
    ) : PolarisDependencies {
        fun performShutdown() {
            bleController.cancelAll()
            apiClient.closeClient()
        }
    }

    private var sdkInstance: AndroidSdk? = null

    internal actual suspend fun initialize(context: PlatformContext, config: PolarisConfig): PolarisDependencies {
        logger.info { "Initializing Polaris SDK for Android..." }

        try {
            LibsodiumInitializer.initialize()
        } catch (e: Exception) {
            logger.error(e) { "FATAL: Libsodium initialization failed." }
            throw e // Re-throw to fail initialization
        }

        // Common, stateless utilities
        val cryptoUtils = CryptoUtils
        val beaconDataParser = BeaconDataParser

        // Platform-specific implementations
        val androidBleController: BleController = AndroidBleController(context, beaconDataParser, config.bleConfig)

        // Common implementations, injecting the platform-specific parts
        val secureStore = KVault(context, "polaris_secure_store")
        val protocolHandler = DefaultProtocolHandler(cryptoUtils)
        val keyStore = DefaultKeyStore(secureStore, cryptoUtils)
        val apiClient = config.apiConfig?.let { apiCfg ->
            KtorApiClient(secureStore, apiCfg)
        } ?: NoOpApiClient()

        // Create the holder object and store it.
        val instance = AndroidSdk(
            apiClient = apiClient,
            keyStore = keyStore,
            protocolHandler = protocolHandler,
            bleController = androidBleController,
        )
        this.sdkInstance = instance
        return instance
    }

    internal actual fun shutdown() {
        logger.info { "Shutting down Polaris SDK for Android..." }
        sdkInstance?.performShutdown()
        sdkInstance = null
    }
}