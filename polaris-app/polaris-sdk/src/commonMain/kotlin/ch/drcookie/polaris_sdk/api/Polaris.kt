package ch.drcookie.polaris_sdk.api

import ch.drcookie.polaris_sdk.network.ApiClient
import ch.drcookie.polaris_sdk.ble.BleController
import ch.drcookie.polaris_sdk.storage.KeyStore
import ch.drcookie.polaris_sdk.protocol.ProtocolHandler
import kotlinx.atomicfu.atomic

public object Polaris : PolarisDependencies {

    private lateinit var internalSdkInstance: PolarisDependencies

    // A flag to ensure initialize is called only once.
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
     * Initializes the SDK. This suspend function MUST be called and awaited
     * in the Application's onCreate before any other part of the SDK is accessed.
     */
    public suspend fun initialize(context: PlatformContext) {
        if (isInitialized.getAndSet(true)) {
            // Already initialized or in the process.
            return
        }
        internalSdkInstance = initializer.initialize(context)
    }

    /**
     * Gets the instance, which throws if not initialized.
     */
    private fun getInstance(): PolarisDependencies {
        if (!::internalSdkInstance.isInitialized) {
            error("PolarisSdk not initialized. Call and await Polaris.initialize() in your Application.onCreate() first.")
        }
        return internalSdkInstance
    }

    public fun shutdown() {
        if (isInitialized.getAndSet(false)) {
            if (::internalSdkInstance.isInitialized) {
                initializer.shutdown()
            }
        }
    }
}