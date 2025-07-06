package ch.heigvd.iict.services.crypto

import ch.heigvd.iict.entities.Beacon
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import java.util.concurrent.ConcurrentHashMap

@ApplicationScoped
class X25519SharedKeyManager(private val keyManager: KeyManager) : ISharedKeyManager {

    private val sharedKeyCache = ConcurrentHashMap<Long, ByteArray>()

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

    fun invalidateCacheForBeacon(beacon: Beacon) {
        sharedKeyCache.remove(beacon.id)
        Log.info("Cache invalidated for beacon ${beacon.id}")
    }
}