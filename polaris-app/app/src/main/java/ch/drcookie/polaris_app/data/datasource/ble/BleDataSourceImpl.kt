package ch.drcookie.polaris_app.data.datasource.ble

import android.annotation.SuppressLint
import android.bluetooth.le.ScanFilter
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import ch.drcookie.polaris_app.domain.model.CommonScanFilter
import ch.drcookie.polaris_app.domain.model.CommonBleScanResult
import ch.drcookie.polaris_app.domain.model.PoLRequest
import ch.drcookie.polaris_app.domain.model.PoLResponse
import ch.drcookie.polaris_app.domain.model.Constants
import ch.drcookie.polaris_app.domain.model.ScanConfig
import ch.drcookie.polaris_app.domain.repository.BleDataSource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.util.UUID
import androidx.core.util.size

@SuppressLint("MissingPermission")
@OptIn(ExperimentalUnsignedTypes::class)
class BleDataSourceImpl(context: Context): BleDataSource {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val bleManager = BleManager(context, scope)
    private val transport = FragmentationTransport()
    override val connectionState: StateFlow<ConnectionState> = bleManager.connectionState

    init {
        // Wire up the transport layer to the BleManager
        scope.launch {
            bleManager.receivedData.collect { chunk ->
                transport.process(chunk)
            }
        }
        scope.launch {
            bleManager.mtu.collect { newMtu ->
                transport.onMtuChanged(newMtu)
            }
        }
    }

    override fun scanForBeacons(
        filters: List<CommonScanFilter>?,
        scanConfig: ScanConfig
    ): Flow<CommonBleScanResult> {

        // Map our CommonScanFilter to Android's ScanFilter
        val androidFilters = filters?.map { commonFilter ->
            when (commonFilter) {
                is CommonScanFilter.ByServiceUuid ->
                    ScanFilter.Builder()
                        .setServiceUuid(ParcelUuid(UUID.fromString(commonFilter.uuid)))
                        .build()
                is CommonScanFilter.ByManufacturerData ->
                    ScanFilter.Builder()
                        .setManufacturerData(commonFilter.id, null)
                        .build()
            }
        }

        // Start the underlying scan, which emits Android's ScanResult
        return bleManager.scanResults
            .map { androidScanResult ->
                CommonBleScanResult(
                    deviceAddress = androidScanResult.device.address,
                    deviceName = androidScanResult.scanRecord?.deviceName,
                    manufacturerData = androidScanResult.scanRecord?.manufacturerSpecificData?.let { sparseArray ->
                        (0 until sparseArray.size).associate { i ->
                            sparseArray.keyAt(i) to sparseArray.valueAt(i)
                        }
                    } ?: emptyMap()
                )
            }
            .onStart { bleManager.startScan(androidFilters, scanConfig) }
            .onCompletion { bleManager.stopScan() }
    }

    override suspend fun connect(deviceAddress: String) {
        val device = bleManager.bluetoothAdapter.getRemoteDevice(deviceAddress)
        bleManager.connectToDevice(device)
    }


    override suspend fun requestPoL(request: PoLRequest): PoLResponse {
        val responseBytes = performRequestResponse(
            requestPayload = request.toBytes(),
            writeUuid = Constants.TOKEN_WRITE_UUID,
            indicateUuid = Constants.TOKEN_INDICATE_UUID
        )
        return PoLResponse.fromBytes(responseBytes)
            ?: throw IOException("Failed to parse PoLResponse from beacon data.")
    }

    override suspend fun deliverSecurePayload(encryptedBlob: ByteArray): ByteArray {
        return performRequestResponse(
            requestPayload = encryptedBlob,
            writeUuid = Constants.ENCRYPTED_WRITE_UUID,
            indicateUuid = Constants.ENCRYPTED_INDICATE_UUID
        )
    }

    override fun disconnect() = bleManager.close()

    override fun cancelAll() {
        scope.cancel()
        bleManager.close()
    }

    private suspend fun performRequestResponse(
        requestPayload: ByteArray,
        writeUuid: String,
        indicateUuid: String
    ): ByteArray {
        // Ensure we are in a ready state
        if (connectionState.value !is ConnectionState.Ready) {
            throw IOException("Cannot perform request, not in a ready state.")
        }

        // Configure the manager for this specific transaction
        bleManager.setTransactionUuids(writeUuid, indicateUuid)

        // Enable indications and wait for the descriptor write signal
        bleManager.enableIndication()
        val indicationEnabled = bleManager.descriptorWriteSignal.receive()
        if (!indicationEnabled) {
            throw IOException("Failed to enable indications for characteristic $indicateUuid")
        }

        // Now we can proceed with the write/read logic
        val responseJob = scope.async {
            withTimeout(10000) { transport.reassembledMessages.first() }
        }

        // Send chunks and wait for the characteristic write signal for each
        transport.fragment(requestPayload).forEach { chunk ->
            bleManager.send(chunk)
            val writeSuccess = bleManager.characteristicWriteSignal.receive()
            if (!writeSuccess) {
                responseJob.cancel()
                throw IOException("Failed to write BLE characteristic chunk to UUID $writeUuid.")
            }
        }

        val response = responseJob.await().asByteArray()

        // Disable Indications and wait for the descriptor write signal
        bleManager.disableIndication()
        val indicationDisabled = bleManager.descriptorWriteSignal.receive()
        if (!indicationDisabled) {
            Log.w("BleDataSource", "Failed to disable indications cleanly for $indicateUuid")
        }

        return response
    }
}