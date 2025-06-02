package ch.heigvd.iict.dto.api

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponseDto(
    val error: String
)