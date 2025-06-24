package ch.drcookie.polaris_app.domain.repository

import ch.drcookie.polaris_app.domain.model.PoLToken
import ch.drcookie.polaris_app.domain.model.dto.AckRequestDto
import ch.drcookie.polaris_app.domain.model.dto.BeaconProvisioningDto
import ch.drcookie.polaris_app.domain.model.dto.EncryptedPayloadDto
import ch.drcookie.polaris_app.domain.model.dto.PhoneRegistrationRequestDto

interface AuthRepository {
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