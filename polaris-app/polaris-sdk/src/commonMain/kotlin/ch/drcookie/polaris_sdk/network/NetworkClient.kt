package ch.drcookie.polaris_sdk.network

import ch.drcookie.polaris_sdk.api.SdkError
import ch.drcookie.polaris_sdk.api.SdkResult
import ch.drcookie.polaris_sdk.ble.model.Beacon
import ch.drcookie.polaris_sdk.ble.model.DeliveryAck
import ch.drcookie.polaris_sdk.ble.model.EncryptedPayload
import ch.drcookie.polaris_sdk.model.PoLToken
import ch.drcookie.polaris_sdk.api.Polaris

/**
 * The main interface for all network communication with a Polaris-compatible backend.
 *
 * This component handles the serialization of requests and deserialization of responses.
 * All methods are `suspend` functions and return an [SdkResult] to explicitly handle
 * network failures and other errors without throwing exceptions.
 *
 * An instance of this client is available via [Polaris.networkClient]. If the network is not
 * configured during SDK initialization, a [NoOpNetworkClient] is provided which will return
 * a [SdkError.PreconditionError] for all calls.
 */
public interface NetworkClient {

    /**
     * A read-only list of beacons known to the SDK, typically populated after a successful
     * call to [registerPhone] or [fetchBeacons].
     */
    public val knownBeacons: List<Beacon>

    /**
     * Returns the unique ID assigned to this phone by the backend, retrieved from local storage.
     * @return The phone's ID, or -1L if the device has not been registered.
     */
    public fun getPhoneId(): Long

    /**
     * Registers this device with the backend for the first time.
     * This involves sending the device's public key and metadata to the server.
     * On success, the server returns an API key, a phone ID, and a list of known beacons, which are stored by the SDK.
     *
     * @param publicKey The Ed25519 public key of this device.
     * @param deviceModel A string identifying the device model (e.g., "Pixel 8 Pro").
     * @param osVersion A string identifying the OS version (e.g., "14").
     * @param appVersion A string identifying the application version (e.g., "1.2.0").
     * @return An [SdkResult] containing a list of [Beacon] on success.
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    public suspend fun registerPhone(
        publicKey: UByteArray,
        deviceModel: String,
        osVersion: String,
        appVersion: String,
    ): SdkResult<List<Beacon>, SdkError>

    /**
     * Fetches the list of known beacons for an already-registered device.
     * This should be used on subsequent app launches instead of [registerPhone].
     *
     * @return An [SdkResult] containing a list of [Beacon]s on success.
     */
    public suspend fun fetchBeacons(): SdkResult<List<Beacon>, SdkError>

    /**
     * Submits a completed PoL token to the server for verification and storage.
     *
     * @param token The [PoLToken] generated after a successful transaction with a beacon.
     * @return An [SdkResult] containing [Unit] on success.
     */
    public suspend fun submitPoLToken(token: PoLToken): SdkResult<Unit, SdkError>

    /**
     * Fetches a list of pending secure payloads from the server that are queued for delivery to beacons.
     *
     * @return An [SdkResult] containing a list of [EncryptedPayload]s on success.
     */
    public suspend fun getPayloadsForDelivery(): SdkResult<List<EncryptedPayload>, SdkError>

    /**
     * Submits an acknowledgement to the server after attempting to deliver a secure payload.
     *
     * @param ack The [DeliveryAck] containing the original delivery ID and the ACK/ERR blob from the beacon.
     * @return An [SdkResult] containing [Unit] on success.
     */
    public suspend fun submitSecureAck(ack: DeliveryAck): SdkResult<Unit, SdkError>

    /**
     * Forwards an encrypted data blob from a beacon to the server and returns the server's response.
     *
     * @param beaconId The ID of the beacon that originated the payload.
     * @param payload The raw encrypted data from the beacon.
     * @return An [SdkResult] containing the server's raw encrypted response data as a [ByteArray].
     */
    public suspend fun forwardBeaconPayload(beaconId: UInt, payload: ByteArray): SdkResult<ByteArray, SdkError>

    /**
     * Closes the underlying network client and releases its resources.
     */
    public fun closeClient()
}