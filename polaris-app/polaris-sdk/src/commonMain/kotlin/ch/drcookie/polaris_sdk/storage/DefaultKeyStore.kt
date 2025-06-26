package ch.drcookie.polaris_sdk.storage

import ch.drcookie.polaris_sdk.api.SdkError
import ch.drcookie.polaris_sdk.api.SdkResult
import ch.drcookie.polaris_sdk.crypto.CryptoUtils
import ch.drcookie.polaris_sdk.util.ByteConversionUtils
import ch.drcookie.polaris_sdk.util.ByteConversionUtils.toHexString
import com.liftric.kvault.KVault
import io.github.oshai.kotlinlogging.KotlinLogging

private val Log = KotlinLogging.logger {}

internal class DefaultKeyStore(
    private val store: KVault,
    private val cryptoManager: CryptoUtils,
) : KeyStore {

    private companion object {
        const val KEY_PHONE_PK = "polaris_phone_pk"
        const val KEY_PHONE_SK = "polaris_phone_sk"
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun getOrCreateSignatureKeyPair(): SdkResult<Pair<UByteArray, UByteArray>, SdkError> {
        return runCatching {
            Log.info { "Attempting to get or create key pair..." }
            val pubKeyHex = store.string(forKey = KEY_PHONE_PK)
            val secKeyHex = store.string(forKey = KEY_PHONE_SK)

            // If keys exist in storage, use them.
            if (pubKeyHex != null && secKeyHex != null) {
                Log.info { "Found existing keys in store. Loading them." }
                ByteConversionUtils.hexStringToUByteArray(pubKeyHex) to ByteConversionUtils.hexStringToUByteArray(
                    secKeyHex
                )
            } else {
                Log.warn { "No keys found in store. Generating a new pair." }

                val (publicKey, secretKey) = cryptoManager.generateKeyPair()

                Log.info { "New public key generated: ${publicKey.toHexString()}"}

                // Save the new keys to storage for next time.
                store.set(key = KEY_PHONE_PK, stringValue = publicKey.toHexString())
                store.set(key = KEY_PHONE_SK, stringValue = secretKey.toHexString())

                // Return the new keys.
                publicKey to secretKey
            }
        }.fold(
            onSuccess = { keyPair -> SdkResult.Success(keyPair) },
            onFailure = { throwable -> SdkResult.Failure(SdkError.GenericError(throwable)) }
        )
    }

    override suspend fun clearAllKeys(): SdkResult<Unit, SdkError> {
        return runCatching {
            store.deleteObject(forKey = KEY_PHONE_PK)
            store.deleteObject(forKey = KEY_PHONE_SK)
        }.fold(
            onSuccess = { SdkResult.Success(Unit) },
            onFailure = { throwable -> SdkResult.Failure(SdkError.GenericError(throwable)) }
        )
    }
}