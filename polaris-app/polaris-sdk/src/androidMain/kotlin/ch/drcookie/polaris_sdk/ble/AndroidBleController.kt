package ch.drcookie.polaris_sdk.ble

import android.annotation.SuppressLint
import android.bluetooth.le.ScanFilter
import android.content.Context
import android.os.ParcelUuid
import io.github.oshai.kotlinlogging.KotlinLogging
import ch.drcookie.polaris_sdk.ble.model.CommonScanFilter
import ch.drcookie.polaris_sdk.ble.model.CommonBleScanResult
import ch.drcookie.polaris_sdk.protocol.model.PoLRequest
import ch.drcookie.polaris_sdk.protocol.model.PoLResponse
import ch.drcookie.polaris_sdk.ble.model.ScanConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.util.UUID
import androidx.core.util.size
import ch.drcookie.polaris_sdk.api.SdkError
import ch.drcookie.polaris_sdk.api.SdkResult
import ch.drcookie.polaris_sdk.api.config.BleConfig
import ch.drcookie.polaris_sdk.ble.model.Beacon
import ch.drcookie.polaris_sdk.ble.util.BeaconDataParser
import ch.drcookie.polaris_sdk.protocol.model.BroadcastPayload
import ch.drcookie.polaris_sdk.ble.model.FoundBeacon
import ch.drcookie.polaris_sdk.ble.model.ConnectionState
import ch.drcookie.polaris_sdk.ble.model.DiscriminatedScanResult
import ch.drcookie.polaris_sdk.protocol.model.poLResponseFromBytes
import ch.drcookie.polaris_sdk.protocol.model.toBytes

private val Log = KotlinLogging.logger {}
private val unknownErr = "Unknown error"

