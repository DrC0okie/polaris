package ch.heig.iict.polaris_health

import android.app.Application
import ch.heig.iict.polaris_health.data.db.PolarisHealthDatabase
import ch.heig.iict.polaris_health.data.repository.PatientRepositoryImpl
import ch.heig.iict.polaris_health.data.repository.TokenRepositoryImpl
import ch.heig.iict.polaris_health.domain.repository.PatientRepository
import ch.heig.iict.polaris_health.domain.repository.TokenRepository
import kotlin.getValue

class PolarisHealthApplication : Application() {

    private val database: PolarisHealthDatabase by lazy {
        PolarisHealthDatabase.getDatabase(this)
    }

    val patientRepository: PatientRepository by lazy {
        PatientRepositoryImpl(database.patientDao())
    }

    val tokenRepository: TokenRepository by lazy {
        TokenRepositoryImpl(database.polTokenDao())
    }

    override fun onCreate() {
        super.onCreate()
    }
}