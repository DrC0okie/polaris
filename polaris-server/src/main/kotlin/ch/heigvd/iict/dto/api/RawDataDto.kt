package ch.heigvd.iict.dto.api

import ch.heigvd.iict.util.UByteArrayAsBase64StringSerializer
import kotlinx.serialization.Serializable

@Serializable
@OptIn(ExperimentalUnsignedTypes::class)
data class RawDataDto(
    @Serializable(with = UByteArrayAsBase64StringSerializer::class)
    val data: UByteArray
)