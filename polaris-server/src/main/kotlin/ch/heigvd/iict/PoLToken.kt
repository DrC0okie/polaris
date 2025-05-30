package ch.heigvd.iict

import ch.heigvd.iict.Utils.toUByteArrayLE
import kotlinx.serialization.Serializable

@Serializable
@OptIn(ExperimentalUnsignedTypes::class)
data class PoLToken(
    val requestFlags: UByte,
    val phoneId: ULong,
    val beaconId: UInt,
    @Serializable(with = UByteArrayBase64Serializer::class)
    val nonce: UByteArray,
    @Serializable(with = UByteArrayBase64Serializer::class)
    val phonePk: UByteArray,
    @Serializable(with = UByteArrayBase64Serializer::class)
    val phoneSig: UByteArray,
    val responseFlags: UByte,
    val beaconCounter: ULong,
    @Serializable(with = UByteArrayBase64Serializer::class)
    val beaconSig: UByteArray
) {
    // Add validation in init or a separate validation method if needed
    init {
        // Basic size checks, more can be added
        require(nonce.size == PoLConstants.PROTOCOL_NONCE_SIZE) { "Invalid nonce size" }
        require(phonePk.size == PoLConstants.ED25519_PK_SIZE) { "Invalid phone PK size" }
        require(phoneSig.size == PoLConstants.SIG_SIZE) { "Invalid phone signature size" }
        require(beaconSig.size == PoLConstants.SIG_SIZE) { "Invalid beacon signature size" }
    }

    // Method to construct the data that the phone signed
    @OptIn(ExperimentalUnsignedTypes::class)
    fun getPhoneSignedData(): UByteArray {
        // This MUST exactly match PoLRequest.getSignedData() on the Android side
        val buffer = UByteArray(
            1 /*flags*/ + 8 /*phoneId*/ + 4 /*beaconId*/ +
                    PoLConstants.PROTOCOL_NONCE_SIZE + PoLConstants.ED25519_PK_SIZE
        )
        var offset = 0
        buffer[offset] = requestFlags; offset += 1
        phoneId.toUByteArrayLE().copyInto(buffer, offset); offset += 8
        beaconId.toUByteArrayLE().copyInto(buffer, offset); offset += 4 // Beacon ID from PoLToken
        nonce.copyInto(buffer, offset); offset += PoLConstants.PROTOCOL_NONCE_SIZE
        phonePk.copyInto(buffer, offset)
        return buffer
    }

    // Method to construct the data that the beacon signed
    @OptIn(ExperimentalUnsignedTypes::class)
    fun getBeaconEffectivelySignedData(): UByteArray {
        // This MUST exactly match PoLResponse.getEffectivelySignedData() on the Android/Beacon side
        val buffer = UByteArray(
            1 /*respFlags*/ + 4 /*beaconId*/ + 8 /*beaconCounter*/ +
                    PoLConstants.PROTOCOL_NONCE_SIZE /*nonce*/ +
                    8 /*phoneId*/ + PoLConstants.ED25519_PK_SIZE /*phonePk*/ +
                    PoLConstants.SIG_SIZE /*phoneSig*/
        )
        var offset = 0
        // Response part
        buffer[offset] = responseFlags; offset += 1
        beaconId.toUByteArrayLE().copyInto(buffer, offset); offset += 4
        beaconCounter.toUByteArrayLE().copyInto(buffer, offset); offset += 8
        nonce.copyInto(buffer, offset); offset += PoLConstants.PROTOCOL_NONCE_SIZE
        // Original Request part (as included by beacon)
        phoneId.toUByteArrayLE().copyInto(buffer, offset); offset += 8
        phonePk.copyInto(buffer, offset); offset += PoLConstants.ED25519_PK_SIZE
        phoneSig.copyInto(buffer, offset)
        return buffer
    }
}



