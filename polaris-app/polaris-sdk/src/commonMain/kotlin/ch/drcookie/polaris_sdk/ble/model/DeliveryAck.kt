package ch.drcookie.polaris_sdk.ble.model

@OptIn(ExperimentalUnsignedTypes::class)
public data class DeliveryAck(
    val deliveryId: Long,
    val ackBlob: UByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DeliveryAck

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
