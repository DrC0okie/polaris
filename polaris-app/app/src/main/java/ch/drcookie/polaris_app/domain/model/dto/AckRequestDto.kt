package ch.drcookie.polaris_app.domain.model.dto

import ch.drcookie.polaris_app.core.serialization.UByteArrayBase64Serializer
import kotlinx.serialization.Serializable

@OptIn(ExperimentalUnsignedTypes::class)
@Serializable
data class AckRequestDto(
    val deliveryId: Long,
    @Serializable(with = UByteArrayBase64Serializer::class)
    val ackBlob: UByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AckRequestDto

        if (deliveryId != other.deliveryId) return false
        if (!ackBlob.contentEquals(other.ackBlob)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = deliveryId.hashCode()
        result = 31 * result + ackBlob.contentHashCode()
        return result
    }
}
