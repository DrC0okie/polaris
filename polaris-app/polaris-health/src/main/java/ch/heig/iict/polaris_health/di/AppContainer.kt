package ch.heig.iict.polaris_health.di

import android.content.Context
import android.os.Build
import android.util.Log
import ch.drcookie.polaris_sdk.api.Polaris
import ch.drcookie.polaris_sdk.api.Polaris.bleController
import ch.drcookie.polaris_sdk.api.Polaris.keyStore
import ch.drcookie.polaris_sdk.api.Polaris.networkClient
import ch.drcookie.polaris_sdk.api.Polaris.protocolHandler
import ch.drcookie.polaris_sdk.api.SdkResult
import ch.drcookie.polaris_sdk.api.config.AuthMode
import ch.drcookie.polaris_sdk.api.message
import ch.drcookie.polaris_sdk.api.use_case.*
import ch.drcookie.polaris_sdk.ble.BleController
import ch.heig.iict.polaris_health.data.db.PolarisHealthDatabase
import ch.heig.iict.polaris_health.data.repositories.TokenRepositoryImpl
import ch.heig.iict.polaris_health.data.repositories.VisitRepositoryImpl
import ch.heig.iict.polaris_health.domain.repositories.TokenRepository
import ch.heig.iict.polaris_health.domain.repositories.VisitRepository

object AppContainer {

    private var initialized = false

    lateinit var visitRepository: VisitRepository
        private set

    lateinit var tokenRepository: TokenRepository
        private set

    lateinit var scanForBeacon: ScanForBeacon
        private set

    lateinit var monitorBroadcasts: MonitorBroadcasts
        private set

    lateinit var polTransaction: PolTransaction
        private set

    lateinit var deliverPayload: DeliverPayload
        private set

    lateinit var pullAndForward: PullAndForward
        private set

    lateinit var fetchBeacons: FetchBeacons
        private set

    lateinit var sdkBleController: BleController
        private set

    suspend fun init(context: Context) {
        if (initialized) return

        Polaris.initialize(context.applicationContext) {
            network {
                baseUrl = "https://polaris.iict-heig-vd.ch"
                authMode = AuthMode.ManagedApiKey
            }
        }

        fetchBeacons = FetchBeacons(networkClient, keyStore)

        val deviceModel = Build.MODEL
        val osVersion = Build.VERSION.RELEASE

        when (val result = fetchBeacons(deviceModel, osVersion, "1.0.1")) {
            is SdkResult.Success -> { /* On success, continue */ }
            is SdkResult.Failure -> Log.e (this.javaClass.simpleName, result.error.message())
        }

        val db = PolarisHealthDatabase.getDatabase(context.applicationContext)

        visitRepository = VisitRepositoryImpl(db.visitDao(), db.patientDao(), db.beaconDao())
        tokenRepository = TokenRepositoryImpl(db.polTokenDao(), networkClient)
        scanForBeacon = ScanForBeacon(bleController, networkClient)
        polTransaction = PolTransaction(bleController, networkClient, keyStore, protocolHandler)
        monitorBroadcasts = MonitorBroadcasts(bleController, networkClient, protocolHandler)
        deliverPayload = DeliverPayload(bleController, networkClient, scanForBeacon)
        pullAndForward = PullAndForward(bleController, networkClient)
        sdkBleController = bleController

        visitRepository.seedWithDemoData()
        initialized = true
    }
}
