package ch.drcookie.polaris_app.ui

import android.app.Application
import ch.drcookie.polaris_sdk.api.Polaris
import kotlinx.coroutines.runBlocking

class PolarisApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Block the main thread to ensure SDK is ready before the app continues.
        runBlocking {
            Polaris.initialize(applicationContext)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        Polaris.shutdown()
    }
}