package ch.drcookie.polaris_app.dto

import kotlinx.serialization.Serializable

@Serializable
data class PhoneRegistrationResponseDto(
    val message: String,
    val assignedPhoneId: Long?,
    val apiKey: String,
    val beacons: BeaconProvisioningListDto
)