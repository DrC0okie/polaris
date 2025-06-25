package ch.drcookie.polaris_sdk.api

import ch.drcookie.polaris_sdk.ble.AndroidBleController
import ch.drcookie.polaris_sdk.storage.SharedPreferencesProvider
import ch.drcookie.polaris_sdk.network.KtorApiClient
import ch.drcookie.polaris_sdk.storage.SharedPreferencesKeyStore
import ch.drcookie.polaris_sdk.protocol.DefaultProtocolHandler
import ch.drcookie.polaris_sdk.ble.util.BeaconDataParser
import ch.drcookie.polaris_sdk.crypto.CryptoUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import ch.drcookie.polaris_sdk.network.ApiClient
import ch.drcookie.polaris_sdk.ble.BleController
import ch.drcookie.polaris_sdk.network.KtorClientFactory
import ch.drcookie.polaris_sdk.storage.KeyStore
import ch.drcookie.polaris_sdk.storage.SdkPreferences
import ch.drcookie.polaris_sdk.protocol.ProtocolHandler
import com.ionspin.kotlin.crypto.LibsodiumInitializer

private val logger = KotlinLogging.logger {}

// Provide the 'actual' implementation for the SdkInitializer
actual class SdkInitializer {

    // This is a simple data class to hold our initialized repositories.
    // It implements the common interface.
    private class AndroidSdk(
        override val apiClient: ApiClient,
        override val keyStore: KeyStore,
        override val protocolHandler: ProtocolHandler,
        override val bleController: BleController,
    ) : PolarisDependencies {
        fun performShutdown() {
            bleController.cancelAll()
            KtorClientFactory.closeClient()
        }
    }

    private var sdkInstance: AndroidSdk? = null

    actual suspend fun initialize(context: PlatformContext): PolarisDependencies {
        logger.info { "Initializing Polaris SDK for Android..." }

        try {
            LibsodiumInitializer.initialize()
        } catch (e: Exception) {
            logger.error(e) { "FATAL: Libsodium initialization failed." }
            throw e // Re-throw to fail initialization
        }

        val cryptoManager = CryptoUtils
        val beaconDataParser = BeaconDataParser
        val sdkPreferences: SdkPreferences = SharedPreferencesProvider(context)

        val ktorApiClient = KtorApiClient(KtorClientFactory, sdkPreferences)
        val sharedPreferencesKeyStore = SharedPreferencesKeyStore(sdkPreferences, cryptoManager)
        val defaultProtocolHandler = DefaultProtocolHandler(cryptoManager)
        val androidBleController = AndroidBleController(context, beaconDataParser)

        // Create the holder object and store it.
        val instance = AndroidSdk(
            apiClient = ktorApiClient,
            keyStore = sharedPreferencesKeyStore,
            protocolHandler = defaultProtocolHandler,
            bleController = androidBleController,
        )
        this.sdkInstance = instance
        return instance
    }

    actual fun shutdown() {
        logger.info { "Shutting down Polaris SDK for Android..." }
        sdkInstance?.performShutdown()
        sdkInstance = null
    }
}