package ch.heigvd.iict.dto.admin

import java.time.Instant

data class BeaconAdminDto(
    val id: Long?,
    val technicalId: Int,
    val name: String,
    val locationDescription: String,
    val publicKeyHex: String,
    val lastKnownCounter: Long,
    val createdAt: Instant,
    val updatedAt: Instant
)