package ch.heigvd.iict.dto.admin

import java.time.Instant

/**
 * A DTO for displaying PoL token records in the admin dashboard.
 */
data class TokenAdminDto(
    val id: Long?,
    val phoneId: Long?,
    val beaconId: Int,
    val beaconCounter: Long,
    val nonceHex: String,
    val isValid: Boolean,
    val validationError: String?,
    val receivedAt: Instant
)