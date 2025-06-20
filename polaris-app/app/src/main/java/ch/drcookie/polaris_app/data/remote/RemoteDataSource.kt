package ch.drcookie.polaris_app.data.remote

import ch.drcookie.polaris_app.data.model.PoLToken
import ch.drcookie.polaris_app.data.model.dto.*

class RemoteDataSource {
    suspend fun registerPhone(request: PhoneRegistrationRequestDto): PhoneRegistrationResponseDto {
        return ApiService.registerPhone(request)
    }

    suspend fun fetchBeacons(apiKey: String): BeaconProvisioningListDto {
        return ApiService.fetchBeacons(apiKey)
    }

    suspend fun sendPoLToken(token: PoLToken, apiKey: String): Boolean {
        return ApiService.sendPoLToken(token, apiKey)
    }

    suspend fun getPayloads(apiKey: String): EncryptedPayloadListDto {
        return ApiService.getPayloads(apiKey)
    }

    suspend fun postAck(apiKey: String, request: AckRequestDto): Boolean {
        return ApiService.postAck(apiKey, request)
    }

    fun shutdown() {
        ApiService.closeClient()
    }
}