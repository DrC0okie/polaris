package ch.drcookie.polaris_sdk.network

import ch.drcookie.polaris_sdk.api.SdkError
import ch.drcookie.polaris_sdk.api.SdkResult
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
        appVersion: String,
    ): SdkResult<List<Beacon>, SdkError>

    public suspend fun fetchBeacons(): SdkResult<List<Beacon>, SdkError>
    public suspend fun submitPoLToken(token: PoLToken): SdkResult<Unit, SdkError>
    public suspend fun getPayloadsForDelivery(): SdkResult<List<EncryptedPayload>, SdkError>
    public suspend fun submitSecureAck(ack: DeliveryAck): SdkResult<Unit, SdkError>

    /**
     * Forwards an encrypted data blob from a beacon to the server and returns
     * the server's encrypted ACK/ERR response.
     *
     * @param data The raw encrypted data from the beacon.
     * @return An SdkResult containing the server's raw encrypted ACK/ERR data.
     */
    public suspend fun forwardBeaconPayload(payload: ByteArray): SdkResult<ByteArray, SdkError>
    public fun closeClient()
}