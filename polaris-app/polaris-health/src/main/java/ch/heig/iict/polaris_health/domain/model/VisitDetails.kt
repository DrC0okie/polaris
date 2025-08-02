package ch.heig.iict.polaris_health.domain.model

data class VisitDetails(
    val visitId: Long,
    val patientId: Long,
    val patientFirstName: String,
    val patientLastName: String,
    val patientFullName: String = "$patientFirstName $patientLastName",
    val associatedBeaconId: Int?,
    val isLocked: Boolean = true
)