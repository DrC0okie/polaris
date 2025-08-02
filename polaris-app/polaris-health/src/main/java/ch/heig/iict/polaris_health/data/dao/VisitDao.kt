package ch.heig.iict.polaris_health.data.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import ch.heig.iict.polaris_health.data.entities.PatientEntity
import ch.heig.iict.polaris_health.data.entities.VisitEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate


data class VisitWithPatient(
    @Embedded val visit: VisitEntity,

    @Relation(
        parentColumn = "patient_id",
        entityColumn = "id"
    )
    val patient: PatientEntity
)

@Dao
interface VisitDao {
    @Transaction
    @Query("SELECT * FROM visits WHERE visitDate = :date ORDER BY patient_id")
    fun getVisitsForDate(date: LocalDate): Flow<List<VisitWithPatient>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: VisitEntity)
}