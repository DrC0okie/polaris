package ch.drcookie.polaris_sdk.api

import ch.drcookie.polaris_sdk.api.config.PolarisConfig

internal actual class SdkInitializer {

    internal actual suspend fun initialize(context: PlatformContext, config: PolarisConfig): PolarisDependencies {
        throw NotImplementedError("iOS Initializer has not been implemented yet.")
    }

    internal actual fun shutdown() {    }
}