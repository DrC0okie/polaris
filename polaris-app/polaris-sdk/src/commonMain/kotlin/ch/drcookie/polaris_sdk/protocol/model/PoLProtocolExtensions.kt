package ch.drcookie.polaris_sdk.protocol.model

import ch.drcookie.polaris_sdk.protocol.model.PoLResponse.Companion.EFFECTIVE_SIGNED_DATA_SIZE
import ch.drcookie.polaris_sdk.protocol.model.PoLResponse.Companion.PACKED_SIZE
import ch.drcookie.polaris_sdk.util.ByteConversionUtils.toUByteArrayLE
import ch.drcookie.polaris_sdk.util.ByteConversionUtils.toUIntLE
import ch.drcookie.polaris_sdk.util.ByteConversionUtils.toULongLE

@OptIn(ExperimentalUnsignedTypes::class)
public fun PoLRequest.getSignedData(): UByteArray {
    val buffer = UByteArray(PoLRequest.SIGNED_DATA_SIZE)
    var offset = 0
    buffer[offset] = flags
    offset += 1
    phoneId.toUByteArrayLE().copyInto(buffer, offset)
    offset += 8
    beaconId.toUByteArrayLE().copyInto(buffer, offset)
    offset += 4
    nonce.copyInto(buffer, offset)
    offset += Constants.PROTOCOL_NONCE
    phonePk.copyInto(buffer, offset) // offset += PoLConstants.ED25519_PK_SIZE
    return buffer
}

@OptIn(ExperimentalUnsignedTypes::class)
public fun poLRequestFromBytes(data: ByteArray): PoLRequest? {
    if (data.size < PoLRequest.Companion.PACKED_SIZE) return null
    var offset = 0
    val uData = data.toUByteArray() // We work with UByteArrays for unsigned values

    val flags = uData[offset]
    offset += Constants.FLAGS
    val phoneId = uData.sliceArray(offset until offset + Constants.PHONE_ID).toULongLE()
    offset += Constants.PHONE_ID
    val beaconId = uData.sliceArray(offset until offset + Constants.BEACON_ID).toUIntLE()
    offset += Constants.BEACON_ID
    val nonce = uData.sliceArray(offset until offset + Constants.PROTOCOL_NONCE)
    offset += Constants.PROTOCOL_NONCE
    val phonePk = uData.sliceArray(offset until offset + Constants.ED25519_PK)
    offset += Constants.ED25519_PK
    val phoneSig = uData.sliceArray(offset until offset + Constants.SIG)
    offset += Constants.SIG // If we ever want to add something after that

    return PoLRequest(flags, phoneId, beaconId, nonce, phonePk, phoneSig)
}

@OptIn(ExperimentalUnsignedTypes::class)
public fun PoLRequest.toBytes(): ByteArray {
    val sig = phoneSig ?: throw IllegalStateException("Signature not set before serializing PoLRequest")
    val buffer = UByteArray(PoLRequest.PACKED_SIZE)
    var offset = 0

    buffer[offset] = flags; offset += 1
    phoneId.toUByteArrayLE().copyInto(buffer, offset)
    offset += 8
    beaconId.toUByteArrayLE().copyInto(buffer, offset)
    offset += 4
    nonce.copyInto(buffer, offset)
    offset += Constants.PROTOCOL_NONCE
    phonePk.copyInto(buffer, offset)
    offset += Constants.ED25519_PK
    sig.copyInto(buffer, offset) // offset += PoLConstants.SIG_SIZE

    return buffer.asByteArray()
}

// Constructs the data that the beacon *actually* signed
@OptIn(ExperimentalUnsignedTypes::class)
public fun PoLResponse.getEffectivelySignedData(originalRequest: PoLRequest): UByteArray {
    val buffer = UByteArray(EFFECTIVE_SIGNED_DATA_SIZE)
    var offset = 0

    // Response fields
    buffer[offset] = flags; offset += 1
    beaconId.toUByteArrayLE().copyInto(buffer, offset); offset += 4
    counter.toUByteArrayLE().copyInto(buffer, offset); offset += 8
    nonce.copyInto(buffer, offset); offset += Constants.PROTOCOL_NONCE // This is originalRequest.nonce

    // Original Request fields
    originalRequest.phoneId.toUByteArrayLE().copyInto(buffer, offset); offset += 8
    originalRequest.phonePk.copyInto(buffer, offset); offset += Constants.ED25519_PK
    (originalRequest.phoneSig ?: UByteArray(Constants.SIG)) // Use placeholder if null, though it should be set
        .copyInto(buffer, offset) // offset += PoLConstants.SIG_SIZE

    return buffer
}

@OptIn(ExperimentalUnsignedTypes::class)
public fun poLResponseFromBytes(data: ByteArray): PoLResponse? {
    if (data.size < PACKED_SIZE) return null
    var offset = 0
    val uData = data.toUByteArray()

    val flags = uData[offset]
    offset += Constants.FLAGS
    val beaconId = uData.sliceArray(offset until offset + Constants.BEACON_ID).toUIntLE()
    offset += Constants.BEACON_ID
    val counter = uData.sliceArray(offset until offset + Constants.BEACON_COUNTER).toULongLE()
    offset += Constants.BEACON_COUNTER
    val nonce = uData.sliceArray(offset until offset + Constants.PROTOCOL_NONCE)
    offset += Constants.PROTOCOL_NONCE
    val beaconSig = uData.sliceArray(offset until offset + Constants.SIG)
    offset += Constants.SIG // If we ever want to add something after that

    return PoLResponse(flags, beaconId, counter, nonce, beaconSig)
}

// Data that is physically sent over BLE
@OptIn(ExperimentalUnsignedTypes::class)
public fun PoLResponse.toBytes(): ByteArray {
    val buffer = UByteArray(PACKED_SIZE)
    var offset = 0
    buffer[offset] = flags; offset += 1
    beaconId.toUByteArrayLE().copyInto(buffer, offset); offset += 4
    counter.toUByteArrayLE().copyInto(buffer, offset); offset += 8
    nonce.copyInto(buffer, offset); offset += Constants.PROTOCOL_NONCE
    beaconSig.copyInto(buffer, offset)
    return buffer.asByteArray()
}