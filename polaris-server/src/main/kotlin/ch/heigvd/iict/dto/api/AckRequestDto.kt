package ch.heigvd.iict.dto.api

import ch.heigvd.iict.util.UByteArrayAsBase64StringSerializer
import kotlinx.serialization.Serializable

@OptIn(ExperimentalUnsignedTypes::class)
@Serializable
data class AckRequestDto(
    // The phone echoes back the deliveryId it received in the GET request.
    val deliveryId: Long,

    // The raw, encrypted ACK/ERR blob received from the beacon.
    @Serializable(with = UByteArrayAsBase64StringSerializer::class)
    val ackBlob: UByteArray
)