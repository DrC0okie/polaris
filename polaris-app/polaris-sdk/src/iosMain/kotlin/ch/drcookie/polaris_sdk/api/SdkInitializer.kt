package ch.drcookie.polaris_sdk.api

internal actual class SdkInitializer {

    internal actual suspend fun initialize(context: PlatformContext): PolarisDependencies {
        throw NotImplementedError("iOS Initializer has not been implemented yet.")
    }

    internal actual fun shutdown() {    }
}