package ch.drcookie.polaris_app.domain.model

data class CommonBleScanResult(
    val deviceAddress: String,
    val deviceName: String?,
    val manufacturerData: Map<Int, ByteArray>,
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CommonBleScanResult

        if (deviceAddress != other.deviceAddress) return false
        if (deviceName != other.deviceName) return false
        if (!manufacturerData.keys.containsAll(other.manufacturerData.keys)) return false
        if (!other.manufacturerData.keys.containsAll(manufacturerData.keys)) return false
        for (key in manufacturerData.keys) {
            if (!manufacturerData[key].contentEquals(other.manufacturerData[key])) return false
        }

        return true
    }

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
}

sealed class CommonScanFilter {
    data class ByServiceUuid(val uuid: String) : CommonScanFilter()
    data class ByManufacturerData(val id: Int) : CommonScanFilter()
}