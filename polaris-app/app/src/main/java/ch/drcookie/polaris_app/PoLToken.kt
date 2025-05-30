package ch.drcookie.polaris_app

import kotlinx.serialization.Serializable

@Serializable
@OptIn(ExperimentalUnsignedTypes::class)
data class PoLToken(
    val flags: UByte,
    val phoneId: ULong,
    @Serializable(with = UByteArrayBase64Serializer::class)
    val requestNonce: UByteArray,
    @Serializable(with = UByteArrayBase64Serializer::class)
    val phonePk: UByteArray,
    @Serializable(with = UByteArrayBase64Serializer::class)
    val phoneSig: UByteArray,
    val responseBeaconId: UInt,
    val beaconCounter: ULong,
    @Serializable(with = UByteArrayBase64Serializer::class)
    val beaconSig: UByteArray,
) {
    init {
        require(requestNonce.size == PoLConstants.PROTOCOL_NONCE_SIZE)
        require(phonePk.size == PoLConstants.ED25519_PK_SIZE)
        require(phoneSig.size == PoLConstants.SIG_SIZE)
        require(beaconSig.size == PoLConstants.SIG_SIZE)
    }

    companion object {
        fun create(request: PoLRequest, response: PoLResponse): PoLToken {
            val pSig = request.phoneSig ?: throw IllegalArgumentException("PoLRequest must be signed to create a PoLToken")
            return PoLToken(
                flags = request.flags,
                phoneId = request.phoneId,
                requestNonce = request.nonce,
                phonePk = request.phonePk,
                phoneSig = pSig,
                responseBeaconId = response.beaconId,
                beaconCounter = response.counter,
                beaconSig = response.beaconSig,
            )
        }
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PoLToken

        if (flags != other.flags) return false
        if (phoneId != other.phoneId) return false
        if (!requestNonce.contentEquals(other.requestNonce)) return false
        if (!phonePk.contentEquals(other.phonePk)) return false
        if (!phoneSig.contentEquals(other.phoneSig)) return false
        if (responseBeaconId != other.responseBeaconId) return false
        if (beaconCounter != other.beaconCounter) return false
        if (!beaconSig.contentEquals(other.beaconSig)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = flags.hashCode()
        result = 31 * result + phoneId.hashCode()
        result = 31 * result + requestNonce.contentHashCode()
        result = 31 * result + phonePk.contentHashCode()
        result = 31 * result + phoneSig.contentHashCode()
        result = 31 * result + responseBeaconId.hashCode()
        result = 31 * result + beaconCounter.hashCode()
        result = 31 * result + beaconSig.contentHashCode()
        return result
    }
}