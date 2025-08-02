package ch.heig.iict.polaris_health.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "medical_records",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patient_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MedicalRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "patient_id", index = true)
    val patientId: Long,

    val bloodType: String?,
    val allergies: String?,
    val chronicConditions: String?,
    val heightCm: Int?,
    val weightKg: Int?
)
