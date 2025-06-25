package ch.drcookie.polaris_sdk.network

import ch.drcookie.polaris_sdk.model.PoLToken
import ch.drcookie.polaris_sdk.network.dto.AckRequestDto
import ch.drcookie.polaris_sdk.network.dto.BeaconProvisioningDto
import ch.drcookie.polaris_sdk.network.dto.EncryptedPayloadDto
import ch.drcookie.polaris_sdk.network.dto.PhoneRegistrationRequestDto
import ch.drcookie.polaris_sdk.storage.SdkPreferences

class KtorApiClient(
    private val api: KtorClientFactory,
    private val userPrefs: SdkPreferences
) : ApiClient {
    // Holds the list of beacons after a successful registration/fetch
    override var knownBeacons: List<BeaconProvisioningDto> = emptyList()

    override fun getPhoneId(): Long {
        // It simply delegates the call to the underlying preference storage.
        return userPrefs.phoneId
    }

    override suspend fun registerPhone(request: PhoneRegistrationRequestDto): List<BeaconProvisioningDto> {
        val response = api.registerPhone(request)
        userPrefs.apiKey = response.apiKey
        userPrefs.phoneId = response.assignedPhoneId ?: -1L
        knownBeacons = response.beacons.beacons
        return knownBeacons
    }

    override suspend fun submitPoLToken(token: PoLToken) {
        val apiKey = userPrefs.apiKey ?: throw IllegalStateException("API Key not available.")
        api.sendPoLToken(token, apiKey)
    }

    override suspend fun submitSecureAck(request: AckRequestDto) {
        val apiKey = userPrefs.apiKey ?: throw IllegalStateException("API Key not available.")
        api.postAck(apiKey, request)
    }

    override suspend fun getPayloadsForDelivery(): List<EncryptedPayloadDto> {
        val apiKey = userPrefs.apiKey ?: throw IllegalStateException("API Key not available.")
        return api.getPayloads(apiKey).payloads
    }
}