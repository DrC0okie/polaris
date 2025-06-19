package ch.heigvd.iict.services.payload

import ch.heigvd.iict.entities.Beacon
import ch.heigvd.iict.services.crypto.ISharedKeyManager
import ch.heigvd.iict.services.crypto.LibsodiumBridge
import ch.heigvd.iict.services.crypto.model.PlaintextMessage
import ch.heigvd.iict.services.crypto.model.SealedMessage
import ch.heigvd.iict.util.PoLUtils.toUByteArrayLE
import jakarta.enterprise.context.ApplicationScoped

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

        return PlaintextMessage.fromBytes(plaintextBytes)
    }
}