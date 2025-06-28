package ch.drcookie.polaris_sdk.api

import ch.drcookie.polaris_sdk.api.config.PolarisConfig

internal expect class SdkInitializer() {
    internal suspend fun initialize(context: PlatformContext, config: PolarisConfig): PolarisAPI
    internal fun shutdown()
}