package ch.drcookie.polaris_sdk

import ch.drcookie.polaris_sdk.domain.repository.AuthRepository
import ch.drcookie.polaris_sdk.domain.repository.BleDataSource
import ch.drcookie.polaris_sdk.domain.repository.KeyRepository
import ch.drcookie.polaris_sdk.domain.repository.ProtocolRepository

object Polaris : PolarisApi {

    override val authRepository: AuthRepository
        get() = internalSdkInstance?.authRepository ?: error("PolarisSdk not initialized. Call initialize() first.")
    override val keyRepository: KeyRepository
        get() = internalSdkInstance?.keyRepository ?: error("PolarisSdk not initialized.")
    override val protocolRepository: ProtocolRepository
        get() = internalSdkInstance?.protocolRepository ?: error("PolarisSdk not initialized.")
    override val bleDataSource: BleDataSource
        get() = internalSdkInstance?.bleDataSource ?: error("PolarisSdk not initialized.")

    private var internalSdkInstance: PolarisApi? = null
    private var isInitialized = false

    private val initializer = SdkInitializer()

    suspend fun initialize(context: PlatformContext) {
        if (isInitialized) return

        // The platform-specific code will create and return an instance that fulfills the PolarisApi contract.
        internalSdkInstance = initializer.initialize(context)
        isInitialized = true
    }

    fun shutdown() {
        if (!isInitialized) return
        initializer.shutdown()
        internalSdkInstance = null
        isInitialized = false
    }
}

internal expect class SdkInitializer() {
    suspend fun initialize(context: PlatformContext): PolarisApi
    fun shutdown()
}