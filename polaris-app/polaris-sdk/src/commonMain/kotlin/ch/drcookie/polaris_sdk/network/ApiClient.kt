package ch.drcookie.polaris_sdk.network

import ch.drcookie.polaris_sdk.model.PoLToken
import ch.drcookie.polaris_sdk.model.dto.AckRequestDto
import ch.drcookie.polaris_sdk.model.dto.BeaconProvisioningDto
import ch.drcookie.polaris_sdk.model.dto.EncryptedPayloadDto
import ch.drcookie.polaris_sdk.model.dto.PhoneRegistrationRequestDto

interface ApiClient {
    var knownBeacons: List<BeaconProvisioningDto>

    /**
     * Returns the unique ID assigned to this phone by the backend.
     * @return The phone's ID, or a default value (e.g., -1L) if not registered.
     */
    fun getPhoneId(): Long

    suspend fun registerPhone(request: PhoneRegistrationRequestDto): List<BeaconProvisioningDto>
    suspend fun submitPoLToken(token: PoLToken)
    suspend fun getPayloadsForDelivery(): List<EncryptedPayloadDto>
    suspend fun submitSecureAck(request: AckRequestDto)
}