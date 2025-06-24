package ch.drcookie.polaris_sdk

actual class SdkInitializer {

    actual suspend fun initialize(context: PlatformContext): PolarisApi {
        throw NotImplementedError("iOS Initializer has not been implemented yet.")
    }

    actual fun shutdown() {    }
}