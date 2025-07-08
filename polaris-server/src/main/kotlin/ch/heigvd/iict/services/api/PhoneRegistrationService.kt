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

/**
 * Service responsible for handling mobile device registration.
 *
 * This service manages the creation and updating of [RegisteredPhone] entities,
 * issues API keys, and provides initial provisioning data.
 */
@ApplicationScoped
@OptIn(ExperimentalUnsignedTypes::class)
class PhoneRegistrationService {

    @Inject
    private lateinit var repo: RegisteredPhoneRepository

    @Inject
    private lateinit var provisioningApiService: ProvisioningApiService

    /**
     * Registers a new phone or updates an existing one based on its public key.
     *
     * If a phone with the given public key already exists, its `lastSeenAt` and `userAgent`
     * fields are updated. If not, a new [RegisteredPhone] record is created with a newly
     * generated API key.
     *
     * In both cases, the response includes the phone's API key and the current list of
     * provisioned beacons.
     *
     * @param request The DTO containing the phone's public key and metadata.
     * @return A DTO containing the registration result, including the API key and beacon list.
     */
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