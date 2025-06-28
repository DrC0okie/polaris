package ch.drcookie.polaris_sdk.ble.model

/**
 * A platform-agnostic representation of a raw BLE scan result.
 *
 * @property deviceAddress The MAC address of the discovered device.
 * @property deviceName The advertised local name of the device, if available.
 * @property manufacturerData A map of manufacturer IDs to their corresponding raw data from the advertisement packet.
 */
public data class CommonBleScanResult(
    public val deviceAddress: String,
    public val deviceName: String?,
    public val manufacturerData: Map<Int, ByteArray>,
){
    public override fun hashCode(): Int {
        var result = deviceAddress.hashCode()
        result = 31 * result + (deviceName?.hashCode() ?: 0)
        result = 31 * result + manufacturerData.keys.hashCode()
        // Hash the content of the byte arrays
        manufacturerData.values.forEach {
            result = 31 * result + it.contentHashCode()
        }
        return result
    }

    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CommonBleScanResult

        if (deviceAddress != other.deviceAddress) return false
        if (deviceName != other.deviceName) return false
        if (manufacturerData != other.manufacturerData) return false

        return true
    }
}

/**
 * Represents a filter to be applied to a BLE scan.
 */
public sealed class CommonScanFilter {
    /** A filter that scans for devices advertising a specific GATT service UUID. */
    public data class ByServiceUuid(public val uuid: String) : CommonScanFilter()
    /** A filter that scans for devices containing a specific manufacturer ID in their advertisement data. */
    public data class ByManufacturerData(public val id: Int) : CommonScanFilter()
}

/**
 * Represents a sorted scan result.
 */
@PublishedApi
internal sealed class DiscriminatedScanResult {
    internal  data class Legacy(internal  val result: CommonBleScanResult) : DiscriminatedScanResult()
    internal  data class Extended(internal  val result: CommonBleScanResult) : DiscriminatedScanResult()
    internal  data class Other(internal  val result: CommonBleScanResult) : DiscriminatedScanResult()
}