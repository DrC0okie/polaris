package ch.drcookie.polaris_sdk.network.dto

import ch.drcookie.polaris_sdk.util.UByteArrayBase64Serializer
import kotlinx.serialization.Serializable

@Serializable
@OptIn(ExperimentalUnsignedTypes::class)
internal data class BeaconProvisioningDto(
    internal val beaconId: UInt,
    internal val name: String,
    internal val locationDescription: String,
    @Serializable(with = UByteArrayBase64Serializer::class)
    internal val publicKey: UByteArray,
    internal val lastKnownCounter: ULong
) {

    override fun hashCode(): Int {
        var result = beaconId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + locationDescription.hashCode()
        result = 31 * result + publicKey.contentHashCode()
        result = 31 * result + lastKnownCounter.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BeaconProvisioningDto

        if (beaconId != other.beaconId) return false
        if (name != other.name) return false
        if (locationDescription != other.locationDescription) return false
        if (!publicKey.contentEquals(other.publicKey)) return false
        if (lastKnownCounter != other.lastKnownCounter) return false

        return true
    }
}