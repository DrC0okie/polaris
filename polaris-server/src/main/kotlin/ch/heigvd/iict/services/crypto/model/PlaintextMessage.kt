package ch.heigvd.iict.services.crypto.model

import ch.heigvd.iict.util.PoLUtils.toUByteArrayLE

@OptIn(ExperimentalUnsignedTypes::class)
data class PlaintextMessage(
    val serverMsgId: Long,
    val msgType: UByte,   // REQ, ACK, ERR
    val opType: UByte,    // Command code
    val payload: ByteArray
) {
    // This serialization must match the beacon's `InnerPlaintext::serialize`
    fun toBytes(): UByteArray {
        // msgId (4) + msgType (1) + opType (1) + beaconCnt (4) + payloadLength (2) + payload
        val msgIdBytes = serverMsgId.toUInt().toUByteArrayLE()
        val beaconCounterBytes = 0u.toUByteArrayLE() // Server sets this to 0

        if (payload.size > UShort.MAX_VALUE.toInt()) {
            throw IllegalArgumentException("Payload size ${payload.size} exceeds maximum of ${UShort.MAX_VALUE}")
        }
        val payloadLenBytes = payload.size.toUShort().toUByteArrayLE()

        return msgIdBytes +
                msgType +
                opType +
                beaconCounterBytes +
                payloadLenBytes +
                payload.asUByteArray()
    }
}