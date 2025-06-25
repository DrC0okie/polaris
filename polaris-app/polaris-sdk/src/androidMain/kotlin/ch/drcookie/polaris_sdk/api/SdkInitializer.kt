package ch.drcookie.polaris_sdk.api

import ch.drcookie.polaris_sdk.api.config.PolarisConfig
import ch.drcookie.polaris_sdk.ble.AndroidBleController
import ch.drcookie.polaris_sdk.storage.SharedPreferencesProvider
import ch.drcookie.polaris_sdk.network.KtorApiClient
import ch.drcookie.polaris_sdk.storage.DefaultKeyStore
import ch.drcookie.polaris_sdk.protocol.DefaultProtocolHandler
import ch.drcookie.polaris_sdk.ble.util.BeaconDataParser
import ch.drcookie.polaris_sdk.crypto.CryptoUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import ch.drcookie.polaris_sdk.network.ApiClient
import ch.drcookie.polaris_sdk.ble.BleController
import ch.drcookie.polaris_sdk.network.DynamicApiKeyProvider
import ch.drcookie.polaris_sdk.storage.KeyStore
import ch.drcookie.polaris_sdk.storage.SdkPreferences
import ch.drcookie.polaris_sdk.protocol.ProtocolHandler
import com.ionspin.kotlin.crypto.LibsodiumInitializer

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
        val sdkPreferences: SdkPreferences = SharedPreferencesProvider(context)
        val androidBleController: BleController = AndroidBleController(context, beaconDataParser, config.bleConfig)

        val apiConfig = config.apiConfig
        // Check if the user provided a dynamic provider.
        if (apiConfig?.authInterceptor is DynamicApiKeyProvider) {
            apiConfig.authInterceptor.preferences = sdkPreferences
        }

        // Common implementations, injecting the platform-specific parts
        val protocolHandler = DefaultProtocolHandler(cryptoUtils)
        val keyStore = DefaultKeyStore(sdkPreferences, cryptoUtils)
        val apiClient = KtorApiClient(sdkPreferences, config.apiConfig)

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