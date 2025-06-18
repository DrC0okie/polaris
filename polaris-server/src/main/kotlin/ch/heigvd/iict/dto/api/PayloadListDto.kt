package ch.heigvd.iict.dto.api

import kotlinx.serialization.Serializable

@Serializable
data class PayloadListDto(
    val payloads: List<PhonePayloadDto>
)
