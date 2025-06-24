package ch.drcookie.polaris_sdk.data.datasource.remote

import android.os.Build
import androidx.annotation.RequiresApi
import ch.drcookie.polaris_sdk.domain.model.PoLToken
import ch.drcookie.polaris_sdk.domain.model.dto.AckRequestDto
import ch.drcookie.polaris_sdk.domain.model.dto.BeaconProvisioningListDto
import ch.drcookie.polaris_sdk.domain.model.dto.EncryptedPayloadListDto
import ch.drcookie.polaris_sdk.domain.model.dto.PhoneRegistrationRequestDto
import ch.drcookie.polaris_sdk.domain.model.dto.PhoneRegistrationResponseDto

class RemoteDataSource {
    suspend fun registerPhone(request: PhoneRegistrationRequestDto): PhoneRegistrationResponseDto {
        return ApiService.registerPhone(request)
    }

    @RequiresApi(Build.VERSION_CODES.O)
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