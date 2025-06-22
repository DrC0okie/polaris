package ch.drcookie.polaris_app.repository

import ch.drcookie.polaris_app.util.PoLConstants

/**
 * Defines the configuration for a BLE scan session.
 */
data class ScanConfig(
    /**
     * If not null, the scan will be filtered to only find devices advertising this service UUID.
     * Use [PoLConstants.POL_SERVICE_UUID] to find connectable Polaris beacons.
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