package ch.drcookie.polaris_app.ui

import android.app.Application
import ch.drcookie.polaris_sdk.api.Polaris
import ch.drcookie.polaris_sdk.network.DynamicApiKeyProvider
import kotlinx.coroutines.runBlocking

class PolarisApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Block the main thread to ensure SDK is ready before the app continues.
        runBlocking {
            Polaris.initialize(applicationContext) {
                ble {
                }
                api {
                    baseUrl = "https://polaris.iict-heig-vd.ch"
                    authInterceptor = DynamicApiKeyProvider()
                }
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        Polaris.shutdown()
    }
}