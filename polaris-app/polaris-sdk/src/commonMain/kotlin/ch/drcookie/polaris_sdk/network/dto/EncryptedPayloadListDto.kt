package ch.drcookie.polaris_sdk.network.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class EncryptedPayloadListDto(
    internal val payloads: List<EncryptedPayloadDto>
)