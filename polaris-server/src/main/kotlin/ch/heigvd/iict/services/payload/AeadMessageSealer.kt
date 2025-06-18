package ch.heigvd.iict.services.payload

import ch.heigvd.iict.entities.Beacon
import ch.heigvd.iict.services.crypto.ISharedKeyManager
import ch.heigvd.iict.services.crypto.LibsodiumBridge
import ch.heigvd.iict.services.crypto.model.*
import ch.heigvd.iict.util.PoLUtils.toUByteArrayLE
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class AeadMessageSealer(private val keyManager: ISharedKeyManager) : IMessageSealer {
    @OptIn(ExperimentalUnsignedTypes::class)
    override fun seal(plaintext: PlaintextMessage, targetBeacon: Beacon): SealedMessage {
        val sharedKey = keyManager.getSharedKeyForBeacon(targetBeacon)
        val nonce = LibsodiumBridge.generateNonce(12) // 12 bytes for IETF
        val associatedData = targetBeacon.beaconId.toUInt().toUByteArrayLE()

        val ciphertextWithTag = LibsodiumBridge.aeadEncrypt(
            message = plaintext.toBytes(),
            associatedData = associatedData,
            nonce = nonce,
            key = sharedKey.asUByteArray()
        )

        return SealedMessage(targetBeacon.beaconId, nonce, ciphertextWithTag)
    }
}