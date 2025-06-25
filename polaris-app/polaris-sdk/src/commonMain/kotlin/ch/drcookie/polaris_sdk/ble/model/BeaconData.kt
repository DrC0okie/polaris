package ch.drcookie.polaris_sdk.ble.model

import ch.drcookie.polaris_sdk.network.dto.BeaconProvisioningDto

enum class ScanMode {
    LOW_POWER,
    BALANCED,
    LOW_LATENCY
}

enum class ScanCallbackType {
    FIRST_MATCH, // For finding a device to connect to
    ALL_MATCHES  // For continuous monitoring
}

// Represents a known beacon that has been successfully found via a BLE scan.
data class FoundBeacon(
    val provisioningInfo: BeaconProvisioningDto,
    val address: String
) {
    val name: String get() = provisioningInfo.name
}

/**
 * Defines the configuration for a BLE scan session.
 */
data class ScanConfig(
    /**
     * If not null, the scan will be filtered to only find devices advertising this service UUID.
     * Use [ch.drcookie.polaris_sdk.util.Constants.POL_SERVICE_UUID] to find connectable Polaris beacons.
     * A null value will scan for all nearby devices.
     */
    val filterByServiceUuid: String? = null,

    /**
     * The power/latency mode for the scan.
     * [ScanMode.LOW_LATENCY] is best for active monitoring.
     * [ScanMode.BALANCED] is good for general-purpose finding.
     */
    val scanMode: ScanMode = ScanMode.BALANCED,

    val callbackType: ScanCallbackType = ScanCallbackType.FIRST_MATCH,

    /**
     * If true, the scanner will only report legacy advertisements.
     * If false, it will report both legacy and extended advertisements.
     */
    val scanLegacyOnly: Boolean = true,

    /**
     * If true, the scanner will listen on all supported physical layers (PHYs),
     * including LE Coded for long-range. This is necessary for extended advertisements.
     */
    val useAllSupportedPhys: Boolean = false
)