@SuppressLint("MissingPermission")
@OptIn(ExperimentalUnsignedTypes::class)
internal class AndroidBleController(
    context: Context,
    private val beaconDataParser: BeaconDataParser,
    private val config: BleConfig,
) : BleController {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val gattManager = GattManager(context, scope, config)
    private val transport = FragmentationTransport()
    override val connectionState: StateFlow<ConnectionState> = gattManager.connectionState

    init {
        // Wire up the transport layer to the BleManager
        scope.launch {
            gattManager.receivedData.collect { chunk ->
                transport.process(chunk)
            }
        }
        scope.launch {
            gattManager.mtu.collect { newMtu ->
                transport.onMtuChanged(newMtu)
            }
        }
    }

    override fun scanForBeacons(
        filters: List<CommonScanFilter>?,
        scanConfig: ScanConfig,
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
        return gattManager.scanResults
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
            .onStart { gattManager.startScan(androidFilters, scanConfig) }
            .onCompletion { gattManager.stopScan() }
    }

    override suspend fun connect(deviceAddress: String): SdkResult<Unit, SdkError> {
        return runCatching {
            val device = gattManager.bluetoothAdapter.getRemoteDevice(deviceAddress)
            gattManager.connectToDevice(device)
        }.fold(
            onSuccess = { SdkResult.Success(Unit) },
            onFailure = { throwable ->
                SdkResult.Failure(
                    SdkError.BleError(throwable.message ?: "$unknownErr during connection")
                )
            }
        )
    }


    override suspend fun requestPoL(request: PoLRequest): SdkResult<PoLResponse, SdkError> {
        return runCatching {
            val responseBytes = performRequestResponse(
                requestPayload = request.toBytes(),
                writeUuid = config.tokenWriteUuid,
                indicateUuid = config.tokenIndicateUuid
            )
            poLResponseFromBytes(responseBytes)
                ?: throw IOException("Failed to parse PoLResponse from beacon data.")
        }.fold(
            onSuccess = { polResponse -> SdkResult.Success(polResponse) },
            onFailure = { throwable ->
                SdkResult.Failure(
                    SdkError.BleError(throwable.message ?: "$unknownErr during pol transaction")
                )
            }
        )
    }

    override suspend fun deliverSecurePayload(encryptedBlob: ByteArray): SdkResult<ByteArray, SdkError> {
        return runCatching {
            performRequestResponse(
                requestPayload = encryptedBlob,
                writeUuid = config.encryptedWriteUuid,
                indicateUuid = config.encryptedIndicateUuid
            )
        }.fold(
            onSuccess = { encryptedBlob -> SdkResult.Success(encryptedBlob) },
            onFailure = { throwable ->
                SdkResult.Failure(
                    SdkError.BleError(throwable.message ?: "$unknownErr during secure payload delivering")
                )
            }
        )
    }

    override fun disconnect() = gattManager.close()

    override fun cancelAll() {
        scope.cancel()
        gattManager.close()
    }

    override fun findConnectableBeacons(
        scanConfig: ScanConfig,
        beaconsToFind: List<Beacon>,
    ): Flow<FoundBeacon> {
        return getDiscriminatedScanFlow(scanConfig)
            .filterIsInstance<DiscriminatedScanResult.Legacy>() // We only care about Legacy ads
            .mapNotNull { legacyResult ->
                val commonScanResult = legacyResult.result
                // Parse the ID
                val beaconId = beaconDataParser.parseConnectableBeaconId(commonScanResult, config.manufacturerId)
                if (beaconId != null) {
                    val matchedInfo = beaconsToFind.find { it.id == beaconId }
                    if (matchedInfo != null) {
                        FoundBeacon(
                            provisioningInfo = matchedInfo,
                            address = commonScanResult.deviceAddress
                        )
                    } else null
                } else null
            }
    }

    override fun monitorBroadcasts(scanConfig: ScanConfig): Flow<BroadcastPayload> {
        return getDiscriminatedScanFlow(scanConfig)
            .filterIsInstance<DiscriminatedScanResult.Extended>() // We only care about Extended ads
            .mapNotNull { extendedResult ->
                val scanResult = extendedResult.result
                // Parse the payload
                beaconDataParser.parseBroadcastPayload(scanResult, config.manufacturerId)
            }
    }

    private suspend fun performRequestResponse(
        requestPayload: ByteArray,
        writeUuid: String,
        indicateUuid: String,
    ): ByteArray {
        // Ensure we are in a ready state
        if (connectionState.value !is ConnectionState.Ready) {
            throw IOException("Cannot perform request, not in a ready state.")
        }

        // Configure the manager for this specific transaction
        gattManager.setTransactionUuids(writeUuid, indicateUuid)

        // Enable indications and wait for the descriptor write signal
        gattManager.enableIndication()
        val indicationEnabled = gattManager.descriptorWriteSignal.receive()
        if (!indicationEnabled) {
            throw IOException("Failed to enable indications for characteristic $indicateUuid")
        }

        // Now we can proceed with the write/read logic
        val responseJob = scope.async {
            withTimeout(10000) { transport.reassembledMessages.first() }
        }

        // Send chunks and wait for the characteristic write signal for each
        transport.fragment(requestPayload).forEach { chunk ->
            gattManager.send(chunk)
            val writeSuccess = gattManager.characteristicWriteSignal.receive()
            if (!writeSuccess) {
                responseJob.cancel()
                throw IOException("Failed to write BLE characteristic chunk to UUID $writeUuid.")
            }
        }

        val response = responseJob.await().asByteArray()

        // Disable Indications and wait for the descriptor write signal
        gattManager.disableIndication()
        val indicationDisabled = gattManager.descriptorWriteSignal.receive()
        if (!indicationDisabled) {
            Log.warn { "Failed to disable indications cleanly for $indicateUuid" }
        }

        return response
    }

    private fun getDiscriminatedScanFlow(scanConfig: ScanConfig): Flow<DiscriminatedScanResult> {
        val filters = listOf(
            ScanFilter.Builder().setManufacturerData(config.manufacturerId, null).build()
        )

        return gattManager.discriminatedScanResults
            .onStart { gattManager.startScan(filters, scanConfig) }
            .onCompletion { gattManager.stopScan() }
    }
}