package ch.drcookie.polaris_sdk.ble

import ch.drcookie.polaris_sdk.api.SdkError
import ch.drcookie.polaris_sdk.api.SdkResult
import ch.drcookie.polaris_sdk.ble.model.Beacon
import ch.drcookie.polaris_sdk.ble.model.CommonBleScanResult
import ch.drcookie.polaris_sdk.ble.model.CommonScanFilter
import ch.drcookie.polaris_sdk.ble.model.ConnectionState
import ch.drcookie.polaris_sdk.ble.model.FoundBeacon
import ch.drcookie.polaris_sdk.ble.model.ScanConfig
import ch.drcookie.polaris_sdk.protocol.model.BroadcastPayload
import ch.drcookie.polaris_sdk.protocol.model.PoLRequest
import ch.drcookie.polaris_sdk.protocol.model.PoLResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * The primary interface for all BLE interactions.
 *
 * This controller provides a unified API for scanning, connecting, and performing data transactions.
 * All asynchronous operations that involve I/O return an [SdkResult] to handle potential failures.
 */
public interface BleController {

    /**
     * A [StateFlow] that emits the current state of the BLE connection.
     * Can be collected by the UI to show connection status (e.g., "Scanning", "Connecting", "Ready").
     */
    public val connectionState: StateFlow<ConnectionState>

    /**
     * Initiates a BLE scan for devices matching the given filters.
     * This is a low-level scanning function. For higher-level, parsed flows, use [findConnectableBeacons]
     * or [monitorBroadcasts].
     *
     * @param filters A list of [CommonScanFilter]s to apply. A `null` or empty list will scan for all nearby devices.
     * @param scanConfig The configuration for the scan's power and callback settings.
     * @return An [SdkResult] containing a [Flow] of found devices on success.
     */
    public fun scanForBeacons(
        filters: List<CommonScanFilter>?,
        scanConfig: ScanConfig,
    ): SdkResult<Flow<CommonBleScanResult>, SdkError>

    /**
     * Initiates a connection to a BLE device at the given address.
     * Collect the [connectionState] flow to know when the device is `Ready` or if the connection has `Failed`.
     *
     * @param deviceAddress The MAC address of the device to connect to.
     * @return An [SdkResult] containing [Unit] if the connection process was initiated successfully.
     */
    public suspend fun connect(deviceAddress: String): SdkResult<Unit, SdkError>

    /**
     * Full PoL transaction, which is a request-response exchange.
     *
     * @param request The signed [PoLRequest] to send to the beacon.
     * @return An [SdkResult] containing the beacon's [PoLResponse] on success.
     */
    public suspend fun requestPoL(request: PoLRequest): SdkResult<PoLResponse, SdkError>

    /**
     * Sends an encrypted payload and waits for an encrypted ACK.
     *
     * @param encryptedBlob The encrypted data blob to send to the beacon.
     * @return An [SdkResult] containing the beacon's encrypted ACK/ERR response as a [ByteArray].
     */
    public suspend fun exchangeSecurePayload(encryptedBlob: ByteArray): SdkResult<ByteArray, SdkError>

    /**
     * Sends a secure data payload in a "fire-and-forget" manner, without waiting for a response.
     *
     * @param payload The encrypted data blob to send to the beacon.
     * @return An [SdkResult] containing [Unit] if the data was successfully written.
     */
    public suspend fun postSecurePayload(payload: ByteArray): SdkResult<Unit, SdkError>


    /**
     * Disconnects from the currently connected BLE device.
     */
    public fun disconnect()

    /**
     * Cancels all ongoing operations, including scans and connections, and releases all SDK resources.
     */
    public fun cancelAll()

    /**
     * Scans for connectable beacons advertising the Polaris service.
     *
     * @param scanConfig The configuration for the scan.
     * @param beaconsToFind A list of known [Beacon]s to filter against.
     * @return An [SdkResult] containing a [Flow] of found beacons on success.
     */
    public fun findConnectableBeacons(
        scanConfig: ScanConfig,
        beaconsToFind: List<Beacon>,
    ): SdkResult<Flow<FoundBeacon>, SdkError>

    /**
     * Scans for non-connectable broadcast advertisements from Polaris beacons.
     * This method does not perform signature verification.
     *
     * @param scanConfig The configuration for the scan.
     * @return A [Flow] of parsed [BroadcastPayload].
     */
    public fun monitorBroadcasts(scanConfig: ScanConfig): SdkResult<Flow<BroadcastPayload>, SdkError>

    /**
     * Triggers a connected beacon to send its pending data and receives it.
     *
     * @return An [SdkResult] containing the beacon's complete encrypted data blob as a [ByteArray].
     */
    public suspend fun pullEncryptedData(): SdkResult<ByteArray, SdkError>
}