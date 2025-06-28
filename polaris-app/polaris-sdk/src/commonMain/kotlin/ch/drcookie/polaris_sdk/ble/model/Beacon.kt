package ch.drcookie.polaris_sdk.ble.model

/**
 * Represents a known Polaris beacon.
 *
 * Holds all the static information about a beacon that is needed by the SDK
 *
 * @property id The unique id of the beacon.
 * @property name Name for the beacon.
 * @property locationDescription Description of the beacon's physical location.
 * @property publicKey The Ed25519 public key of the beacon.
 * @property lastKnownCounter The last known counter value from the beacon.
 */
@OptIn(ExperimentalUnsignedTypes::class)
public data class Beacon(
    val id: UInt,
    val name: String,
    val locationDescription: String,
    val publicKey: UByteArray,
    val lastKnownCounter: ULong
) {
    /**
    * Necessary, because the compiler cannot infer it for the [UByteArray] class
    */
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

    /**
     * Necessary, because the compiler cannot infer it for the [UByteArray] class
     */
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + locationDescription.hashCode()
        result = 31 * result + publicKey.contentHashCode()
        result = 31 * result + lastKnownCounter.hashCode()
        return result
    }
}