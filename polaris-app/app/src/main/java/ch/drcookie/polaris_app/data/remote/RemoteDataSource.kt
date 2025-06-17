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

    fun shutdown() {
        ApiService.closeClient()
    }
}