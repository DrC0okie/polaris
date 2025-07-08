package ch.heigvd.iict.services.token

import ch.heigvd.iict.dto.api.PoLTokenDto
import ch.heigvd.iict.entities.PoLTokenRecord
import ch.heigvd.iict.entities.RegisteredPhone
import ch.heigvd.iict.entities.Beacon
import jakarta.enterprise.context.ApplicationScoped
import ch.heigvd.iict.util.PoLUtils.toHexString
import java.time.Instant

/**
 * A service responsible for assembling a [PoLTokenRecord] entity from a DTO and validation results.
 * This class handles the mapping from the transport layer object to the persistence layer entity.
 */
@OptIn(ExperimentalUnsignedTypes::class)
@ApplicationScoped
class PoLTokenAssembler {

    /**
     * Assembles a [PoLTokenDto] and its associated validation data into a persistable [PoLTokenRecord].
     *
     * @param dto The raw token data submitted by the phone.
     * @param phone The [RegisteredPhone] entity that submitted the token.
     * @param beacon The [Beacon] entity that issued the token.
     * @param isValid A boolean indicating the result of the validation checks.
     * @param errors A list of validation error messages, if any.
     * @return A fully populated [PoLTokenRecord] instance, ready to be persisted.
     */
    fun assemble( dto: PoLTokenDto, phone: RegisteredPhone, beacon: Beacon, isValid: Boolean, errors: List<String>
    ): PoLTokenRecord {
        val now = Instant.now()
        val rec = PoLTokenRecord().apply {
            flags = dto.flags.toByte()
            this.phone = phone
            this.beacon = beacon
            beaconCounter = dto.beaconCounter.toLong()
            nonceHex = dto.nonce.toHexString()
            phonePkUsed = dto.phonePk.asByteArray()
            beaconPkUsed = dto.beaconPk.asByteArray()
            phoneSig = dto.phoneSig.asByteArray()
            beaconSig = dto.beaconSig.asByteArray()
            this.isValid = isValid
            validationError = errors.joinToString("; ")
            receivedAt = now
        }
        return rec
    }
}