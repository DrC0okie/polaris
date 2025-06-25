package ch.drcookie.polaris_sdk.network.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class PhoneRegistrationResponseDto(
    internal val message: String,
    internal val assignedPhoneId: Long?,
    internal val apiKey: String,
    internal val beacons: BeaconProvisioningListDto
)