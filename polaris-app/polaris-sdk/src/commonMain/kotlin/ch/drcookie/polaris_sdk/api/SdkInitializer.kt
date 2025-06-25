package ch.drcookie.polaris_sdk.api

internal expect class SdkInitializer() {
    suspend fun initialize(context: PlatformContext): PolarisDependencies
    fun shutdown()
}