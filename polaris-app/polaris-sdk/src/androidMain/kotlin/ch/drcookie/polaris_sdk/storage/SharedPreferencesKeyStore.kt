package ch.drcookie.polaris_sdk.storage

import ch.drcookie.polaris_sdk.crypto.CryptoUtils
import ch.drcookie.polaris_sdk.util.ByteConversionUtils
import ch.drcookie.polaris_sdk.util.ByteConversionUtils.toHexString

class SharedPreferencesKeyStore(
    private val sdkPreferences: SdkPreferences,
    private val cryptoManager: CryptoUtils
) : KeyStore {

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun getOrCreateSignatureKeyPair(): Pair<UByteArray, UByteArray> {
        val pubKeyHex = sdkPreferences.phonePublicKey
        val secKeyHex = sdkPreferences.phoneSecretKey

        // If keys exist in storage, use them.
        if (pubKeyHex != null && secKeyHex != null) {
            return ByteConversionUtils.hexStringToUByteArray(pubKeyHex) to ByteConversionUtils.hexStringToUByteArray(
                secKeyHex
            )
        }

        // If keys DO NOT exist, use the CryptoManager tool to make new ones.
        val (publicKey, secretKey) = cryptoManager.generateKeyPair()

        // Save the new keys to storage for next time.
        sdkPreferences.phonePublicKey = publicKey.toHexString()
        sdkPreferences.phoneSecretKey = secretKey.toHexString()

        // Return the new keys.
        return publicKey to secretKey
    }

    override suspend fun clearAllKeys() {
        sdkPreferences.phonePublicKey = null
        sdkPreferences.phoneSecretKey = null
    }
}