package ch.heigvd.iict.services.crypto

import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.*

@ApplicationScoped
class KeyManager {

    @ConfigProperty(name = "polaris.server.aead.sk.b64")
    lateinit var serverSkBase64: String

    val serverPrivateKey: ByteArray by lazy {
        try {
            val key = Base64.getDecoder().decode(serverSkBase64)
            if (key.size != 32) throw IllegalArgumentException("Key must be 32 bytes")
            key
        } catch (e: Exception) {
            throw IllegalStateException("Invalid Base64 format for polaris.server.aead.sk.b64", e)
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    val serverPublicKey: ByteArray by lazy {
        LibsodiumBridge.scalarMultBase(serverPrivateKey.asUByteArray()).asByteArray()
    }
}