package ch.heig.iict.polaris_health.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null,

    @ColumnInfo(name = "beacon_id", index = true)
    val beaconId: Int,

    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val gender: String,
    val phoneNumber: String?,
    val address: String?,
    val bloodType: String?,
    val allergies: String?,
    val chronicConditions: String?,
    val heightCm: Int?,
    val weightKg: Int?,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
