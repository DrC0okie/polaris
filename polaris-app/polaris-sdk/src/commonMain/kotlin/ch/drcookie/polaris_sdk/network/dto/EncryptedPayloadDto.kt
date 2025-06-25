package ch.drcookie.polaris_sdk.network.dto

import ch.drcookie.polaris_sdk.util.UByteArrayBase64Serializer
import kotlinx.serialization.Serializable

@Serializable
@OptIn(ExperimentalUnsignedTypes::class)
data class EncryptedPayloadDto (
    val deliveryId: Long,
    val beaconId: UInt,
    @Serializable(with = UByteArrayBase64Serializer::class)
    val encryptedBlob: UByteArray
) {

    override fun hashCode(): Int {
        var result = deliveryId.hashCode()
        result = 31 * result + beaconId.hashCode()
        result = 31 * result + encryptedBlob.contentHashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as EncryptedPayloadDto

        if (deliveryId != other.deliveryId) return false
        if (beaconId != other.beaconId) return false
        if (!encryptedBlob.contentEquals(other.encryptedBlob)) return false

        return true
    }
}