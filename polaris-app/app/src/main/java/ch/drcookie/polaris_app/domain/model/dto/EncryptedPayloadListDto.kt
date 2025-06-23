package ch.drcookie.polaris_app.domain.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class EncryptedPayloadListDto(
    val payloads: List<EncryptedPayloadDto>
)
