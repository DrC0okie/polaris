package ch.heigvd.iict.dto.api

import kotlinx.serialization.Serializable

@Serializable
data class PoLTokenValidationResultDto(
    val isValid: Boolean,
    val message: String?,
    val id: Long?
)