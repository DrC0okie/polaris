package ch.drcookie.polaris_sdk.network

import ch.drcookie.polaris_sdk.ble.model.Beacon
import ch.drcookie.polaris_sdk.ble.model.EncryptedPayload
import ch.drcookie.polaris_sdk.network.dto.AckDto
import ch.drcookie.polaris_sdk.network.dto.BeaconProvisioningDto
import ch.drcookie.polaris_sdk.network.dto.EncryptedPayloadDto

/**
 * Maps a [BeaconProvisioningDto] from the network layer to a public-facing [Beacon] model.
 */
@OptIn(ExperimentalUnsignedTypes::class)
internal fun BeaconProvisioningDto.toBeacon(): Beacon {
    return Beacon(
        id = this.beaconId,
        name = this.name,
        locationDescription = this.locationDescription,
        publicKey = this.publicKey,
        lastKnownCounter = this.lastKnownCounter
    )
}

/**
 * Maps an [EncryptedPayloadDto] from the network layer to a public-facing [EncryptedPayload] model.
 */
@OptIn(ExperimentalUnsignedTypes::class)
internal fun EncryptedPayloadDto.toEncryptedPayload(): EncryptedPayload {
    return EncryptedPayload(
        deliveryId = this.deliveryId,
        beaconId = this.beaconId,
        blob = this.encryptedBlob
    )
}