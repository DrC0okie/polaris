package ch.drcookie.polaris_sdk.api

import ch.drcookie.polaris_sdk.api.config.PolarisConfig
import ch.drcookie.polaris_sdk.ble.AndroidBleController
import ch.drcookie.polaris_sdk.network.KtorNetworkClient
import ch.drcookie.polaris_sdk.storage.DefaultKeyStore
import ch.drcookie.polaris_sdk.protocol.DefaultProtocolHandler
import ch.drcookie.polaris_sdk.ble.util.BeaconDataParser
import ch.drcookie.polaris_sdk.crypto.CryptoUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import ch.drcookie.polaris_sdk.network.NetworkClient
import ch.drcookie.polaris_sdk.ble.BleController
import ch.drcookie.polaris_sdk.network.NoOpNetworkClient
import ch.drcookie.polaris_sdk.storage.KeyStore
import ch.drcookie.polaris_sdk.protocol.ProtocolHandler
import com.ionspin.kotlin.crypto.LibsodiumInitializer
import com.liftric.kvault.KVault

private val logger = KotlinLogging.logger {}

private object AndroidLoggingInitializer {
    init {
        System.setProperty("kotlin-logging-to-android-native", "true")
    }
}

/**
 * Android implementation of the [SdkInitializer].
 */
internal actual class SdkInitializer {

    /** Sets up the native Android logger for kotlin-logging. */
    init {
        AndroidLoggingInitializer
    }

    /** Holds the SDK components for Android. */
    private class AndroidSdk(
        override val networkClient: NetworkClient,
        override val keyStore: KeyStore,
        override val protocolHandler: ProtocolHandler,
        override val bleController: BleController,
    ) : PolarisAPI {
        fun performShutdown() {
            bleController.cancelAll()
            networkClient.closeClient()
        }
    }

    private var sdkInstance: AndroidSdk? = null

    /**  The platform-specific initialization logic for Android. */
    internal actual suspend fun initialize(context: PlatformContext, config: PolarisConfig): PolarisAPI {
        logger.info { "Initializing Polaris SDK for Android..." }

        try {
            LibsodiumInitializer.initialize()
        } catch (e: Exception) {
            logger.error(e) { "FATAL: Libsodium initialization failed." }
            throw e // Re-throw to fail the application's startup, as this is unrecoverable.
        }

        // Instantiate common, stateless utility classes.
        val cryptoUtils = CryptoUtils
        val beaconDataParser = BeaconDataParser

        // Instantiate platform-specific components using the provided Android context.
        val androidBleController: BleController = AndroidBleController(context, beaconDataParser, config.bleConfig)

        // Instantiate common components, injecting their dependencies.
        val secureStore = KVault(context, "polaris_secure_store")
        val protocolHandler = DefaultProtocolHandler(cryptoUtils)
        val keyStore = DefaultKeyStore(secureStore, cryptoUtils)
        val networkClient = config.networkConfig?.let { apiCfg ->
            KtorNetworkClient(secureStore, apiCfg)
        } ?: NoOpNetworkClient()
        val instance = AndroidSdk(networkClient, keyStore, protocolHandler, androidBleController)

        this.sdkInstance = instance
        return instance
    }

    internal actual fun shutdown() {
        logger.info { "Shutting down Polaris SDK for Android..." }
        sdkInstance?.performShutdown()
        sdkInstance = null
    }
}