package ch.drcookie.polaris_sdk.model

import ch.drcookie.polaris_sdk.protocol.model.Constants
import ch.drcookie.polaris_sdk.util.UByteArrayBase64Serializer
import ch.drcookie.polaris_sdk.protocol.model.PoLRequest
import ch.drcookie.polaris_sdk.protocol.model.PoLResponse
import kotlinx.serialization.Serializable

/**
 * Represents a PoL token
 *
 * @property flags Protocol-specific flags from the transaction.
 * @property phoneId The unique ID of the phone that initiated the transaction.
 * @property beaconId The unique ID of the beacon that responded to the transaction.
 * @property beaconCounter The beacon's internal counter at the time of the transaction. This acts as a form of timestamp.
 * @property nonce The random nonce used to prevent replay attacks for this specific transaction.
 * @property phonePk The public key of the phone.
 * @property beaconPk The public key of the beacon.
 * @property phoneSig The phone's signature over the request data.
 * @property beaconSig The beacon's signature over the response data.
 */
@Serializable
@OptIn(ExperimentalUnsignedTypes::class)
public data class PoLToken(
    public val flags: UByte,
    public val phoneId: ULong,
    public val beaconId: UInt,
    public val beaconCounter: ULong,
    @Serializable(with = UByteArrayBase64Serializer::class)
    public val nonce: UByteArray,
    @Serializable(with = UByteArrayBase64Serializer::class)
    public val phonePk: UByteArray,
    @Serializable(with = UByteArrayBase64Serializer::class)
    public val beaconPk: UByteArray,
    @Serializable(with = UByteArrayBase64Serializer::class)
    public val phoneSig: UByteArray,
    @Serializable(with = UByteArrayBase64Serializer::class)
    public val beaconSig: UByteArray
) {
    init {
        require(nonce.size == Constants.PROTOCOL_NONCE)
        require(phonePk.size == Constants.ED25519_PK)
        require(phoneSig.size == Constants.SIG)
        require(beaconSig.size == Constants.SIG)
    }

    public companion object {

        /**
         * Factory method to create a [PoLToken] from a transaction.
         *
         * @param request The original, signed [PoLRequest] sent by the phone.
         * @param response The verified [PoLResponse] received from the beacon.
         * @param beaconPk The public key of the beacon involved in the transaction.
         * @return A new, fully populated [PoLToken] instance.
         * @throws IllegalArgumentException if the provided [request] is not signed.
         */
        public fun create(request: PoLRequest, response: PoLResponse, beaconPk: UByteArray): PoLToken {
            val pSig = request.phoneSig ?: throw IllegalArgumentException("PoLRequest must be signed to create a PoLToken")
            return PoLToken(
                flags = request.flags,
                phoneId = request.phoneId,
                beaconId = response.beaconId,
                beaconCounter = response.counter,
                nonce = request.nonce,
                phonePk = request.phonePk,
                beaconPk = beaconPk,
                phoneSig = pSig,
                beaconSig = response.beaconSig,
            )
        }
    }

    public override fun hashCode(): Int {
        var result = flags.hashCode()
        result = 31 * result + phoneId.hashCode()
        result = 31 * result + nonce.contentHashCode()
        result = 31 * result + phonePk.contentHashCode()
        result = 31 * result + beaconPk.contentHashCode()
        result = 31 * result + phoneSig.contentHashCode()
        result = 31 * result + beaconId.hashCode()
        result = 31 * result + beaconCounter.hashCode()
        result = 31 * result + beaconSig.contentHashCode()
        return result
    }

    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as PoLToken

        if (flags != other.flags) return false
        if (phoneId != other.phoneId) return false
        if (beaconId != other.beaconId) return false
        if (beaconCounter != other.beaconCounter) return false
        if (!nonce.contentEquals(other.nonce)) return false
        if (!phonePk.contentEquals(other.phonePk)) return false
        if (!beaconPk.contentEquals(other.beaconPk)) return false
        if (!phoneSig.contentEquals(other.phoneSig)) return false
        if (!beaconSig.contentEquals(other.beaconSig)) return false

        return true
    }
}