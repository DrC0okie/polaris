package ch.drcookie.polaris_app.ui

import android.app.Application
import ch.drcookie.polaris_sdk.api.Polaris
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PolarisApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // The single, simple initialization call for the entire SDK.
        applicationScope.launch {
            Polaris.initialize(applicationContext)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        Polaris.shutdown()
    }
}