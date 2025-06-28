package ch.drcookie.polaris_sdk.api.flows

import ch.drcookie.polaris_sdk.api.SdkError
import ch.drcookie.polaris_sdk.api.SdkResult
import ch.drcookie.polaris_sdk.network.NetworkClient
import ch.drcookie.polaris_sdk.storage.KeyStore

/**
 * A high-level use case that handles the device onboarding process with the backend server.
 *
 * - If the device has no stored Phone ID, it performs a full registration.
 * - If a Phone ID already exists, it performs a faster fetch to update the known beacon list.
 *
 * @property networkClient The client for all server communication.
 * @property keyStore The store for creating or retrieving the device's cryptographic keys.
 */
public class FetchBeaconsFlow(
    private val networkClient: NetworkClient,
    private val keyStore: KeyStore,
) {
    /**
     * Executes the flow
     *
     * @param deviceModel A string identifying the device model
     * @param osVersion A string identifying the OS version
     * @param appVersion A string identifying the application version
     * @return An [SdkResult] containing the number of known beacons on success.
     */
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
        val registerResult = networkClient.registerPhone(pk, deviceModel, osVersion, appVersion)
        return when (registerResult) {
            is SdkResult.Success -> SdkResult.Success(registerResult.value.size)
            is SdkResult.Failure -> registerResult
        }
    }
}