package ch.drcookie.polaris_app.domain.repository

import ch.drcookie.polaris_app.data.datasource.ble.ConnectionState
import ch.drcookie.polaris_app.domain.model.CommonBleScanResult
import ch.drcookie.polaris_app.domain.model.CommonScanFilter
import ch.drcookie.polaris_app.domain.model.PoLRequest
import ch.drcookie.polaris_app.domain.model.PoLResponse
import ch.drcookie.polaris_app.domain.model.ScanConfig
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
}