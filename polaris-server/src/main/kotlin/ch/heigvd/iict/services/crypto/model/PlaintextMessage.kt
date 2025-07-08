package ch.heigvd.iict.services.crypto.model

import ch.heigvd.iict.util.PoLUtils.toUByteArrayLE
import ch.heigvd.iict.services.protocol.MessageType
import ch.heigvd.iict.services.protocol.OperationType
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A data class representing the unencrypted content of a secure message.
 *
 * This structure is what gets serialized and then encrypted for the end-to-end secure channel.
 * Its serialization format must exactly match the `InnerPlaintext` struct on the beacon firmware.
 *
 * @property msgId A unique identifier for this message.
 * @property msgType The type of the message (e.g., REQ, ACK, ERR).
 * @property opType The specific operation this message pertains to.
 * @property beaconCounter The beacon's monotonic counter value at the time the message was created.
 * @property payload The variable-length payload of the message, typically a JSON string.
 */
@OptIn(ExperimentalUnsignedTypes::class)
data class PlaintextMessage(
    val msgId: Long,
    val msgType: MessageType,
    val opType: OperationType,
    val beaconCounter: Long,
    val payload: ByteArray
) {
    /**
     * Serializes this message into a byte array with a little-endian format,
     * matching the beacon's C++ struct layout.
     * @return The serialized message as a [UByteArray].
     */
    fun toBytes(): UByteArray {
        // msgId (4) + msgType (1) + opType (1) + beaconCnt (4) + payloadLength (2) + payload
        val msgIdBytes = msgId.toUInt().toUByteArrayLE()
        val beaconCounterBytes = beaconCounter.toUInt().toUByteArrayLE()
        val payloadLenBytes = payload.size.toUShort().toUByteArrayLE()

        return msgIdBytes +
                msgType.code +
                opType.code +
                beaconCounterBytes +
                payloadLenBytes +
                payload.asUByteArray()
    }

    companion object {

        /**
         * Deserializes a byte array into a [PlaintextMessage] object.
         * @param bytes The raw byte array received after decryption.
         * @return A new [PlaintextMessage] instance.
         * @throws IllegalArgumentException if the byte array is malformed or contains invalid data.
         */
        fun fromBytes(bytes: ByteArray): PlaintextMessage {
            val buffer = ByteBuffer.wrap(bytes)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            val msgId = buffer.int.toLong()
            val msgType = MessageType.fromCode(buffer.get().toUByte())
            val opType = OperationType.fromCode(buffer.get().toUByte())
            val beaconCounter = buffer.int.toLong()
            val payloadLength = buffer.short.toInt()

            if (buffer.remaining() < payloadLength) {
                throw IllegalArgumentException("Payload length mismatch during deserialization.")
            }

            val payload = ByteArray(payloadLength)
            buffer.get(payload)

            if (msgType == MessageType.UNDEFINED || opType == OperationType.UNKNOWN) {
                throw IllegalArgumentException("Invalid or unknown message/operation type in payload")
            }

            return PlaintextMessage(msgId, msgType, opType, beaconCounter, payload)
        }
    }
}