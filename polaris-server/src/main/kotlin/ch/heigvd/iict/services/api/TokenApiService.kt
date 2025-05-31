package ch.heigvd.iict.services.api

import ch.heigvd.iict.entities.Beacon
import ch.heigvd.iict.entities.PoLTokenRecord
import ch.heigvd.iict.entities.RegisteredPhone
import ch.heigvd.iict.repositories.BeaconRepository
import ch.heigvd.iict.repositories.PoLTokenRecordRepository
import ch.heigvd.iict.repositories.RegisteredPhoneRepository
import ch.heigvd.iict.dto.api.PoLTokenDto
import ch.heigvd.iict.dto.api.PoLTokenValidationResultDto
import ch.heigvd.iict.services.core.CryptoService
import ch.heigvd.iict.services.core.PoLUtils.toUByteArrayLE
import ch.heigvd.iict.services.core.PoLUtils.toHexString
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.time.Instant

@OptIn(ExperimentalUnsignedTypes::class)
@ApplicationScoped
class TokenApiService {

    @Inject
    private lateinit var beaconRepository: BeaconRepository

    @Inject
    private lateinit var phoneRepository: RegisteredPhoneRepository

    @Inject
    private lateinit var tokenRecordRepository: PoLTokenRecordRepository

    @Inject
    private lateinit var cryptoService: CryptoService

    @Transactional
    fun processAndValidatePoLToken(tokenDto: PoLTokenDto, clientUserAgent: String?): PoLTokenValidationResultDto {
        val receivedAt = Instant.now()
        var validationError: String? = null
        var isValidOverall = true

        val phoneEntity: RegisteredPhone = try {
            phoneRepository.findOrCreate(tokenDto.phoneId.toLong(), tokenDto.phonePk, clientUserAgent, receivedAt)
        } catch (e: IllegalStateException) {
            Log.warn("Failed to find/create phone: ${e.message}")
            return PoLTokenValidationResultDto(false, "Phone registration conflict: ${e.message}", null)
        }

        val beaconEntity: Beacon? = beaconRepository.findByBeaconTechnicalId(tokenDto.beaconId.toInt()) // Assumant beaconId DTO est UInt
        if (beaconEntity == null) {
            Log.warn("Beacon not found with technical ID: ${tokenDto.beaconId}")
            return PoLTokenValidationResultDto(false, "Beacon not found", null)
        }

        val phoneSignedData = reconstructPhoneSignedData(tokenDto)

        val isPhoneSignatureValid = cryptoService.verifyEd25519Signature(
            signature = tokenDto.phoneSig.asUByteArray(),
            message = phoneSignedData,
            publicKey = tokenDto.phonePk.asUByteArray()
        )
        if (!isPhoneSignatureValid) {
            Log.warn("Phone signature verification failed for token from phoneId: ${tokenDto.phoneId}")
            validationError = appendError(validationError, "Invalid phone signature.")
            isValidOverall = false
        }

        val beaconSignedData = reconstructBeaconSignedData(tokenDto)

        if (!tokenDto.beaconPk.contentEquals(beaconEntity.publicKey)) {
            Log.warn("Beacon public key mismatch for beaconId: ${tokenDto.beaconId}. Token PK: ${tokenDto.beaconPk.toUByteArray().toHexString()}, DB PK: ${beaconEntity.publicKey.toUByteArray().toHexString()}")
            validationError = appendError(validationError, "Beacon public key mismatch.")
            isValidOverall = false
        }

        val isBeaconSignatureValid = cryptoService.verifyEd25519Signature(
            signature = tokenDto.beaconSig.asUByteArray(),
            message = beaconSignedData,
            publicKey = tokenDto.beaconPk.asUByteArray()
        )
        if (!isBeaconSignatureValid) {
            Log.warn("Beacon signature verification failed for token from beaconId: ${tokenDto.beaconId}")
            validationError = appendError(validationError, "Invalid beacon signature.")
            isValidOverall = false
        }

        if (tokenDto.beaconCounter < beaconEntity.lastKnownCounter.toULong()) {
            Log.warn("Stale beacon counter for beaconId: ${tokenDto.beaconId}. Token: ${tokenDto.beaconCounter}, DB: ${beaconEntity.lastKnownCounter}")
            validationError = appendError(validationError, "Stale beacon counter.")
            isValidOverall = false
        }

        val nonceAsHex = tokenDto.nonce.toUByteArray().toHexString() // Convertir le nonce binaire en hex pour la recherche
        if (tokenRecordRepository.exists(beaconEntity, tokenDto.beaconCounter.toLong(), nonceAsHex)) {
            Log.warn("Duplicate PoLToken detected for beaconId: ${tokenDto.beaconId}, counter: ${tokenDto.beaconCounter}, nonce: $nonceAsHex")
            validationError = appendError(validationError, "Duplicate token (replay).")
            isValidOverall = false
        }

        val tokenRecord = PoLTokenRecord().apply {
            this.flags = tokenDto.flags.toByte()
            this.phone = phoneEntity
            this.beacon = beaconEntity
            this.beaconCounter = tokenDto.beaconCounter.toLong()
            this.nonceHex = nonceAsHex
            this.phonePkUsed = tokenDto.phonePk
            this.beaconPkUsed = tokenDto.beaconPk
            this.phoneSig = tokenDto.phoneSig
            this.beaconSig = tokenDto.beaconSig
            this.isValid = isValidOverall
            this.validationError = validationError
            this.receivedAt = receivedAt
        }
        tokenRecordRepository.persist(tokenRecord)

        Log.info("Processed PoLToken for phoneId: ${tokenDto.phoneId}, beaconId: ${tokenDto.beaconId}. Valid: $isValidOverall")

        if (isValidOverall && tokenDto.beaconCounter.toLong() > beaconEntity.lastKnownCounter) {
            beaconEntity.lastKnownCounter = tokenDto.beaconCounter.toLong()
            // beaconRepository.persist(beaconEntity) // Panache gère la persistance des entités managées dans une transaction
        }

        return PoLTokenValidationResultDto(isValidOverall, validationError, tokenRecord.id)
    }

