package ch.drcookie.polaris_sdk.data.repository

import ch.drcookie.polaris_sdk.core.ByteConversionUtils.hexStringToUByteArray
import ch.drcookie.polaris_sdk.core.ByteConversionUtils.toHexString
import ch.drcookie.polaris_sdk.domain.interactor.logic.CryptoManager
import ch.drcookie.polaris_sdk.domain.repository.KeyRepository
import ch.drcookie.polaris_sdk.domain.repository.LocalPreferences

class KeyRepositoryImpl(
    private val localPreferences: LocalPreferences,
    private val cryptoManager: CryptoManager
) : KeyRepository {

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun getOrCreateSignatureKeyPair(): Pair<UByteArray, UByteArray> {
        val pubKeyHex = localPreferences.phonePublicKey
        val secKeyHex = localPreferences.phoneSecretKey

        // If keys exist in storage, use them.
        if (pubKeyHex != null && secKeyHex != null) {
            return hexStringToUByteArray(pubKeyHex) to hexStringToUByteArray(secKeyHex)
        }

        // If keys DO NOT exist, use the CryptoManager tool to make new ones.
        val (publicKey, secretKey) = cryptoManager.generateKeyPair()

        // Save the new keys to storage for next time.
        localPreferences.phonePublicKey = publicKey.toHexString()
        localPreferences.phoneSecretKey = secretKey.toHexString()

        // Return the new keys.
        return publicKey to secretKey
    }

    override suspend fun clearAllKeys() {
        localPreferences.phonePublicKey = null
        localPreferences.phoneSecretKey = null
    }
}