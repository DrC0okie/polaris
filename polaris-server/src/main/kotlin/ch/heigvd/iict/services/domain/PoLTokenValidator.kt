package ch.heigvd.iict.services.domain

import ch.heigvd.iict.dto.api.PoLTokenDto
import ch.heigvd.iict.entities.Beacon
import ch.heigvd.iict.entities.RegisteredPhone
import ch.heigvd.iict.repositories.PoLTokenRecordRepository
import ch.heigvd.iict.services.crypto.CryptoService
import com.ionspin.kotlin.crypto.util.toHexString
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

@OptIn(ExperimentalUnsignedTypes::class)
@ApplicationScoped
class PoLTokenValidator @Inject constructor(
    private val crypto: CryptoService,
    private val recordRepo: PoLTokenRecordRepository
) {
    fun validate(dto: PoLTokenDto, phone: RegisteredPhone, beacon: Beacon): List<String> {
        val errs = mutableListOf<String>()

        // phonePK matches registered
        if (!dto.phonePk.contentEquals(phone.publicKey.asUByteArray())) {
            errs += "Phone public key mismatch"
        }

        // verify phone signature
        val phoneMsg = SignedPayload.forPhone(dto)
        if (!crypto.verifyEd25519Signature(dto.phoneSig, phoneMsg, dto.phonePk)) {
            errs += "Invalid phone signature"
        }

        // beacon exists & PK matches
        if (!dto.beaconPk.contentEquals(beacon.publicKey.asUByteArray())) {
            errs += "Beacon public key mismatch"
        }

        // verify beacon signature
        val beaconMsg = SignedPayload.forBeacon(dto)
        if (!crypto.verifyEd25519Signature(dto.beaconSig, beaconMsg, dto.beaconPk)) {
            errs += "Invalid beacon signature"
        }

        // counter monotonicity
        if (dto.beaconCounter < beacon.lastKnownCounter.toULong()) {
            errs += "Stale beacon counter"
        }

        // replay check
        if (recordRepo.exists(beacon, dto.beaconCounter.toLong(), dto.nonce.toHexString())) {
            errs += "Duplicate token (replay)"
        }

        return errs
    }
}