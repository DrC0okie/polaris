package ch.heigvd.iict.services.crypto

import ch.heigvd.iict.entities.Beacon

/**
 * Defines the contract for a service that can provide a shared secret key for communicating
 * with a specific beacon.
 */
interface ISharedKeyManager {

    /**
     * Retrieves the shared secret key for a given beacon.
     *
     * Implementations are responsible for performing the key exchange (e.g., ECDH)
     * and may employ caching strategies for performance.
     *
     * @param beacon The beacon for which the shared key is needed.
     * @return A byte array containing the 32-byte shared secret.
     */
    fun getSharedKeyForBeacon(beacon: Beacon): ByteArray
}