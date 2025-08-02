package ch.heig.iict.polaris_health.data.dao

import androidx.room.*
import ch.heig.iict.polaris_health.data.entities.BeaconEntity
import ch.heig.iict.polaris_health.data.entities.MedicalRecordEntity
import ch.heig.iict.polaris_health.data.entities.PatientEntity

@Dao
interface PatientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: PatientEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicalRecord(record: MedicalRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeacon(beacon: BeaconEntity)
}