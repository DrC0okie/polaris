package ch.heigvd.iict.services.api

import ch.heigvd.iict.dto.api.BeaconProvisioningListDto
import ch.heigvd.iict.repositories.RegisteredPhoneRepository
import ch.heigvd.iict.dto.api.PhoneRegistrationRequestDto
import ch.heigvd.iict.dto.api.PhoneRegistrationResponseDto
import ch.heigvd.iict.entities.RegisteredPhone
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.time.Instant
import java.util.UUID

@ApplicationScoped
@OptIn(ExperimentalUnsignedTypes::class)
class PhoneRegistrationService {

    @Inject
    private lateinit var repo: RegisteredPhoneRepository

    @Inject
    private lateinit var provisioningApiService: ProvisioningApiService

    @Transactional
    fun register(request: PhoneRegistrationRequestDto): PhoneRegistrationResponseDto {
        val now = Instant.now()

        // lookup by public key only
        val existing = repo.findByPublicKey(request.publicKey.asByteArray())
        val phone = if (existing != null) {
            existing.apply {
                lastSeenAt = now
                userAgent  = buildUserAgent(request)
                updatedAt  = now
            }
        } else {
            RegisteredPhone().apply {
                publicKey = request.publicKey.asByteArray()
                apiKey    = generateNewApiKey()
                userAgent = buildUserAgent(request)
                createdAt = now
                updatedAt = now
            }.also {
                repo.persist(it)
            }
        }

        val beacons = provisioningApiService.getBeaconsForProvisioning()
        return PhoneRegistrationResponseDto(
            message          = if (existing == null) "Phone registered successfully." else "Phone information updated.",
            assignedPhoneId  = phone.id!!,
            apiKey           = phone.apiKey,
            beacons          = BeaconProvisioningListDto(beacons)
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