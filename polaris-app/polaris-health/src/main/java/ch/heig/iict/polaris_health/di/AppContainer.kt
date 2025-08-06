package ch.heig.iict.polaris_health.di

import android.content.Context
import ch.drcookie.polaris_sdk.api.Polaris
import ch.drcookie.polaris_sdk.api.config.AuthMode
import ch.drcookie.polaris_sdk.api.use_case.*
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

    suspend fun init(context: Context) {
        if (initialized) return

        Polaris.initialize(context.applicationContext) {
            network {
                baseUrl = "https://polaris.iict-heig-vd.ch"
                authMode = AuthMode.ManagedApiKey
            }
        }

        val db = PolarisHealthDatabase.getDatabase(context.applicationContext)

        visitRepository = VisitRepositoryImpl(
            db.visitDao(),
            db.patientDao(),
            db.beaconDao()
        )

        tokenRepository = TokenRepositoryImpl(
            db.polTokenDao(),
            Polaris.networkClient
        )

        scanForBeacon = ScanForBeacon(
            Polaris.bleController,
            Polaris.networkClient
        )

        polTransaction = PolTransaction(
            Polaris.bleController,
            Polaris.networkClient,
            Polaris.keyStore,
            Polaris.protocolHandler
        )

        monitorBroadcasts = MonitorBroadcasts(
            Polaris.bleController,
            Polaris.networkClient,
            Polaris.protocolHandler
        )

        visitRepository.seedWithDemoData()

        initialized = true
    }
}
