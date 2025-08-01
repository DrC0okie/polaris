package ch.heig.iict.polaris_health.data.repository

import ch.heig.iict.polaris_health.data.dao.PatientDao
import ch.heig.iict.polaris_health.data.model.PatientEntity
import ch.heig.iict.polaris_health.domain.model.PatientWithLockStatus
import ch.heig.iict.polaris_health.domain.repository.PatientRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.LocalDate



class PatientRepositoryImpl ( private val patientDao: PatientDao) : PatientRepository {

    private val lockStatusFlow = MutableStateFlow<Map<Int, Boolean>>(emptyMap())

    override fun getPatientsWithLockStatus(): Flow<List<PatientWithLockStatus>> {
        // combine() est un opérateur de Flow puissant : il écoute les deux flux (celui de la DB
        // et celui de l'état de verrouillage) et émet une nouvelle liste combinée dès que l'un ou l'autre change.
        return patientDao.getPatients().combine(lockStatusFlow) { patients, lockStatus ->
            patients.map { patient ->
                patient.toPatientWithLockStatus(
                    isLocked = lockStatus[patient.beaconId] ?: true // Par défaut, c'est verrouillé
                )
            }
        }
    }

    override suspend fun updateLockStatus(patientBeaconId: Int, isLocked: Boolean) {
        lockStatusFlow.value = lockStatusFlow.value.toMutableMap().apply {
            this[patientBeaconId] = isLocked
        }
    }

    override suspend fun seedWithDemoData() {
        if (patientDao.getPatients().first().isEmpty()) {
            val demoPatients = listOf(
                PatientEntity(1, 1, "Jean", "Heimart", LocalDate.parse("1951-01-01"), "male", "phoneNumber", "Address", "O+", "none", "none", 180, 82),
                PatientEntity(2, 2, "Alice", "Wonderland", LocalDate.parse("1952-02-02"), "female", "phoneNumber", "Address", "A-", "none", "none", 180, 82),
                PatientEntity(3, 3, "Laurent", "Barre", LocalDate.parse("1953-03-03"), "male", "phoneNumber", "Address", "AB+", "none", "none", 180, 82)
            )
            patientDao.insertAll(demoPatients)
        }
    }
}

private fun PatientEntity.toPatientWithLockStatus(isLocked: Boolean): PatientWithLockStatus {
    return PatientWithLockStatus(
        this.id!!,
        this.beaconId,
        this.firstName,
        this.lastName,
        this.dateOfBirth,
        this.gender,
        this.phoneNumber,
        this.address,
        this.bloodType,
        this.allergies,
        this.chronicConditions,
        this.heightCm,
        this.weightKg,
        isLocked
    )
}