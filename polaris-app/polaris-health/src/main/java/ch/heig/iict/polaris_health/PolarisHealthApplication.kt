package ch.heig.iict.polaris_health

import android.app.Application
import ch.heig.iict.polaris_health.data.db.PolarisHealthDatabase
import ch.heig.iict.polaris_health.data.repositories.TokenRepositoryImpl
import ch.heig.iict.polaris_health.data.repositories.VisitRepositoryImpl
import ch.heig.iict.polaris_health.domain.repositories.VisitRepository
import ch.heig.iict.polaris_health.domain.repositories.TokenRepository
import kotlin.getValue

class PolarisHealthApplication : Application() {

    private val database: PolarisHealthDatabase by lazy {
        PolarisHealthDatabase.getDatabase(this)
    }

    val visitRepository: VisitRepository by lazy {
        VisitRepositoryImpl(database.visitDao(), database.patientDao(), database.beaconDao())
    }

    val tokenRepository: TokenRepository by lazy {
        TokenRepositoryImpl(database.polTokenDao())
    }

    override fun onCreate() {
        super.onCreate()
    }
}