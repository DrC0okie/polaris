package ch.drcookie.polaris_app.ui

import android.app.Application
import ch.drcookie.polaris_sdk.api.Polaris
import ch.drcookie.polaris_sdk.api.config.AuthMode
import kotlinx.coroutines.runBlocking

/**
 * Initialize the [Polaris] SDK when the application is created.
 * This ensures that all SDK components are ready before any Activity or ViewModel attempts to use them.
 */
class PolarisApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Block the main thread to ensure SDK is ready before the app continues.
        runBlocking {
            Polaris.initialize(applicationContext) {
                ble {
                    polServiceUuid = "f44dce36-ffb2-565b-8494-25fa5a7a7cd6"
                    tokenWriteUuid = "8e8c14b7-d9f0-5e5c-9da8-6961e1f33d6b"
                    tokenIndicateUuid = "d234a7d8-ea1f-5299-8221-9cf2f942d3df"
                    encryptedWriteUuid = "8ed72380-5adb-4d2d-81fb-ae6610122ee8"
                    encryptedIndicateUuid = "079b34dd-2310-4b61-89bb-494cc67e097f"
                    pullDataWriteUuid = "e914a8e4-843a-4b72-8f2a-f9175d71cf88"
                    manufacturerId = 0xFFFF
                    mtu = 23
                }
                api {
                    baseUrl = "https://polaris.iict-heig-vd.ch" // mandatory
                    authMode = AuthMode.ManagedApiKey
                    registrationPath = "/api/v1/register"
                    beaconsPath = "/api/v1/beacons"
                    tokensPath = "/api/v1/tokens"
                    fetchPayloadsPath = "/api/v1/payloads"
                    forwardPayloadPath = "/api/v1/payloads"
                    ackPath = "/api/v1/payloads/ack"
                }
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        Polaris.shutdown()
    }
}