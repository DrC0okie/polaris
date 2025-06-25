package ch.drcookie.polaris_sdk.api

actual class SdkInitializer {

    actual suspend fun initialize(context: PlatformContext): PolarisDependencies {
        throw NotImplementedError("iOS Initializer has not been implemented yet.")
    }

    actual fun shutdown() {    }
}