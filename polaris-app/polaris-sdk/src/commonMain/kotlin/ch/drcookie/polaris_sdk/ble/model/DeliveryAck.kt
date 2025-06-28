package ch.drcookie.polaris_sdk.ble.model

import ch.drcookie.polaris_sdk.network.NetworkClient

/**
 * Acknowledgement data that must be sent to the server after delivering a server payload to a beacon.
 *
 * @property deliveryId The unique ID of the delivery job.
 * @property ackBlob The raw ACK or ERR data blob received from the beacon after it processed the payload.
 *
 * @see NetworkClient.submitSecureAck
 * @see EncryptedPayload
 */
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