package ch.drcookie.polaris_sdk.ble.model

data class CommonBleScanResult(
    val deviceAddress: String,
    val deviceName: String?,
    val manufacturerData: Map<Int, ByteArray>,
){
    override fun hashCode(): Int {
        var result = deviceAddress.hashCode()
        result = 31 * result + (deviceName?.hashCode() ?: 0)
        result = 31 * result + manufacturerData.keys.hashCode()
        // Hash the content of the byte arrays
        manufacturerData.values.forEach {
            result = 31 * result + it.contentHashCode()
        }
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CommonBleScanResult

        if (deviceAddress != other.deviceAddress) return false
        if (deviceName != other.deviceName) return false
        if (manufacturerData != other.manufacturerData) return false

        return true
    }
}

sealed class CommonScanFilter {
    data class ByServiceUuid(val uuid: String) : CommonScanFilter()
    data class ByManufacturerData(val id: Int) : CommonScanFilter()
}