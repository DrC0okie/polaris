package ch.drcookie.polaris_app.data.repository

import ch.drcookie.polaris_app.data.datasource.remote.RemoteDataSource
import ch.drcookie.polaris_app.domain.model.PoLToken
import ch.drcookie.polaris_app.domain.model.dto.AckRequestDto
import ch.drcookie.polaris_app.domain.model.dto.BeaconProvisioningDto
import ch.drcookie.polaris_app.domain.model.dto.EncryptedPayloadDto
import ch.drcookie.polaris_app.domain.model.dto.PhoneRegistrationRequestDto
import ch.drcookie.polaris_app.domain.repository.AuthRepository
import ch.drcookie.polaris_app.domain.repository.LocalPreferences

class AuthRepositoryImpl(
    private val remoteDataSource: RemoteDataSource,
    private val userPrefs: LocalPreferences
) : AuthRepository {
    // Holds the list of beacons after a successful registration/fetch
    override var knownBeacons: List<BeaconProvisioningDto> = emptyList()

    override suspend fun registerPhone(req: PhoneRegistrationRequestDto): List<BeaconProvisioningDto> {
        val response = remoteDataSource.registerPhone(req)
        userPrefs.apiKey = response.apiKey
        userPrefs.phoneId = response.assignedPhoneId ?: -1L
        knownBeacons = response.beacons.beacons
        return knownBeacons
    }

    override suspend fun submitPoLToken(token: PoLToken) {
        val apiKey = userPrefs.apiKey ?: throw IllegalStateException("API Key not available.")
        remoteDataSource.sendPoLToken(token, apiKey)
    }

    override suspend fun submitSecureAck(ackRequest: AckRequestDto) {
        val apiKey = userPrefs.apiKey ?: throw IllegalStateException("API Key not available.")
        remoteDataSource.postAck(apiKey, ackRequest)
    }

    override suspend fun getPayloadsForDelivery(): List<EncryptedPayloadDto> {
        val apiKey = userPrefs.apiKey ?: throw IllegalStateException("API Key not available.")
        return remoteDataSource.getPayloads(apiKey).payloads
    }
}