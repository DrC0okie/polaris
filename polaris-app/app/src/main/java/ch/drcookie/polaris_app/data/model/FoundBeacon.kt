package ch.drcookie.polaris_app.data.model

import android.bluetooth.le.ScanResult
import ch.drcookie.polaris_app.data.model.dto.BeaconProvisioningDto

// Represents a known beacon that has been successfully found via a BLE scan.
data class FoundBeacon(
    val provisioningInfo: BeaconProvisioningDto,
    val scanResult: ScanResult
) {
    // Convenience accessors
    val name: String get() = provisioningInfo.name
    val address: String get() = scanResult.device.address
}