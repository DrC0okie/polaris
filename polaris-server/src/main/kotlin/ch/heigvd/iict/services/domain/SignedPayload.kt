package ch.heigvd.iict.services.domain

import ch.heigvd.iict.dto.api.PoLTokenDto
import ch.heigvd.iict.util.PoLUtils.toUByteArrayLE

@OptIn(ExperimentalUnsignedTypes::class)
object SignedPayload {

    fun forPhone(dto: PoLTokenDto): UByteArray {
        with(dto){
            // 1 byte flags, 8 bytes phoneId, 4 bytes beaconId, nonce, phonePk
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

    fun forBeacon(dto: PoLTokenDto): UByteArray {
        with (dto){
            // 1 byte flags, 4 bytes beaconId, 8 bytes beaconCounter, nonce, 8 bytes phoneId, phonePk, phoneSig
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