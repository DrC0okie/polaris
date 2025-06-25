package ch.drcookie.polaris_sdk.ble

import ch.drcookie.polaris_sdk.ble.model.CommonBleScanResult
import ch.drcookie.polaris_sdk.ble.model.CommonScanFilter
import ch.drcookie.polaris_sdk.ble.model.ConnectionState
import ch.drcookie.polaris_sdk.ble.model.FoundBeacon
import ch.drcookie.polaris_sdk.ble.model.ScanConfig
import ch.drcookie.polaris_sdk.network.dto.BeaconProvisioningDto
import ch.drcookie.polaris_sdk.protocol.model.BroadcastPayload
import ch.drcookie.polaris_sdk.protocol.model.PoLRequest
import ch.drcookie.polaris_sdk.protocol.model.PoLResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BleController {
    val connectionState: StateFlow<ConnectionState>

    /**
     * The core, generic scanning function. Allows developers to provide any combination of custom filters.
     *
     * @param filters A list of [ch.drcookie.polaris_sdk.ble.model.CommonScanFilter] to apply. Can be null for an open scan.
     * @param scanConfig The configuration for the scan's power and callback settings.
     * @return A Flow of found devices that match the filter criteria.
     */
    fun scanForBeacons(filters: List<CommonScanFilter>?, scanConfig: ScanConfig): Flow<CommonBleScanResult>
    suspend fun connect(deviceAddress: String)
    suspend fun requestPoL(request: PoLRequest): PoLResponse
    suspend fun deliverSecurePayload(encryptedBlob: ByteArray): ByteArray
    fun disconnect()
    fun cancelAll()

    /**
     * Scans for connectable beacons and returns a flow of FOUND beacons,
     * already parsed and associated with their provisioning info.
     */
    fun findConnectableBeacons(
        scanConfig: ScanConfig,
        beaconsToFind: List<BeaconProvisioningDto>
    ): Flow<FoundBeacon>

    /**
     * Scans for broadcast advertisements and returns a flow of parsed payloads.
     * Note: This does not verify the signature.
     */
    fun monitorBroadcasts(scanConfig: ScanConfig): Flow<BroadcastPayload>
}