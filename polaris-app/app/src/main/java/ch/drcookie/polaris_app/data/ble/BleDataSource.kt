package ch.drcookie.polaris_app.data.ble

import android.annotation.SuppressLint
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import ch.drcookie.polaris_app.data.model.PoLRequest
import ch.drcookie.polaris_app.data.model.PoLResponse
import ch.drcookie.polaris_app.repository.ScanConfig
import ch.drcookie.polaris_app.util.PoLConstants
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException

@SuppressLint("MissingPermission")
@OptIn(ExperimentalUnsignedTypes::class)
class BleDataSource(context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val bleManager = BleManager(context, scope)
    private val transport = FragmentationTransport()
    val connectionState: StateFlow<ConnectionState> = bleManager.connectionState

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

    fun scanForBeacons(filters: List<ScanFilter>?, scanConfig: ScanConfig): Flow<ScanResult> {
        return bleManager.scanResults
            .onStart { bleManager.startScan(filters, scanConfig) }
            .onCompletion { bleManager.stopScan() }
    }

    suspend fun connect(deviceAddress: String) {
        val device = bleManager.bluetoothAdapter.getRemoteDevice(deviceAddress)
        bleManager.connectToDevice(device)
    }


    suspend fun requestPoL(request: PoLRequest): PoLResponse {
        val responseBytes = performRequestResponse(
            requestPayload = request.toBytes(),
            writeUuid = PoLConstants.TOKEN_WRITE_UUID,
            indicateUuid = PoLConstants.TOKEN_INDICATE_UUID
        )
        return PoLResponse.fromBytes(responseBytes)
            ?: throw IOException("Failed to parse PoLResponse from beacon data.")
    }

    suspend fun deliverSecurePayload(encryptedBlob: ByteArray): ByteArray {
        return performRequestResponse(
            requestPayload = encryptedBlob,
            writeUuid = PoLConstants.ENCRYPTED_WRITE_UUID,
            indicateUuid = PoLConstants.ENCRYPTED_INDICATE_UUID
        )
    }

    fun disconnect() = bleManager.close()

    fun cancelAll() {
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