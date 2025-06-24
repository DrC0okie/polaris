package ch.drcookie.polaris_sdk.data.repository

import ch.drcookie.polaris_sdk.data.datasource.remote.RemoteDataSource
import ch.drcookie.polaris_sdk.domain.model.PoLToken
import ch.drcookie.polaris_sdk.domain.model.dto.AckRequestDto
import ch.drcookie.polaris_sdk.domain.model.dto.BeaconProvisioningDto
import ch.drcookie.polaris_sdk.domain.model.dto.EncryptedPayloadDto
import ch.drcookie.polaris_sdk.domain.model.dto.PhoneRegistrationRequestDto
import ch.drcookie.polaris_sdk.domain.repository.AuthRepository
import ch.drcookie.polaris_sdk.domain.repository.LocalPreferences

class AuthRepositoryImpl(
    private val remoteDataSource: RemoteDataSource,
    private val userPrefs: LocalPreferences
) : AuthRepository {
    // Holds the list of beacons after a successful registration/fetch
    override var knownBeacons: List<BeaconProvisioningDto> = emptyList()

    override fun getPhoneId(): Long {
        // It simply delegates the call to the underlying preference storage.
        return userPrefs.phoneId
    }

    override suspend fun registerPhone(request: PhoneRegistrationRequestDto): List<BeaconProvisioningDto> {
        val response = remoteDataSource.registerPhone(request)
        userPrefs.apiKey = response.apiKey
        userPrefs.phoneId = response.assignedPhoneId ?: -1L
        knownBeacons = response.beacons.beacons
        return knownBeacons
    }

    override suspend fun submitPoLToken(token: PoLToken) {
        val apiKey = userPrefs.apiKey ?: throw IllegalStateException("API Key not available.")
        remoteDataSource.sendPoLToken(token, apiKey)
    }

    override suspend fun submitSecureAck(request: AckRequestDto) {
        val apiKey = userPrefs.apiKey ?: throw IllegalStateException("API Key not available.")
        remoteDataSource.postAck(apiKey, request)
    }

    override suspend fun getPayloadsForDelivery(): List<EncryptedPayloadDto> {
        val apiKey = userPrefs.apiKey ?: throw IllegalStateException("API Key not available.")
        return remoteDataSource.getPayloads(apiKey).payloads
    }
}