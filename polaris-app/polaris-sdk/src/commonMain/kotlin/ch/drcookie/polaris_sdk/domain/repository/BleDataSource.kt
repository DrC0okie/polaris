package ch.drcookie.polaris_sdk.domain.repository

import ch.drcookie.polaris_sdk.domain.model.ConnectionState
import ch.drcookie.polaris_sdk.domain.model.BroadcastPayload
import ch.drcookie.polaris_sdk.domain.model.CommonBleScanResult
import ch.drcookie.polaris_sdk.domain.model.CommonScanFilter
import ch.drcookie.polaris_sdk.domain.model.FoundBeacon
import ch.drcookie.polaris_sdk.domain.model.PoLRequest
import ch.drcookie.polaris_sdk.domain.model.PoLResponse
import ch.drcookie.polaris_sdk.domain.model.ScanConfig
import ch.drcookie.polaris_sdk.domain.model.dto.BeaconProvisioningDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BleDataSource {
    val connectionState: StateFlow<ConnectionState>

    /**
     * The core, generic scanning function. Allows developers to provide any combination of custom filters.
     *
     * @param filters A list of [CommonScanFilter] to apply. Can be null for an open scan.
     * @param scanConfig The configuration for the scan's power and callback settings.
     * @return A Flow of found devices that match the filter criteria.
     */
    fun scanForBeacons( filters: List<CommonScanFilter>?, scanConfig: ScanConfig ): Flow<CommonBleScanResult>
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