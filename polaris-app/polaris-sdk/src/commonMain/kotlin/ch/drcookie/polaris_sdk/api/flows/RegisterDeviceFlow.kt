package ch.drcookie.polaris_sdk.api.flows

import ch.drcookie.polaris_sdk.model.dto.PhoneRegistrationRequestDto
import ch.drcookie.polaris_sdk.network.ApiClient
import ch.drcookie.polaris_sdk.storage.KeyStore

class RegisterDeviceFlow(
    private val apiClient: ApiClient,
    private val keyRepo: KeyStore
) {
    @OptIn(ExperimentalUnsignedTypes::class)
    suspend operator fun invoke(deviceModel: String, osVersion: String, appVersion: String): Int {
        val (pk, _) = keyRepo.getOrCreateSignatureKeyPair()
        val request = PhoneRegistrationRequestDto(
            publicKey = pk,
            deviceModel = deviceModel,
            osVersion = osVersion,
            appVersion = appVersion
        )
        val beacons = apiClient.registerPhone(request)
        return beacons.size
    }
}