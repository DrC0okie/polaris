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
private const val unknownErr = "Unknown error"

/**
 * Android implementation of the [BleController] interface.
 *
 * This class orchestrates the lower-level [GattManager] and [FragmentationTransport] to provide
 * the high-level BLE operations defined in the common interface.
 *
 * @property beaconDataParser A utility for parsing raw advertisement data.
 * @property config The global BLE configuration for UUIDs and other parameters.
 */
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
    ): SdkResult<Flow<CommonBleScanResult>, SdkError> {

        // Map CommonScanFilter to Android's ScanFilter
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

        if (!gattManager.isReady()) {
            return SdkResult.Failure(SdkError.PreconditionError("Bluetooth is not enabled."))
        }

        return runCatching {
            // Start the underlying scan, which emits Android's ScanResult
            gattManager.scanResults
                .map {
                    CommonBleScanResult(
                        it.device.address,
                        it.scanRecord?.deviceName,
                        it.scanRecord?.manufacturerSpecificData?.let { sparseArray ->
                            (0 until sparseArray.size).associate { i ->
                                sparseArray.keyAt(i) to sparseArray.valueAt(i)
                            }
                        } ?: emptyMap()
                    )
                }
                .onStart { gattManager.startScan(androidFilters, scanConfig) }
                .onCompletion { gattManager.stopScan() }
        }.fold(
            onSuccess = { scanResults -> SdkResult.Success(scanResults) },
            onFailure = { throwable ->
                SdkResult.Failure(
                    SdkError.BleError(throwable.message ?: "$unknownErr during scanning")
                )
            }
        )
    }

    override suspend fun connect(deviceAddress: String): SdkResult<Unit, SdkError> {
        return runCatching {
            if (!gattManager.isReady()) {
                return SdkResult.Failure(SdkError.PreconditionError("Bluetooth is not enabled."))
            }
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
            if (!gattManager.isReady()) {
                return SdkResult.Failure(SdkError.PreconditionError("Bluetooth is not enabled."))
            }
            val response = performRequestResponse(request.toBytes(), config.tokenWriteUuid, config.tokenIndicateUuid)
            poLResponseFromBytes(response) ?: throw IOException("Failed to parse PoLResponse from beacon data.")
        }.fold(
            onSuccess = { polResponse -> SdkResult.Success(polResponse) },
            onFailure = { throwable ->
                SdkResult.Failure(
                    SdkError.BleError(throwable.message ?: "$unknownErr during pol transaction")
                )
            }
        )
    }

    override suspend fun exchangeSecurePayload(encryptedBlob: ByteArray): SdkResult<ByteArray, SdkError> {
        return runCatching {
            performRequestResponse(encryptedBlob, config.encryptedWriteUuid, config.encryptedIndicateUuid)
        }.fold(
            onSuccess = { encryptedBlob -> SdkResult.Success(encryptedBlob) },
            onFailure = { throwable ->
                SdkResult.Failure(
                    SdkError.BleError(throwable.message ?: "$unknownErr during secure payload delivering")
                )
            }
        )
    }

    override suspend fun postSecurePayload(payload: ByteArray): SdkResult<Unit, SdkError> {
        return runCatching {
            if (!gattManager.isReady()) {
                return SdkResult.Failure(SdkError.PreconditionError("Bluetooth is not enabled."))
            }
            performWriteTransaction(payload, config.encryptedWriteUuid)
        }.fold(
            onSuccess = { SdkResult.Success(Unit) },
            onFailure = {
                SdkResult.Failure(
                    SdkError.BleError(
                        it.message ?: "$unknownErr during secure payload delivering"
                    )
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
    ): SdkResult<Flow<FoundBeacon>, SdkError> {
        if (!gattManager.isReady()) {
            return SdkResult.Failure(SdkError.PreconditionError("Bluetooth is not enabled."))
        }
        val flow = getDiscriminatedScanFlow(scanConfig)
            .filterIsInstance<DiscriminatedScanResult.Legacy>() // We only care about Legacy ads
            .mapNotNull { legacyResult ->
                val commonScanResult = legacyResult.result
                // Parse the advertisement data
                val (beaconId, statusByte) = beaconDataParser.parseConnectableBeaconAd(
                    commonScanResult,
                    config.manufacturerId
                )
                if (beaconId != null) {
                    val matchedInfo = beaconsToFind.find { it.id == beaconId }
                    if (matchedInfo != null) {
                        FoundBeacon(matchedInfo, commonScanResult.deviceAddress, statusByte)
                    } else null
                } else null
            }
        return SdkResult.Success(flow)
    }

    override suspend fun pullEncryptedData(): SdkResult<ByteArray, SdkError> {
        return runCatching {
            if (!gattManager.isReady()) {
                return SdkResult.Failure(SdkError.PreconditionError("Bluetooth is not enabled."))
            }
            performPullTransaction(config.pullDataWriteUuid, config.encryptedIndicateUuid)
        }.fold(
            onSuccess = { encryptedBlob -> SdkResult.Success(encryptedBlob) },
            onFailure = { throwable ->
                SdkResult.Failure(
                    SdkError.BleError(throwable.message ?: "$unknownErr during secure payload fetching")
                )
            }
        )
    }

    override fun monitorBroadcasts(scanConfig: ScanConfig): SdkResult<Flow<BroadcastPayload>, SdkError> {
        if (!gattManager.isReady()) {
            return SdkResult.Failure(SdkError.PreconditionError("Bluetooth is not enabled."))
        }
        val flow = getDiscriminatedScanFlow(scanConfig)
            .filterIsInstance<DiscriminatedScanResult.Extended>() // We only care about Extended ads
            .mapNotNull { extendedResult ->
                val scanResult = extendedResult.result
                // Parse the payload
                beaconDataParser.parseBroadcastPayload(scanResult, config.manufacturerId)
            }
        return SdkResult.Success(flow)
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

        // Wire the transport layer to listen to the beacon response
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

        // Wait for the response
        val response = responseJob.await().asByteArray()

        // Disable Indications and wait for the descriptor write signal
        gattManager.disableIndication()
        val indicationDisabled = gattManager.descriptorWriteSignal.receive()
        if (!indicationDisabled) {
            Log.warn { "Failed to disable indications cleanly for $indicateUuid" }
        }

        return response
    }

    private suspend fun performWriteTransaction(payload: ByteArray, writeUuid: String) {
        // Ensure we are in a ready state
        if (connectionState.value !is ConnectionState.Ready) {
            throw IOException("Cannot perform write transaction, not in a ready state.")
        }

        // Only set the uuid wot the write characteristic
        gattManager.setTransactionUuids(writeUuid, null)

        // Send chunks and wait for the characteristic write signal for each
        transport.fragment(payload).forEach { chunk ->
            gattManager.send(chunk)
            val writeSuccess = gattManager.characteristicWriteSignal.receive()
            if (!writeSuccess) {
                throw IOException("Failed to write BLE characteristic chunk to UUID $writeUuid.")
            }
        }
    }

    private suspend fun performPullTransaction(
        triggerWriteUuid: String,
        dataIndicateUuid: String,
    ): ByteArray {
        // Precondition Check
        if (connectionState.value !is ConnectionState.Ready) {
            throw IOException("Cannot perform pull transaction, not in a ready state.")
        }

        // Configure the GATT manager for this specific transaction
        gattManager.setTransactionUuids(triggerWriteUuid, dataIndicateUuid)

        // Enable indications on the data channel, so we are ready to receive.
        gattManager.enableIndication()
        val indicationEnabled = gattManager.descriptorWriteSignal.receive()
        if (!indicationEnabled) {
            throw IOException("Failed to enable indications for characteristic $dataIndicateUuid")
        }

        // Listen for the complete reassembled message from the beacon.
        val responseJob = scope.async {
            withTimeout(10000) { transport.reassembledMessages.first() }
        }

        // Send the single trigger byte to the PULL_DATA_WRITE characteristic.
        val triggerPayload = byteArrayOf(0x01)
        gattManager.send(triggerPayload)

        // Await confirmation that our trigger byte was successfully written.
        val writeSuccess = gattManager.characteristicWriteSignal.receive()
        if (!writeSuccess) {
            responseJob.cancel() // Don't wait for a response that will never come.
            throw IOException("Failed to write trigger byte to UUID $triggerWriteUuid.")
        }

        // Await the reassembled data blob from the beacon.
        val responseData = responseJob.await().asByteArray()

        // Disable indications before finishing.
        gattManager.disableIndication()
        val indicationDisabled = gattManager.descriptorWriteSignal.receive()
        if (!indicationDisabled) {
            Log.warn { "Failed to disable indications cleanly for $dataIndicateUuid" }
        }

        return responseData
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