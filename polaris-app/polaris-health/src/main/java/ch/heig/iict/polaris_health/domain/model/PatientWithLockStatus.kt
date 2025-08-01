package ch.heig.iict.polaris_health.domain.model

import java.time.LocalDate

data class PatientWithLockStatus(
    val id: Long,
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
    val isLocked: Boolean = true
)