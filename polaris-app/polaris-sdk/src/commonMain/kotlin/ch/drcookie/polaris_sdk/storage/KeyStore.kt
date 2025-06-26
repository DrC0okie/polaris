package ch.drcookie.polaris_sdk.storage

import ch.drcookie.polaris_sdk.api.SdkError
import ch.drcookie.polaris_sdk.api.SdkResult

/**
 * A repository responsible for the secure storage and retrieval of cryptographic keypairs.
 * The implementation of this interface will handle the actual storage mechanism
 * (e.g., SharedPreferences, Android Keystore).
 */
public interface KeyStore {
    /**
     * Retrieves the phone's signature keypair. If one does not exist, it should
     * generate a new one, store it securely, and then return it.
     *
     * @return A Pair containing the Public Key and the Secret Key.
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    public suspend fun getOrCreateSignatureKeyPair(): SdkResult<Pair<UByteArray, UByteArray>, SdkError>

    /**
     * Clears all stored keys. Useful for factory reset or user logout.
     */
    public suspend fun clearAllKeys(): SdkResult<Unit, SdkError>
}