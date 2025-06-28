package ch.drcookie.polaris_sdk.api

import ch.drcookie.polaris_sdk.api.config.PolarisConfig
import ch.drcookie.polaris_sdk.network.NetworkClient
import ch.drcookie.polaris_sdk.ble.BleController
import ch.drcookie.polaris_sdk.storage.KeyStore
import ch.drcookie.polaris_sdk.protocol.ProtocolHandler
import ch.drcookie.polaris_sdk.network.NoOpNetworkClient
import kotlinx.atomicfu.atomic

/**
 * The main entry point for the Polaris SDK.
 *
 * This singleton object provides access to all core components of the SDK after initialization.
 * It must be initialized once, typically in your application's `onCreate` method, before any
 * of its properties or methods are accessed.
 *
 * ### Example Usage:
 * ```kotlin
 * // In your Application class
 * class MyApp : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         runBlocking { // Or use a proper CoroutineScope
 *             Polaris.initialize(applicationContext) {
 *                 // Optional configuration
 *                 api {
 *                     baseUrl = "https://my.server.com"
 *                     authMode = AuthMode.ManagedApiKey
 *                 }
 *             }
 *         }
 *     }
 * }
 *
 * // Later, in a ViewModel or other class
 * val bleController = Polaris.bleController
 * val networkClient = Polaris.networkClient
 * ```
 */
public object Polaris : PolarisAPI {

    private lateinit var _sdkInstance: PolarisAPI
    private val isInitialized = atomic(false)
    private val initializer = SdkInitializer()

    /**
     * The network client for communicating with a Polaris-compatible backend server.
     * Accessing this property before [initialize] has been called will throw an [IllegalStateException].
     * If the `api` block was not configured during initialization, this will be a [NoOpNetworkClient]
     * that returns a [SdkError.PreconditionError] for all network calls.
     */
    public override val networkClient: NetworkClient
        get() = getInstance().networkClient

    /**
     * The secure storage handler for cryptographic keys.
     * Accessing this property before [initialize] has been called will throw an [IllegalStateException].
     */
    public override val keyStore: KeyStore
        get() = getInstance().keyStore

    /**
     * The handler for low-level cryptographic protocol logic, such as signing and verification.
     * Accessing this property before [initialize] has been called will throw an [IllegalStateException].
     */
    public override val protocolHandler: ProtocolHandler
        get() = getInstance().protocolHandler

    /**
     * The controller for all Bluetooth Low Energy (BLE) operations, including scanning and connecting.
     * Accessing this property before [initialize] has been called will throw an [IllegalStateException].
     */
    public override val bleController: BleController
        get() = getInstance().bleController

    /**
     * Initializes the Polaris SDK with optional custom configuration.
     *
     * Must be called once from a coroutine before any other SDK functionality is used.
     * It is safe to call this multiple times; subsequent calls will be ignored.
     * The ideal place to call this is from your application's `onCreate` method.
     *
     * @param context The platform-specific context (e.g., `android.content.Context` on Android).
     * @param configuration A lambda block with a [PolarisConfig] receiver for setting up the SDK.
     *                      Use this to configure BLE, network, and authentication settings.
     */
    public suspend fun initialize(
        context: PlatformContext,
        configuration: PolarisConfig.() -> Unit = {},
    ) {
        // Prevent re-initialization
        if (isInitialized.getAndSet(true)) return

        // Create a default config object and then apply the user's modifications from the lambda.
        val config = PolarisConfig().apply(configuration)

        // Pass the final config to the internal initializer.
        _sdkInstance = initializer.initialize(context, config)
    }

    /**
     * Shuts down the SDK and releases all resources.
     *
     * This includes stopping any active BLE scans or connections and closing the network client.
     * It is recommended to call this in your application's `onTerminate` method, though it is not strictly required
     */
    public fun shutdown() {
        if (isInitialized.getAndSet(false)) {
            if (::_sdkInstance.isInitialized) {
                initializer.shutdown()
            }
        }
    }

    private fun getInstance(): PolarisAPI {
        if (!::_sdkInstance.isInitialized) {
            error("PolarisSdk not initialized. Call and await Polaris.initialize() in your Application.onCreate() first.")
        }
        return _sdkInstance
    }
}