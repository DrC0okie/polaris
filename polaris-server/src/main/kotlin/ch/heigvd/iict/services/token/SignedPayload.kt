package ch.heigvd.iict.services.token

import ch.heigvd.iict.dto.api.PoLTokenDto
import ch.heigvd.iict.util.PoLUtils.toUByteArrayLE

/**
 * A helper object for reconstructing the byte payloads that were signed by the phone and the beacon.
 *
 * The PoL protocol specifies that signatures must cover a precise sequence of fields. This object
 * ensures that the server reconstructs this data in the exact same way the devices did.
 */
@OptIn(ExperimentalUnsignedTypes::class)
object SignedPayload {

    /**
     * Reconstructs the data payload signed by the phone.
     * The format is: `flags || phoneId || beaconId || nonce || phonePk`.
     *
     * @param dto The token DTO containing the necessary fields.
     * @return The reconstructed signed payload as a [UByteArray].
     */
    fun forPhone(dto: PoLTokenDto): UByteArray {
        with(dto) {
            val buf = UByteArray(1 + 8 + 4 + nonce.size + phonePk.size)
            var off = 0
            buf[off] = flags; off += 1
            phoneId.toUByteArrayLE().copyInto(buf, off); off += 8
            beaconId.toUByteArrayLE().copyInto(buf, off); off += 4
            nonce.copyInto(buf, off); off += nonce.size
            phonePk.copyInto(buf, off)
            return buf
        }
    }

    /**
     * Reconstructs the data payload signed by the beacon.
     * The format is: `flags || beaconId || beaconCounter || nonce || phoneId || phonePk || phoneSig`.
     *
     * @param dto The token DTO containing the necessary fields.
     * @return The reconstructed signed payload as a [UByteArray].
     */
    fun forBeacon(dto: PoLTokenDto): UByteArray {
        with(dto) {
            val buf = UByteArray(1 + 4 + 8 + nonce.size + 8 + phonePk.size + phoneSig.size)
            var off = 0
            buf[off] = flags; off += 1
            beaconId.toUByteArrayLE().copyInto(buf, off); off += 4
            beaconCounter.toUByteArrayLE().copyInto(buf, off); off += 8
            nonce.copyInto(buf, off); off += nonce.size
            phoneId.toUByteArrayLE().copyInto(buf, off); off += 8
            phonePk.copyInto(buf, off); off += phonePk.size
            phoneSig.copyInto(buf, off)
            return buf
        }
    }
}