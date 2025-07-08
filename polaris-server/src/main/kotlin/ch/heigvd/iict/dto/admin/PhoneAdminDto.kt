package ch.heigvd.iict.dto.admin

import java.time.Instant

/**
 * A DTO for displaying registered phone information in the admin dashboard.
 */
data class PhoneAdminDto(
    val id: Long?,
    val publicKeyHex: String,
    val apiKey: String,
    val userAgent: String?,
    val lastSeenAt: Instant?,
    val createdAt: Instant
)