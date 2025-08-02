package ch.heig.iict.polaris_health.data.repositories

import ch.heig.iict.polaris_health.data.dao.BeaconDao
import ch.heig.iict.polaris_health.data.dao.PatientDao
import ch.heig.iict.polaris_health.data.dao.VisitDao
import ch.heig.iict.polaris_health.data.dao.VisitWithPatient
import ch.heig.iict.polaris_health.data.entities.BeaconEntity
import ch.heig.iict.polaris_health.data.entities.PatientEntity
import ch.heig.iict.polaris_health.data.entities.VisitEntity
import ch.heig.iict.polaris_health.domain.model.VisitDetails
import ch.heig.iict.polaris_health.domain.repositories.VisitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class VisitRepositoryImpl(
    private val visitDao: VisitDao,
    private val patientDao: PatientDao,
    private val beaconDao: BeaconDao
) : VisitRepository {

    private val lockStatusFlow = MutableStateFlow<Map<Int, Boolean>>(emptyMap())

    override fun getTodaysVisits(): Flow<List<VisitDetails>> {
        val today = LocalDate.now()

        return combine(
            visitDao.getVisitsForDate(today),
            beaconDao.getAllBeacons(),
            lockStatusFlow
        ) { visits, beacons, lockStatus ->
            val beaconMap = beacons.associateBy { it.patientId }

            visits.map { visitWithPatient ->
                val associatedBeacon = beaconMap[visitWithPatient.patient.id]
                visitWithPatient.toVisitDetails(
                    associatedBeaconId = associatedBeacon?.beaconId,
                    isLocked = lockStatus[associatedBeacon?.beaconId] ?: true
                )
            }
        }
    }

    override suspend fun updateLockStatusForBeacon(beaconId: Int, isLocked: Boolean) {
        lockStatusFlow.value = lockStatusFlow.value.toMutableMap().apply {
            this[beaconId] = isLocked
        }
    }

    override suspend fun seedWithDemoData() {
        if (visitDao.getVisitsForDate(LocalDate.now()).first().isEmpty()) {

            val p1 = PatientEntity(1, "Jean", "Dupont", LocalDate.parse("1951-01-01"), "male", "address", "phoneNumber")
            val p2 = PatientEntity(2, "Marie", "Curie", LocalDate.parse("1952-02-02"), "female", "address", "phoneNumber")
            patientDao.insertPatient(p1)
            patientDao.insertPatient(p2)

            val b1 = BeaconEntity(1, 1, "ACTIVE")
            val b2 = BeaconEntity(2, 2, "ACTIVE")
            beaconDao.insertBeacon(b1)
            beaconDao.insertBeacon(b2)

            val v1 = VisitEntity(patientId = 1, visitDate = LocalDate.now(), status = "PLANNED", notes = "Contrôle tension")
            val v2 = VisitEntity(patientId = 2, visitDate = LocalDate.now(), status = "PLANNED", notes = "Changer pansement")
            visitDao.insertVisit(v1)
            visitDao.insertVisit(v2)
        }
    }
}

private fun VisitWithPatient.toVisitDetails(associatedBeaconId: Int?, isLocked: Boolean): VisitDetails {
    return VisitDetails(
        visitId = this.visit.id,
        patientId = this.patient.id,
        patientFirstName = this.patient.firstName,
        patientLastName = this.patient.lastName,
        associatedBeaconId = associatedBeaconId,
        isLocked = isLocked
    )
}