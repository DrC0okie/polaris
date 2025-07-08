package ch.heigvd.iict.services.crypto

import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.*

/**
 * Manages the server's own cryptographic identity.
 *
 * This service is responsible for loading the server's primary X25519 private key from
 * configuration and deriving the corresponding public key.
 */
@ApplicationScoped
class KeyManager {

    /**
     * The server's private key, loaded from the `polaris.server.aead.sk.b64` configuration property.
     * The key is expected to be a 32-byte value, Base64 encoded.
     */
    @ConfigProperty(name = "polaris.server.aead.sk.b64")
    lateinit var serverSkBase64: String

    /** The decoded 32-byte X25519 private key. Lazily initialized on first access. */
    val serverPrivateKey: ByteArray by lazy {
        try {
            val key = Base64.getDecoder().decode(serverSkBase64)
            if (key.size != 32) throw IllegalArgumentException("Key must be 32 bytes")
            key
        } catch (e: Exception) {
            throw IllegalStateException("Invalid Base64 format for polaris.server.aead.sk.b64", e)
        }
    }

    /** The corresponding X25519 public key, derived from the private key. Lazily initialized on first access. */
    @OptIn(ExperimentalUnsignedTypes::class)
    val serverPublicKey: ByteArray by lazy {
        LibsodiumBridge.scalarMultBase(serverPrivateKey.asUByteArray()).asByteArray()
    }
}