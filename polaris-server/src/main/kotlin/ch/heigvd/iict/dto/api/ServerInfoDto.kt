package ch.heigvd.iict.dto.api


import kotlinx.serialization.Serializable

@Serializable
data class ServerInfoDto(
    val serverX25519PublicKey: String
)
