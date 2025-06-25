package ch.drcookie.polaris_sdk.ble.model

@OptIn(ExperimentalUnsignedTypes::class)
public data class EncryptedPayload(
    val deliveryId: Long,
    val beaconId: UInt,
    val blob: UByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as EncryptedPayload

        if (deliveryId != other.deliveryId) return false
        if (beaconId != other.beaconId) return false
        if (!blob.contentEquals(other.blob)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = deliveryId.hashCode()
        result = 31 * result + beaconId.hashCode()
        result = 31 * result + blob.contentHashCode()
        return result
    }
}
