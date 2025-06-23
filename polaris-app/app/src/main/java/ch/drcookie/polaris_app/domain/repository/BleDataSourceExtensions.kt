package ch.drcookie.polaris_app.domain.repository

import ch.drcookie.polaris_app.domain.model.CommonBleScanResult
import ch.drcookie.polaris_app.domain.model.CommonScanFilter
import ch.drcookie.polaris_app.domain.model.Constants
import ch.drcookie.polaris_app.domain.model.ScanConfig
import kotlinx.coroutines.flow.Flow

/**
 * Convenience extension function to scan specifically for connectable Polaris beacons.
 * This is the recommended way for standard PoL flows.
 */
fun BleDataSource.startConnectableScan(scanConfig: ScanConfig): Flow<CommonBleScanResult> {
    val filters = listOf(CommonScanFilter.ByServiceUuid(Constants.POL_SERVICE_UUID))
    // It calls the core, generic function underneath
    return this.scanForBeacons(filters, scanConfig)
}

/**
 * Convenience extension function to scan specifically for non-connectable Polaris broadcasts.
 * This is the recommended way for monitoring flows.
 */
fun BleDataSource.startBroadcastScan(scanConfig: ScanConfig): Flow<CommonBleScanResult> {
    val filters = listOf(CommonScanFilter.ByManufacturerData(Constants.EXTENDED_MANUFACTURER_ID))
    // It calls the core, generic function underneath
    return this.scanForBeacons(filters, scanConfig)
}