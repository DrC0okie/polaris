package ch.heigvd.iict.dto.api

import ch.heigvd.iict.util.UByteArrayAsBase64StringSerializer
import kotlinx.serialization.Serializable

@Serializable
@OptIn(ExperimentalUnsignedTypes::class)
data class BeaconPayloadDto(
    val beaconId: UInt,
    @Serializable(with = UByteArrayAsBase64StringSerializer::class)
    val data: UByteArray
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BeaconPayloadDto

        if (beaconId != other.beaconId) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = beaconId.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}