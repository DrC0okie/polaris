package ch.heigvd.iict.services.crypto.model

import ch.heigvd.iict.util.PoLUtils.toUByteArrayLE
import ch.heigvd.iict.services.protocol.MessageType
import ch.heigvd.iict.services.protocol.OperationType
import java.nio.ByteBuffer
import java.nio.ByteOrder

@OptIn(ExperimentalUnsignedTypes::class)
data class PlaintextMessage(
    val msgId: Long,
    val msgType: MessageType,
    val opType: OperationType,
    val beaconCounter: Long,
    val payload: ByteArray
) {
    // This serialization must match the beacon's `InnerPlaintext::serialize`
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

            if (msgType == MessageType.UNDEFINED || opType == OperationType.UNDEFINED) {
                throw IllegalArgumentException("Invalid or unknown message/operation type in payload")
            }

            return PlaintextMessage(msgId, msgType, opType, beaconCounter, payload)
        }
    }
}