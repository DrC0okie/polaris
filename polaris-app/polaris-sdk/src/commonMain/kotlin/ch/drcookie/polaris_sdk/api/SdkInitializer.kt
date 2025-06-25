package ch.drcookie.polaris_sdk.api

internal expect class SdkInitializer() {
    internal suspend fun initialize(context: PlatformContext): PolarisDependencies
    internal fun shutdown()
}