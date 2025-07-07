package ch.drcookie.polaris_app.ui

import android.app.Application
import ch.drcookie.polaris_sdk.api.Polaris
import ch.drcookie.polaris_sdk.api.config.AuthMode
import kotlinx.coroutines.runBlocking

/**
 * Custom Application class for the Polaris sample app.
 *
 * This class serves as the central entry point for the application process.
 * In a larger application, it would be used to initialize app-wide components
 * like dependency injection frameworks, logging libraries, etc.
 */
class PolarisApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}