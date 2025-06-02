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
import ch.heigvd.iict.util.PoLUtils.toUByteArrayLE
import ch.heigvd.iict.util.PoLUtils.toHexString
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
    fun processAndValidatePoLToken(tokenDto: PoLTokenDto, clientUserAgent: String?, apiKey: String): PoLTokenValidationResultDto {
        val receivedAt = Instant.now()
        var validationError: String? = null
        var isValidOverall = true

        with(tokenDto) {
            val phoneEntity: RegisteredPhone = phoneRepository.findByApiKey(apiKey) // Nouvelle méthode dans RegisteredPhoneRepository
                ?: return PoLTokenValidationResultDto(false, "Invalid API Key or phone not registered.", null)

            if (!phonePk.contentEquals(phoneEntity.publicKey.asUByteArray())) {
                Log.warn("PoLToken's phonePk does not match registered PK for phoneId ${phoneEntity.phoneTechnicalId} authenticated via API key.")
                validationError = appendError(validationError, "Token public key does not match registered key.")
                isValidOverall = false
                return PoLTokenValidationResultDto(false, "Phone public key and Api key mismatch", null)
            }

            val beacon: Beacon? = beaconRepository.findByBeaconTechnicalId(beaconId.toInt())
            if (beacon == null) {
                Log.warn("Beacon not found with ID: $beaconId")
                return PoLTokenValidationResultDto(false, "Beacon not found", null)
            }

            val phoneSignedData = reconstructPhoneSignedData(this)
            if (!cryptoService.verifyEd25519Signature(phoneSig, phoneSignedData, phonePk)) {
                Log.warn("Phone signature verification failed for token from phoneId: $phoneId")
                validationError = appendError(validationError, "Invalid phone signature.")
                isValidOverall = false
            }

            if (!beaconPk.contentEquals(beacon.publicKey.asUByteArray())) {
                Log.warn("Beacon public key mismatch for beaconId: $beaconId. Token PK: ${beaconPk.toHexString()}, DB PK: ${beacon.publicKey.toHexString()}")
                validationError = appendError(validationError, "Beacon public key mismatch.")
                isValidOverall = false
            }

            val beaconSignedData = reconstructBeaconSignedData(this)
            val isBeaconSignatureValid = cryptoService.verifyEd25519Signature(beaconSig, beaconSignedData, beaconPk)
            if (!isBeaconSignatureValid) {
                Log.warn("Beacon signature verification failed for token from beaconId: $beaconId")
                validationError = appendError(validationError, "Invalid beacon signature.")
                isValidOverall = false
            }

            if (beaconCounter < beacon.lastKnownCounter.toULong()) {
                Log.warn("Stale beacon counter for beaconId: $beaconId. Token: $beaconCounter} DB: ${beacon.lastKnownCounter}")
                validationError = appendError(validationError, "Stale beacon counter.")
                isValidOverall = false
            }

            val nonceAsHex = nonce.toHexString()
            if (tokenRecordRepository.exists(beacon, beaconCounter.toLong(), nonceAsHex)) {
                Log.warn("Duplicate PoLToken detected for beaconId: $beaconId, counter: $beaconCounter, nonce: $nonceAsHex")
                validationError = appendError(validationError, "Duplicate token (replay).")
                isValidOverall = false
                return PoLTokenValidationResultDto(isValidOverall, validationError, null)
            }

            val tokenRecord = PoLTokenRecord().apply {
                this.flags = this@with.flags.toByte()
                this.phone = phoneEntity
                this.beacon = beacon
                this.beaconCounter = this@with.beaconCounter.toLong()
                this.nonceHex = nonceAsHex
                this.phonePkUsed = phonePk.asByteArray()
                this.beaconPkUsed = beaconPk.asByteArray()
                this.phoneSig = this@with.phoneSig.asByteArray()
                this.beaconSig = this@with.beaconSig.asByteArray()
                this.isValid = isValidOverall
                this.validationError = validationError
                this.receivedAt = receivedAt
            }
            try {
                tokenRecordRepository.persist(tokenRecord)
                Log.info("Processed PoLToken. phoneId: $phoneId, beaconId: $beaconId. Valid: $isValidOverall. Record ID: ${tokenRecord.id}")

                if (isValidOverall && beaconCounter.toLong() > beacon.lastKnownCounter) {
                    beacon.lastKnownCounter = beaconCounter.toLong()
                }

                return PoLTokenValidationResultDto(isValidOverall, validationError, tokenRecord.id)

            } catch (e: Exception) {
                Log.error("Failed to persist PoLTokenRecord: ${e.message}", e)
                if (e.message?.contains("idx_poltoken_unique_proof") == true || e.cause?.message?.contains("idx_poltoken_unique_proof") == true ) {
                    validationError = appendError(validationError, "Duplicate token (database constraint).")
                    return PoLTokenValidationResultDto(false, validationError, null)
                }
                throw e
            }
        }
    }

    private fun appendError(existingError: String?, newError: String): String {
        return if (existingError == null) newError else "$existingError $newError"
    }

    private fun reconstructPhoneSignedData(tokenDto: PoLTokenDto): UByteArray {
        with(tokenDto) {
            val buffer = UByteArray(1 + 8 + 4 + nonce.size + phonePk.size)
            var offset = 0
            buffer[offset] = flags; offset += 1
            phoneId.toUByteArrayLE().copyInto(buffer, offset); offset += 8
            beaconId.toUByteArrayLE().copyInto(buffer, offset); offset += 4
            nonce.copyInto(buffer, offset); offset += nonce.size
            phonePk.copyInto(buffer, offset)
            return buffer
        }
    }

    private fun reconstructBeaconSignedData(tokenDto: PoLTokenDto): UByteArray {
        with(tokenDto) {
            val buffer = UByteArray(1 + 4 + 8 + nonce.size + 8 + phonePk.size + phoneSig.size)
            var offset = 0
            buffer[offset] = flags; offset += 1
            beaconId.toUByteArrayLE().copyInto(buffer, offset); offset += 4
            beaconCounter.toUByteArrayLE().copyInto(buffer, offset); offset += 8
            nonce.copyInto(buffer, offset); offset += nonce.size
            phoneId.toUByteArrayLE().copyInto(buffer, offset); offset += 8
            phonePk.copyInto(buffer, offset); offset += phonePk.size
            phoneSig.copyInto(buffer, offset)
            return buffer
        }
    }
}