package ch.heigvd.iict.services.token

import ch.heigvd.iict.dto.api.PoLTokenDto
import ch.heigvd.iict.entities.PoLTokenRecord
import ch.heigvd.iict.entities.RegisteredPhone
import ch.heigvd.iict.entities.Beacon
import jakarta.enterprise.context.ApplicationScoped
import ch.heigvd.iict.util.PoLUtils.toHexString
import java.time.Instant

@OptIn(ExperimentalUnsignedTypes::class)
@ApplicationScoped
class PoLTokenAssembler {

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