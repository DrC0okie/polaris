package ch.drcookie.polaris_app.repository

import ch.drcookie.polaris_app.data.local.UserPreferences
import ch.drcookie.polaris_app.data.model.PoLToken
import ch.drcookie.polaris_app.data.model.dto.AckRequestDto
import ch.drcookie.polaris_app.data.model.dto.BeaconProvisioningDto
import ch.drcookie.polaris_app.data.model.dto.EncryptedPayloadDto
import ch.drcookie.polaris_app.data.model.dto.PhoneRegistrationRequestDto
import ch.drcookie.polaris_app.data.remote.RemoteDataSource

class AuthRepository(
    private val remoteDataSource: RemoteDataSource,
    private val userPrefs: UserPreferences
) {
    // This property will hold the list of beacons after a successful registration/fetch
    var knownBeacons: List<BeaconProvisioningDto> = emptyList()
        private set

    suspend fun registerPhone(req: PhoneRegistrationRequestDto): List<BeaconProvisioningDto> {
        val response = remoteDataSource.registerPhone(req)
        userPrefs.apiKey = response.apiKey
        userPrefs.phoneId = response.assignedPhoneId ?: -1L
        knownBeacons = response.beacons.beacons
        return knownBeacons
    }

    suspend fun submitPoLToken(token: PoLToken) {
        val apiKey = userPrefs.apiKey ?: throw IllegalStateException("API Key not available.")
        remoteDataSource.sendPoLToken(token, apiKey)
    }

    suspend fun submitSecureAck(ackRequest: AckRequestDto) {
        val apiKey = userPrefs.apiKey ?: throw IllegalStateException("API Key not available.")
        remoteDataSource.postAck(apiKey, ackRequest)
    }

    suspend fun getPayloadsForDelivery(): List<EncryptedPayloadDto> {
        val apiKey = userPrefs.apiKey ?: throw IllegalStateException("API Key not available.")
        return remoteDataSource.getPayloads(apiKey).payloads
    }
    // TODO: Should we manage the shutdown here? elsewhere?
}