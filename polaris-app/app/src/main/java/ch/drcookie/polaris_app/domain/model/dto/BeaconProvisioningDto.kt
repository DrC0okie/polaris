package ch.drcookie.polaris_app.domain.model.dto

import ch.drcookie.polaris_app.core.serialization.UByteArrayBase64Serializer
import kotlinx.serialization.Serializable

@Serializable
@OptIn(ExperimentalUnsignedTypes::class)
data class BeaconProvisioningDto(
    val beaconId: UInt,
    val name: String,
    val locationDescription: String,
    @Serializable(with = UByteArrayBase64Serializer::class)
    val publicKey: UByteArray,
    val lastKnownCounter: ULong
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BeaconProvisioningDto

        if (beaconId != other.beaconId) return false
        if (name != other.name) return false
        if (locationDescription != other.locationDescription) return false
        if (!publicKey.contentEquals(other.publicKey)) return false
        if (lastKnownCounter != other.lastKnownCounter) return false

        return true
    }

    override fun hashCode(): Int {
        var result = beaconId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + locationDescription.hashCode()
        result = 31 * result + publicKey.contentHashCode()
        result = 31 * result + lastKnownCounter.hashCode()
        return result
    }
}