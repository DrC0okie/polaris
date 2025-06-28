package ch.drcookie.polaris_sdk.ble.model

import ch.drcookie.polaris_sdk.network.NetworkClient
/**
 * Encrypted payload job fetched from the server, intended for delivery to a specific beacon.
 *
 * @property deliveryId A unique identifier for this specific delivery job, assigned by the server.
 *                      This ID must be sent back to the server in the [DeliveryAck].
 * @property beaconId The ID of the target beacon that this payload must be delivered to.
 * @property blob The raw, encrypted data payload to be sent to the beacon.
 *
 * @see NetworkClient.getPayloadsForDelivery
 * @see DeliveryAck
 */
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