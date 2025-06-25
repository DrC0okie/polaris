package ch.drcookie.polaris_sdk.network

import ch.drcookie.polaris_sdk.ble.model.Beacon
import ch.drcookie.polaris_sdk.ble.model.DeliveryAck
import ch.drcookie.polaris_sdk.ble.model.EncryptedPayload
import ch.drcookie.polaris_sdk.model.PoLToken

public interface ApiClient {
    public val knownBeacons: List<Beacon>

    /**
     * Returns the unique ID assigned to this phone by the backend.
     * @return The phone's ID, or a default value (e.g., -1L) if not registered.
     */
    public fun getPhoneId(): Long

    @OptIn(ExperimentalUnsignedTypes::class)
    public suspend fun registerPhone(
        publicKey: UByteArray,
        deviceModel: String,
        osVersion: String,
        appVersion: String
    ): List<Beacon>

    public suspend fun submitPoLToken(token: PoLToken)
    public suspend fun getPayloadsForDelivery(): List<EncryptedPayload>
    public suspend fun submitSecureAck(ack: DeliveryAck)
}