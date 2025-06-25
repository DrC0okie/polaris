package ch.drcookie.polaris_sdk.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class PhoneRegistrationResponseDto(
    val message: String,
    val assignedPhoneId: Long?,
    val apiKey: String,
    val beacons: BeaconProvisioningListDto
)