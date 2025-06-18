package ch.heigvd.iict.services.payload

import ch.heigvd.iict.entities.Beacon
import ch.heigvd.iict.services.crypto.ISharedKeyManager
import ch.heigvd.iict.services.crypto.LibsodiumBridge
import ch.heigvd.iict.services.crypto.model.PlaintextMessage
import ch.heigvd.iict.services.crypto.model.SealedMessage
import ch.heigvd.iict.util.PoLUtils.toUByteArrayLE
import jakarta.enterprise.context.ApplicationScoped
import java.nio.ByteBuffer

@ApplicationScoped
class AeadMessageUnsealer(private val keyManager: ISharedKeyManager) : IMessageUnsealer {
    @OptIn(ExperimentalUnsignedTypes::class)
    override fun unseal(sealed: SealedMessage, sourceBeacon: Beacon): PlaintextMessage {
        val sharedKey = keyManager.getSharedKeyForBeacon(sourceBeacon)
        val associatedData = sourceBeacon.beaconId.toUInt().toUByteArrayLE()

        val plaintextBytes = LibsodiumBridge.aeadDecrypt(
            ciphertextWithTag = sealed.ciphertextWithTag,
            associatedData = associatedData,
            nonce = sealed.nonce,
            key = sharedKey.asUByteArray()
        ).asByteArray()

        // Deserialize the plaintext bytes back into our PlaintextMessage model
        return deserializePlaintext(plaintextBytes)
    }

    private fun deserializePlaintext(bytes: ByteArray): PlaintextMessage {
        val buffer = ByteBuffer.wrap(bytes)
        val msgId = buffer.int.toLong()
        val msgType = buffer.get().toUByte()
        val opType = buffer.get().toUByte()
        val beaconCnt = buffer.int // We can log this, but don't need it for now
        val payloadLength = buffer.short.toInt()
        val payload = ByteArray(payloadLength)
        buffer.get(payload)

        return PlaintextMessage(msgId, msgType, opType, payload)
    }
}