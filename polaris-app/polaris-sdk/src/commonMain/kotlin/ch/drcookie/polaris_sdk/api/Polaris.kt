package ch.drcookie.polaris_sdk.api

import ch.drcookie.polaris_sdk.api.config.PolarisConfig
import ch.drcookie.polaris_sdk.network.ApiClient
import ch.drcookie.polaris_sdk.ble.BleController
import ch.drcookie.polaris_sdk.storage.KeyStore
import ch.drcookie.polaris_sdk.protocol.ProtocolHandler
import kotlinx.atomicfu.atomic

public object Polaris : PolarisDependencies {

    private lateinit var _sdkInstance: PolarisDependencies
    private val isInitialized = atomic(false)
    private val initializer = SdkInitializer()

    // Properties access the initialized instance directly.
    public override val apiClient: ApiClient
        get() = getInstance().apiClient
    public override val keyStore: KeyStore
        get() = getInstance().keyStore
    public override val protocolHandler: ProtocolHandler
        get() = getInstance().protocolHandler
    public override val bleController: BleController
        get() = getInstance().bleController

    /**
     * Initializes the Polaris SDK with optional custom configuration.
     * This suspend function must be called and awaited before any other SDK functionality is used.
     *
     * @param context The platform-specific context (e.g., Android's ApplicationContext).
     * @param configuration A lambda block to configure the SDK.
     */
    public suspend fun initialize(
        context: PlatformContext,
        configuration: PolarisConfig.() -> Unit = {}
    ) {
        // Prevent re-initialization
        if (isInitialized.getAndSet(true)) return

        // Create a default config object and then apply the user's modifications from the lambda.
        val config = PolarisConfig().apply(configuration)

        // Pass the final config to the internal initializer.
        _sdkInstance = initializer.initialize(context, config)
    }

    /**
     * Gets the instance, which throws if not initialized.
     */
    private fun getInstance(): PolarisDependencies {
        if (!::_sdkInstance.isInitialized) {
            error("PolarisSdk not initialized. Call and await Polaris.initialize() in your Application.onCreate() first.")
        }
        return _sdkInstance
    }

    public fun shutdown() {
        if (isInitialized.getAndSet(false)) {
            if (::_sdkInstance.isInitialized) {
                initializer.shutdown()
            }
        }
    }
}