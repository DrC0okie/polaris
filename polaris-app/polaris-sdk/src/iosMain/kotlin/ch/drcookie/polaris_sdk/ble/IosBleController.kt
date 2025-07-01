package ch.drcookie.polaris_sdk.ble

import ch.drcookie.polaris_sdk.api.SdkError
import ch.drcookie.polaris_sdk.api.SdkResult
import ch.drcookie.polaris_sdk.api.config.BleConfig
import ch.drcookie.polaris_sdk.ble.model.Beacon
import ch.drcookie.polaris_sdk.ble.model.CommonBleScanResult
import ch.drcookie.polaris_sdk.ble.model.CommonScanFilter
import ch.drcookie.polaris_sdk.ble.model.ConnectionState
import ch.drcookie.polaris_sdk.ble.model.FoundBeacon
import ch.drcookie.polaris_sdk.ble.model.ScanConfig
import ch.drcookie.polaris_sdk.protocol.model.BroadcastPayload
import ch.drcookie.polaris_sdk.protocol.model.PoLRequest
import ch.drcookie.polaris_sdk.protocol.model.PoLResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import platform.darwin.NSObject
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBPeripheralManagerDelegateProtocol
import platform.CoreBluetooth.CBPeripheralManager


internal class IosBleController(
    private val config: BleConfig
) : NSObject(), BleController, CBCentralManagerDelegateProtocol, CBPeripheralManagerDelegateProtocol {
    override val connectionState: StateFlow<ConnectionState>
        get() = TODO("Not yet implemented")

    override fun scanForBeacons(
        filters: List<CommonScanFilter>?,
        scanConfig: ScanConfig,
    ): SdkResult<Flow<CommonBleScanResult>, SdkError> {
        TODO("Not yet implemented")
    }

    override suspend fun connect(deviceAddress: String): SdkResult<Unit, SdkError> {
        TODO("Not yet implemented")
    }

    override suspend fun requestPoL(request: PoLRequest): SdkResult<PoLResponse, SdkError> {
        TODO("Not yet implemented")
    }

    override suspend fun exchangeSecurePayload(encryptedBlob: ByteArray): SdkResult<ByteArray, SdkError> {
        TODO("Not yet implemented")
    }

    override suspend fun postSecurePayload(payload: ByteArray): SdkResult<Unit, SdkError> {
        TODO("Not yet implemented")
    }

    override fun disconnect() {
        TODO("Not yet implemented")
    }

    override fun cancelAll() {
        TODO("Not yet implemented")
    }

    override fun findConnectableBeacons(
        scanConfig: ScanConfig,
        beaconsToFind: List<Beacon>,
    ): SdkResult<Flow<FoundBeacon>, SdkError> {
        TODO("Not yet implemented")
    }

    override fun monitorBroadcasts(scanConfig: ScanConfig): SdkResult<Flow<BroadcastPayload>, SdkError> {
        TODO("Not yet implemented")
    }

    override suspend fun pullEncryptedData(): SdkResult<ByteArray, SdkError> {
        TODO("Not yet implemented")
    }

    override fun peripheralManagerDidUpdateState(peripheral: CBPeripheralManager) {
        TODO("Not yet implemented")
    }

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        TODO("Not yet implemented")
    }


}