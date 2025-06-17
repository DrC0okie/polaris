package ch.drcookie.polaris_app.data.ble

import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import android.content.Context
import ch.drcookie.polaris_app.data.model.PoLRequest
import ch.drcookie.polaris_app.data.model.PoLResponse
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException

@SuppressLint("MissingPermission")
@OptIn(ExperimentalUnsignedTypes::class)
class BleDataSource(context: Context) {
    companion object{
        const val TAG = "BleDataSource"
    }

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

    // High-level functions
    fun scanForBeacons(): Flow<ScanResult> {
        return bleManager.scanResults
            .onStart { bleManager.startScan() }
            .onCompletion { bleManager.stopScan() } // Automatically stop when collector finishes
    }

    suspend fun connect(deviceAddress: String) {
        val device = bleManager.bluetoothAdapter.getRemoteDevice(deviceAddress)
        bleManager.connectToDevice(device)
    }


    suspend fun requestPoL(request: PoLRequest): PoLResponse {
        val fullMessage = request.toBytes()

        // Prepare to listen for the reassembled response before sending
        val responseJob = scope.async {
            withTimeout(10000) {
                transport.reassembledMessages
                    .mapNotNull { PoLResponse.fromBytes(it.asByteArray()) }
                    .first()
            }
        }

        // Send the request chunk by chunk, awaiting confirmation for each one.
        transport.fragment(fullMessage).forEach { chunk ->
            bleManager.send(chunk)
            // Suspend until the onCharacteristicWrite callback sends a signal.
            val writeSuccess = bleManager.writeSignal.receive()
            if (!writeSuccess) {
                responseJob.cancel() // Cancel the listening job
                throw IOException("Failed to write BLE characteristic chunk.")
            }
        }

        return responseJob.await()
    }

    fun disconnect() = bleManager.close()

    fun cancelAll() {
        scope.cancel()
        bleManager.close()
    }
}