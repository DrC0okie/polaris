package ch.heig.iict.polaris_health.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.NO_ACTION
import androidx.room.PrimaryKey

@Entity(
    tableName = "pol_tokens",
    foreignKeys = [
        ForeignKey(PatientEntity::class,["id"],["patient_id"],ForeignKey.CASCADE, NO_ACTION, false)
    ]
)
data class PoLTokenEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "patient_id", index = true)
    val patientId: Long,

    val flags: Byte,
    val phoneId: Long,
    val beaconId: Int,
    val beaconCounter: Long,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val nonce: ByteArray,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val phonePk: ByteArray,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val beaconPk: ByteArray,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val phoneSig: ByteArray,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val beaconSig: ByteArray,

    @ColumnInfo(name = "is_synced", defaultValue = "0")
    var isSynced: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PoLTokenEntity

        if (id != other.id) return false
        if (patientId != other.patientId) return false
        if (flags != other.flags) return false
        if (phoneId != other.phoneId) return false
        if (beaconId != other.beaconId) return false
        if (beaconCounter != other.beaconCounter) return false
        if (isSynced != other.isSynced) return false
        if (!nonce.contentEquals(other.nonce)) return false
        if (!phonePk.contentEquals(other.phonePk)) return false
        if (!beaconPk.contentEquals(other.beaconPk)) return false
        if (!phoneSig.contentEquals(other.phoneSig)) return false
        if (!beaconSig.contentEquals(other.beaconSig)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + patientId.hashCode()
        result = 31 * result + flags
        result = 31 * result + phoneId.hashCode()
        result = 31 * result + beaconId
        result = 31 * result + beaconCounter.hashCode()
        result = 31 * result + isSynced.hashCode()
        result = 31 * result + nonce.contentHashCode()
        result = 31 * result + phonePk.contentHashCode()
        result = 31 * result + beaconPk.contentHashCode()
        result = 31 * result + phoneSig.contentHashCode()
        result = 31 * result + beaconSig.contentHashCode()
        return result
    }

}
