package ch.drcookie.polaris_sdk.ble.model

public enum class ScanMode {
    LOW_POWER,
    BALANCED,
    LOW_LATENCY
}

public enum class ScanCallbackType {
    FIRST_MATCH, // For finding a device to connect to
    ALL_MATCHES  // For continuous monitoring
}

// Represents a known beacon that has been found via a BLE scan.
public data class FoundBeacon(
    public val provisioningInfo: Beacon,
    public val address: String,
    public val statusByte: Byte?
) {
    public val name: String get() = provisioningInfo.name

    public val hasDataPending: Boolean
        get() = statusByte?.let { (it.toInt() and 0x01) == 1 } ?: false
}

/**
 * Defines the configuration for a BLE scan session.
 */
public data class ScanConfig(
    /**
     * If not null, the scan will be filtered to only find devices advertising this service UUID.
     * A null value will scan for all nearby devices.
     */
    public val filterByServiceUuid: String? = null,

    /**
     * The power/latency mode for the scan.
     * [ScanMode.LOW_LATENCY] is best for active monitoring.
     * [ScanMode.BALANCED] is good for general-purpose finding.
     */
    public val scanMode: ScanMode = ScanMode.BALANCED,

    public val callbackType: ScanCallbackType = ScanCallbackType.FIRST_MATCH,

    /**
     * If true, the scanner will only report legacy advertisements.
     * If false, it will report both legacy and extended advertisements.
     */
    public val scanLegacyOnly: Boolean = true,

    /**
     * If true, the scanner will listen on all supported physical layers (PHYs),
     * including LE Coded for long-range. This is necessary for extended advertisements.
     */
    public val useAllSupportedPhys: Boolean = false
)