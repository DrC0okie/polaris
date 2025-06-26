package ch.drcookie.polaris_sdk.ble.model

@OptIn(ExperimentalUnsignedTypes::class)
public data class Beacon(
    val id: UInt,
    val name: String,
    val locationDescription: String,
    val publicKey: UByteArray,
    val lastKnownCounter: ULong
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Beacon

        if (id != other.id) return false
        if (name != other.name) return false
        if (locationDescription != other.locationDescription) return false
        if (!publicKey.contentEquals(other.publicKey)) return false
        if (lastKnownCounter != other.lastKnownCounter) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + locationDescription.hashCode()
        result = 31 * result + publicKey.contentHashCode()
        result = 31 * result + lastKnownCounter.hashCode()
        return result
    }
}