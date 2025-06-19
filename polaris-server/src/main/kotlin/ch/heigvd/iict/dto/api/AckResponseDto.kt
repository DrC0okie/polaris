package ch.heigvd.iict.dto.api

import kotlinx.serialization.Serializable

@Serializable
data class AckResponseDto (
    val deliveryId: Long,
    val status: String, // e.g., "ACKNOWLEDGED", "FAILED_DECRYPTION", "REDUNDANT"
    val message: String
)