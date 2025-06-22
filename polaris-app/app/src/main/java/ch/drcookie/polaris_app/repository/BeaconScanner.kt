package ch.drcookie.polaris_app.repository

import android.bluetooth.le.ScanResult
import ch.drcookie.polaris_app.data.ble.BleDataSource
import kotlinx.coroutines.flow.Flow
import android.bluetooth.le.ScanFilter
import android.os.ParcelUuid
import ch.drcookie.polaris_app.util.PoLConstants
import java.util.UUID

/**
 * Provides a low-level, configurable BLE scanning interface.
 * Its sole responsibility is to start and stop the physical scan and emit raw results.
 */
class BeaconScanner(private val bleDataSource: BleDataSource) {

    /**
     * Starts a scan specifically for connectable Polaris beacons advertising the PoL service.
     */
    fun startConnectableScan(scanConfig: ScanConfig): Flow<ScanResult> {
        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(UUID.fromString(PoLConstants.POL_SERVICE_UUID)))
                .build()
        )
        return bleDataSource.scanForBeacons(filters, scanConfig)
    }

    /**
     * Starts a scan specifically for non-connectable Polaris broadcast advertisements.
     * This uses a hardware filter for the extended advertisement's manufacturer ID.
     */
    fun startBroadcastScan(scanConfig: ScanConfig): Flow<ScanResult> {
        // Create a filter that targets only the specific extended advertisement manufacturer ID.
        val filters = listOf(
            ScanFilter.Builder()
                .setManufacturerData(PoLConstants.EXTENDED_MANUFACTURER_ID, null)
                .build()
        )
        return bleDataSource.scanForBeacons(filters, scanConfig)
    }
}