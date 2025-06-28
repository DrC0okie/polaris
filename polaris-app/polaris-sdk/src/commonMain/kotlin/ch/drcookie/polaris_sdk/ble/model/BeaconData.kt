package ch.drcookie.polaris_sdk.ble.model

/**
 * Defines the power-vs-latency trade-off for a BLE scan.
 */
public enum class ScanMode {
    LOW_POWER,
    BALANCED,
    LOW_LATENCY
}

/**
 * Defines when the BLE stack should report scan results.
 */
public enum class ScanCallbackType {
    /** Reports the first advertisement match for a given filter. Ideal for finding a single device to connect to. */
    FIRST_MATCH,
    /** Reports all found advertisements that match a given filter. Ideal for continuous monitoring. */
    ALL_MATCHES
}


/**
 * Represents a [Beacon] that has been discovered during a BLE scan.
 *
 * @property provisioningInfo Information about the beacon.
 * @property address The MAC address of the beacon discovered during the scan.
 * @property statusByte Flag that indicate if the beacon has an encrypted payload to deliver
 */
public data class FoundBeacon(
    public val provisioningInfo: Beacon,
    public val address: String,
    public val statusByte: Byte?
) {
    public val name: String get() = provisioningInfo.name

    /**
     * Checks if the "data pending" flag is set in the beacon advertisement.
     * @return `true` if the beacon has data to be pulled by the phone, `false` otherwise.
     */
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
     */
    public val scanMode: ScanMode = ScanMode.BALANCED,

    /**
     * The callback type that determines how often scan results are reported.
     */
    public val callbackType: ScanCallbackType = ScanCallbackType.FIRST_MATCH,

    /**
     * If true, the scanner will only report legacy advertisements.
     * Set to `false` to receive extended advertisements.
     */
    public val scanLegacyOnly: Boolean = true,

    /**
     * If true, the scanner will listen on all supported physical layers (PHYs), including LE Coded for long-range.
     * This is necessary to receive extended advertisements.
     */
    public val useAllSupportedPhys: Boolean = false
)