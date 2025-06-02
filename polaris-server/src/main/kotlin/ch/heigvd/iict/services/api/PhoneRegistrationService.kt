package ch.heigvd.iict.services.api

import ch.heigvd.iict.repositories.RegisteredPhoneRepository
import ch.heigvd.iict.dto.api.PhoneRegistrationRequestDto
import ch.heigvd.iict.dto.api.PhoneRegistrationResponseDto
import ch.heigvd.iict.dto.api.BeaconProvisioningListDto
import ch.heigvd.iict.entities.RegisteredPhone
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.time.Instant
import ch.heigvd.iict.util.PoLUtils.toHexString

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

        // Vérifier si la clé publique est déjà enregistrée
        val existingPhoneByPk = phoneRepository.findByPublicKey(publicKeyBytes)
        if (existingPhoneByPk != null) {
            if (existingPhoneByPk.phoneTechnicalId == technicalId) {
                // Le téléphone avec cette PK est déjà enregistré avec le même ID technique. C'est OK.
                Log.info("Phone with PK ${request.publicKey.toHexString()} and ID $technicalId already registered. Updating info.")
                existingPhoneByPk.userAgent = buildUserAgent(request)
                existingPhoneByPk.lastSeenAt = Instant.now()
            } else {
                throw RegistrationConflictException(
                    "Public key ${request.publicKey.toHexString()} already registered with a different phoneTechnicalId: ${existingPhoneByPk.phoneTechnicalId}"
                )
            }
        }

        val existingPhoneById = phoneRepository.findByPhoneTechnicalId(technicalId)
        if (existingPhoneById != null) {
            if (!existingPhoneById.publicKey.contentEquals(publicKeyBytes)) {
                throw RegistrationConflictException(
                    "PhoneTechnicalId $technicalId already registered with a different public key."
                )
            }
            if (existingPhoneByPk == null) {
                // Ce cas ne devrait pas arriver si la PK est unique.
                existingPhoneById.userAgent = buildUserAgent(request)
                existingPhoneById.lastSeenAt = Instant.now()
            }
        }

        // Si on arrive ici et qu'aucun téléphone existant n'a été trouvé/géré, on en crée un nouveau.
        if (existingPhoneByPk == null && existingPhoneById == null) {
            Log.info("Registering new phone with ID $technicalId and PK ${request.publicKey.toHexString()}")
            RegisteredPhone().apply {
                this.phoneTechnicalId = technicalId
                this.publicKey = publicKeyBytes
                this.userAgent = buildUserAgent(request)
                // createdAt, updatedAt gérés par @PrePersist
                this.lastSeenAt = Instant.now() // Mettre à jour lastSeenAt lors de l'enregistrement
            }.also { phoneRepository.persist(it) }
        }

        // Récupérer la liste des balises pour le provisioning
        val beacons = BeaconProvisioningListDto(provisioningApiService.getBeaconsForProvisioning())

        return PhoneRegistrationResponseDto(
            "Phone registered successfully.",
            technicalId,
            beacons
        )
    }

    private fun buildUserAgent(request: PhoneRegistrationRequestDto): String? {
        val parts = listOfNotNull(request.deviceModel, request.osVersion, request.appVersion)
        return if (parts.isNotEmpty()) parts.joinToString(" / ") else null
    }
}