    private fun appendError(existingError: String?, newError: String): String {
        return if (existingError == null) newError else "$existingError $newError"
    }

    private fun reconstructPhoneSignedData(tokenDto: PoLTokenDto): UByteArray {
        val buffer = UByteArray(1 + 8 + 4 + tokenDto.nonce.size + tokenDto.phonePk.size)
        var offset = 0
        buffer[offset] = tokenDto.flags; offset += 1
        tokenDto.phoneId.toUByteArrayLE().copyInto(buffer, offset); offset += 8
        tokenDto.beaconId.toUByteArrayLE().copyInto(buffer, offset); offset += 4
        tokenDto.nonce.toUByteArray().copyInto(buffer, offset); offset += tokenDto.nonce.size
        tokenDto.phonePk.toUByteArray().copyInto(buffer, offset)
        return buffer
    }

    private fun reconstructBeaconSignedData(tokenDto: PoLTokenDto): UByteArray {
        val buffer = UByteArray(1 + 4 + 8 + tokenDto.nonce.size + 8 + tokenDto.phonePk.size + tokenDto.phoneSig.size)
        var offset = 0

        buffer[offset] = tokenDto.flags; offset += 1
        tokenDto.beaconId.toUByteArrayLE().copyInto(buffer, offset); offset += 4
        tokenDto.beaconCounter.toUByteArrayLE().copyInto(buffer, offset); offset += 8
        tokenDto.nonce.toUByteArray().copyInto(buffer, offset); offset += tokenDto.nonce.size

        tokenDto.phoneId.toUByteArrayLE().copyInto(buffer, offset); offset += 8
        tokenDto.phonePk.toUByteArray().copyInto(buffer, offset); offset += tokenDto.phonePk.size
        tokenDto.phoneSig.toUByteArray().copyInto(buffer, offset)
        return buffer
    }
}