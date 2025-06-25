package ch.drcookie.polaris_sdk.api.flows

import ch.drcookie.polaris_sdk.network.ApiClient
import ch.drcookie.polaris_sdk.storage.KeyStore

public class RegisterDeviceFlow(
    private val apiClient: ApiClient,
    private val keyRepo: KeyStore
) {
    @OptIn(ExperimentalUnsignedTypes::class)
    public suspend operator fun invoke(deviceModel: String, osVersion: String, appVersion: String): Int {
        val (pk, _) = keyRepo.getOrCreateSignatureKeyPair()
        val beacons = apiClient.registerPhone(pk, deviceModel, osVersion, appVersion)
        return beacons.size
    }
}