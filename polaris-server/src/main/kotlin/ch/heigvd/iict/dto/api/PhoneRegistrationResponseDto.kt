package ch.heigvd.iict.dto.api

import kotlinx.serialization.Serializable

@Serializable
data class PhoneRegistrationResponseDto(
    val message: String,
    val assignedPhoneId: Long,
    val apiKey: String,
    val beacons: BeaconProvisioningListDto
)
