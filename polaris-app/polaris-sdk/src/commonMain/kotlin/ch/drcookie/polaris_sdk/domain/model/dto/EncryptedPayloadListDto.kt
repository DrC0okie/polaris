package ch.drcookie.polaris_sdk.domain.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class EncryptedPayloadListDto(
    val payloads: List<EncryptedPayloadDto>
)
