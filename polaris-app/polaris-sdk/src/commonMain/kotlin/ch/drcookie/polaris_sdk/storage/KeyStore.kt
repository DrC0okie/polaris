package ch.drcookie.polaris_sdk.storage

/**
 * A repository responsible for the secure storage and retrieval of cryptographic keypairs.
 * The implementation of this interface will handle the actual storage mechanism
 * (e.g., SharedPreferences, Android Keystore).
 */
interface KeyStore {
    /**
     * Retrieves the phone's signature keypair. If one does not exist, it should
     * generate a new one, store it securely, and then return it.
     *
     * @return A Pair containing the Public Key and the Secret Key.
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun getOrCreateSignatureKeyPair(): Pair<UByteArray, UByteArray>

    /**
     * Clears all stored keys. Useful for factory reset or user logout.
     */
    suspend fun clearAllKeys()
}