package ch.drcookie.polaris_app.data.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class EncryptedPayloadListDto(
    val payloads: List<EncryptedPayloadDto>
)
