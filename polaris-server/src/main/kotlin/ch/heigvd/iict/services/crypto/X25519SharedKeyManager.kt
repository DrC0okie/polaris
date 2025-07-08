package ch.heigvd.iict.services.crypto

import ch.heigvd.iict.entities.Beacon
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages the derivation and caching of shared secrets for end-to-end encryption.
 *
 * This service implements the [ISharedKeyManager] interface and uses a cache to avoid
 * re-computing the expensive Diffie-Hellman key exchange for every message.
 *
 * @property keyManager The service that holds the server's private key.
 */
@ApplicationScoped
class X25519SharedKeyManager(private val keyManager: KeyManager) : ISharedKeyManager {

    private val sharedKeyCache = ConcurrentHashMap<Long, ByteArray>()

    /**
     * Retrieves the shared secret for a given beacon, computing and caching it if necessary.
     *
     * @param beacon The target [Beacon] for which to get the shared key.
     * @return The 32-byte shared secret key.
     * @throws IllegalStateException if the target beacon has not been provisioned with an X25519 public key.
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    override fun getSharedKeyForBeacon(beacon: Beacon): ByteArray {
        return sharedKeyCache.getOrPut(beacon.id!!) {
            val beaconPk = beacon.publicKeyX25519
                ?: throw IllegalStateException("Beacon ${beacon.id} does not have an X25519 public key provisioned.")
            LibsodiumBridge.scalarMult(
                keyManager.serverPrivateKey.asUByteArray(),
                beaconPk.asUByteArray()
            ).asByteArray()
        }
    }

    /**
     * Removes the cached shared key for a specific beacon.
     * This should be called when a beacon's X25519 key is rotated to force re-computation of the shared secret.
     *
     * @param beacon The [Beacon] whose cached key should be invalidated.
     */
    fun invalidateCacheForBeacon(beacon: Beacon) {
        sharedKeyCache.remove(beacon.id)
        Log.info("Cache invalidated for beacon ${beacon.id}")
    }
}