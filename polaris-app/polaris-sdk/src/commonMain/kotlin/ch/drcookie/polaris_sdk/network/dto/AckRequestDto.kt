package ch.drcookie.polaris_sdk.network.dto

import ch.drcookie.polaris_sdk.util.UByteArrayBase64Serializer
import kotlinx.serialization.Serializable

@OptIn(ExperimentalUnsignedTypes::class)
@Serializable
data class AckRequestDto(
    val deliveryId: Long,
    @Serializable(with = UByteArrayBase64Serializer::class)
    val ackBlob: UByteArray
) {
    override fun hashCode(): Int {
        var result = deliveryId.hashCode()
        result = 31 * result + ackBlob.contentHashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as AckRequestDto

        if (deliveryId != other.deliveryId) return false
        if (!ackBlob.contentEquals(other.ackBlob)) return false

        return true
    }
}
