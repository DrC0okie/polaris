package ch.drcookie.polaris_sdk.storage

import ch.drcookie.polaris_sdk.api.SdkError
import ch.drcookie.polaris_sdk.api.SdkResult

/**
 * An interface for securely storing and retrieving cryptographic key pairs.
 *
 * An instance is available via `Polaris.keyStore`.
 */
public interface KeyStore {
    /**
     * Retrieves the phone's signature keypair. If one does not exist, generates a new one.
     *
     * @return An [SdkResult] containing a [Pair] of (Public Key, Secret Key) on success.
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    public suspend fun getOrCreateSignatureKeyPair(): SdkResult<Pair<UByteArray, UByteArray>, SdkError>

    /**
     * Clears all stored cryptographic keys from the KeyStore.
     *
     * @return An [SdkResult] containing [Unit] on success.
     */
    public suspend fun clearAllKeys(): SdkResult<Unit, SdkError>
}