package ch.drcookie.polaris_sdk.api.flows

import ch.drcookie.polaris_sdk.api.SdkError
import ch.drcookie.polaris_sdk.api.SdkResult
import ch.drcookie.polaris_sdk.network.ApiClient
import ch.drcookie.polaris_sdk.storage.KeyStore

public class RegisterDeviceFlow(
    private val apiClient: ApiClient,
    private val keyStore: KeyStore,
) {
    @OptIn(ExperimentalUnsignedTypes::class)
    public suspend operator fun invoke(
        deviceModel: String,
        osVersion: String,
        appVersion: String,
    ): SdkResult<Int, SdkError> {
        // Get the key pair.
        val keyPairResult = keyStore.getOrCreateSignatureKeyPair()
        val (pk, _) = when (keyPairResult) {
            is SdkResult.Success -> keyPairResult.value
            is SdkResult.Failure -> return keyPairResult
        }
        // Register the phone.
        val registerResult = apiClient.registerPhone(pk, deviceModel, osVersion, appVersion)
        return when (registerResult) {
            is SdkResult.Success -> SdkResult.Success(registerResult.value.size)
            is SdkResult.Failure -> registerResult
        }
    }
}