package ch.heig.iict.polaris_health.domain.repository

import ch.heig.iict.polaris_health.domain.model.PatientWithLockStatus
import kotlinx.coroutines.flow.Flow

interface PatientRepository {

    fun getPatientsWithLockStatus(): Flow<List<PatientWithLockStatus>>
    suspend fun updateLockStatus(patientBeaconId: Int, isLocked: Boolean)
    suspend fun seedWithDemoData()
}