package ch.drcookie.polaris_sdk.api

import ch.drcookie.polaris_sdk.network.ApiClient
import ch.drcookie.polaris_sdk.ble.BleController
import ch.drcookie.polaris_sdk.storage.KeyStore
import ch.drcookie.polaris_sdk.protocol.ProtocolHandler

public object Polaris : PolarisDependencies {

    public override val apiClient: ApiClient
        get() = internalSdkInstance?.apiClient ?: error("PolarisSdk not initialized. Call initialize() first.")
    public override val keyStore: KeyStore
        get() = internalSdkInstance?.keyStore ?: error("PolarisSdk not initialized.")
    public override val protocolHandler: ProtocolHandler
        get() = internalSdkInstance?.protocolHandler ?: error("PolarisSdk not initialized.")
    public override val bleController: BleController
        get() = internalSdkInstance?.bleController ?: error("PolarisSdk not initialized.")

    private var internalSdkInstance: PolarisDependencies? = null
    private var isInitialized = false

    private val initializer = SdkInitializer()

    public suspend fun initialize(context: PlatformContext) {
        if (isInitialized) return

        // The platform-specific code will create and return an instance that fulfills the PolarisApi contract.
        internalSdkInstance = initializer.initialize(context)
        isInitialized = true
    }

    public fun shutdown() {
        if (!isInitialized) return
        initializer.shutdown()
        internalSdkInstance = null
        isInitialized = false
    }
}