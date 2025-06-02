package ch.heigvd.iict.services.api

import ch.heigvd.iict.dto.api.BeaconProvisioningListDto
import ch.heigvd.iict.repositories.RegisteredPhoneRepository
import ch.heigvd.iict.dto.api.PhoneRegistrationRequestDto
import ch.heigvd.iict.dto.api.PhoneRegistrationResponseDto
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.time.Instant
import ch.heigvd.iict.util.PoLUtils.toHexString
import java.util.UUID

@ApplicationScoped
@OptIn(ExperimentalUnsignedTypes::class)
class PhoneRegistrationService {

    @Inject
    private lateinit var phoneRepository: RegisteredPhoneRepository

    @Inject
    private lateinit var provisioningApiService: ProvisioningApiService

    class RegistrationConflictException(message: String) : RuntimeException(message)

    @Transactional
    fun registerPhoneAndGetBeacons(request: PhoneRegistrationRequestDto): PhoneRegistrationResponseDto {
        val technicalId = request.phoneTechnicalId.toLong()
        val publicKeyBytes = request.publicKey.asByteArray()
        val now = Instant.now()

        var phone = phoneRepository.findOrCreate( technicalId, publicKeyBytes, buildUserAgent(request), now )
        var generatedApiKey: String? = null

        if (phone.id == null) {
            Log.info("Registering new phone. ID: $technicalId, PK: ${request.publicKey.toHexString()}")
            generatedApiKey = generateNewApiKey()
            phone.apiKey = generatedApiKey
            phoneRepository.persist(phone)
        } else {
            Log.info("Phone $technicalId (PK: ${request.publicKey.toHexString()}) re-registering or updating info.")
        }

        val beacons = BeaconProvisioningListDto(provisioningApiService.getBeaconsForProvisioning())

        return PhoneRegistrationResponseDto(
            if (generatedApiKey != null) "Phone registered successfully." else "Phone information updated.",
            phone.phoneTechnicalId,
            generatedApiKey ?: phone.apiKey,
            beacons
        )
    }

    private fun generateNewApiKey(): String {
        val part1 = UUID.randomUUID().toString().replace("-", "")
        val part2 = UUID.randomUUID().toString().replace("-", "")
        return part1 + part2
    }

    private fun buildUserAgent(request: PhoneRegistrationRequestDto): String? {
        val parts = listOfNotNull(request.deviceModel, request.osVersion, request.appVersion)
        return if (parts.isNotEmpty()) parts.joinToString(" / ") else null
    }
}