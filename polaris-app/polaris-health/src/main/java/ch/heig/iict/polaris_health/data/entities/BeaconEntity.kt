package ch.heig.iict.polaris_health.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "beacons",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patient_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class BeaconEntity(
    @PrimaryKey
    val beaconId: Int,

    @ColumnInfo(name = "patient_id", index = true)
    val patientId: Long?,

    val status: String
)
