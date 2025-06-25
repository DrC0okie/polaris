package ch.drcookie.polaris_sdk.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import ch.drcookie.polaris_sdk.api.config.BleConfig
import ch.drcookie.polaris_sdk.ble.util.BleUtils.gattStatusToString
import ch.drcookie.polaris_sdk.ble.util.BleUtils.isConnected
import ch.drcookie.polaris_sdk.ble.model.ScanCallbackType
import ch.drcookie.polaris_sdk.ble.model.ScanConfig
import ch.drcookie.polaris_sdk.ble.model.ScanMode
import ch.drcookie.polaris_sdk.ble.model.ConnectionState
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private val Log = KotlinLogging.logger {}

@SuppressLint("MissingPermission")
internal class GattManager(
    private val context: Context,
    private val externalScope: CoroutineScope,
    private val config: BleConfig,
) {

    // Signals completion of a characteristic write (for sending chunks)
    internal val characteristicWriteSignal = Channel<Boolean>(Channel.RENDEZVOUS)

    // Signals completion of a descriptor write (for enabling/disabling indications)
    internal val descriptorWriteSignal = Channel<Boolean>(Channel.RENDEZVOUS)

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    internal val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter
    private val scanner = bluetoothAdapter.bluetoothLeScanner
    private var lastWrittenValue: ByteArray? = null

    @Volatile
    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var indicateCharacteristic: BluetoothGattCharacteristic? = null
    private var currentWriteUuid: UUID? = null
    private var currentIndicateUuid: UUID? = null
    private val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    @Volatile
    private var isScanning = false

    // Flow variables
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    internal val connectionState = _connectionState.asStateFlow()

    private val _scanResults = MutableSharedFlow<ScanResult>()
    internal val scanResults = _scanResults.asSharedFlow()

    private val _receivedData = MutableSharedFlow<ByteArray>()
    internal val receivedData = _receivedData.asSharedFlow()

    private val _mtu = MutableStateFlow(517) // Default GATT MTU
    internal val mtu = _mtu.asStateFlow()

    internal fun startScan(filters: List<ScanFilter>?, scanConfig: ScanConfig) {
        // Prevent starting scan if already scanning
        if (isScanning) return
        _connectionState.value = ConnectionState.Scanning
        isScanning = true

        // Build settings based on the config
        val settingsBuilder = ScanSettings.Builder()
            .setScanMode(
                when (scanConfig.scanMode) {
                    ScanMode.LOW_POWER -> ScanSettings.SCAN_MODE_LOW_POWER
                    ScanMode.BALANCED -> ScanSettings.SCAN_MODE_BALANCED
                    ScanMode.LOW_LATENCY -> ScanSettings.SCAN_MODE_LOW_LATENCY
                }
            )
            .setCallbackType(
                when (scanConfig.callbackType) {
                    ScanCallbackType.FIRST_MATCH -> ScanSettings.CALLBACK_TYPE_FIRST_MATCH
                    ScanCallbackType.ALL_MATCHES -> ScanSettings.CALLBACK_TYPE_ALL_MATCHES
                }
            )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settingsBuilder.setLegacy(scanConfig.scanLegacyOnly)
            if (scanConfig.useAllSupportedPhys) {
                settingsBuilder.setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
            }
        }

        val settings = settingsBuilder.build()

        // Start the scan
        try {
            Log.debug { "Starting scan with config: $scanConfig" }
            scanner.startScan(filters, settings, scanCallback)
        } catch (e: Exception) {
            Log.error(e) { "Failed to start scan" }
            _connectionState.value = ConnectionState.Failed("Failed to start scan")
            isScanning = false
            return
        }
    }

    internal fun stopScan() {
        if (!isScanning) return
        isScanning = false

        if (_connectionState.value is ConnectionState.Scanning) {
            _connectionState.value = ConnectionState.Disconnected
        }

        // Stop the scan - handle potential exceptions
        scanner?.stopScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    internal suspend fun connectToDevice(device: BluetoothDevice) {
        if (isScanning) stopScan()

        if (bluetoothGatt != null) {
            Log.warn { "Connection attempt ignored. Already connected or connecting." }
            return
        }

        // Update the state immediately so the UI reflects the "Connecting" status.
        _connectionState.value = ConnectionState.Connecting(device.address)

        withContext(Dispatchers.Main) {
            Log.debug { "Executing connectGatt on main thread for ${device.address}" }
            try {
                bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
                if (bluetoothGatt == null) {
                    // This is a rare failure.
                    Log.error { "device.connectGatt returned null. BT adapter may be off or in a bad state." }
                    _connectionState.value = ConnectionState.Failed("connectGatt returned null")
                    close() // Ensure cleanup
                }
            } catch (e: SecurityException) {
                // This should not happen if permissions are checked
                Log.error(e) { "SecurityException on connectGatt" }
                _connectionState.value = ConnectionState.Failed("Permission denied for connectGatt")
                close()
            }
        }
    }

    internal fun enableIndication() {
        val gatt = bluetoothGatt ?: return
        indicateCharacteristic = gatt.getService(UUID.fromString(config.polServiceUuid))
            ?.getCharacteristic(currentIndicateUuid)

        if (!gatt.device.isConnected(context)) { // Added check for connection state
            Log.error { "Cannot enable indication: GATT not connected." }
            return
        }

        if (indicateCharacteristic == null) {
            Log.error { "Cannot enable indication: Indicate characteristic is null." }
            return
        }

        // Check if characteristic actually supports indication
        if (indicateCharacteristic!!.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE == 0) {
            Log.error { "Characteristic ${indicateCharacteristic!!.uuid} does not support indication." }
            return
        }

        // Enable notification locally
        if (!gatt.setCharacteristicNotification(indicateCharacteristic, true)) {
            Log.error { "setCharacteristicNotification failed for ${indicateCharacteristic!!.uuid}" }
            return
        }

        // Write to CCCD descriptor
        val cccd = indicateCharacteristic!!.getDescriptor(cccdUuid)
        if (cccd == null) {
            Log.error { "CCCD descriptor not found for characteristic ${indicateCharacteristic!!.uuid}" }
            // Disable local notification if CCCD write fails
            gatt.setCharacteristicNotification(indicateCharacteristic, false)
            return
        }

        val value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        Log.debug { "Writing ENABLE_INDICATION_VALUE to CCCD for ${indicateCharacteristic!!.uuid}" }
        writeCccdDescriptor(gatt, cccd, value)
    }

    internal fun disableIndication() {
        val gatt = bluetoothGatt
        val characteristic = indicateCharacteristic ?: return

        if (!gatt!!.setCharacteristicNotification(characteristic, false)) {
            Log.warn { "setCharacteristicNotification(false) failed for ${characteristic.uuid}" }
            return
        }

        val cccd = characteristic.getDescriptor(cccdUuid) ?: return
        val value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        Log.debug { "Writing DISABLE_NOTIFICATION_VALUE to CCCD for ${characteristic.uuid}" }
        writeCccdDescriptor(gatt, cccd, value)
    }

    // Send using byte array directly
    internal fun send(data: ByteArray) {
        val gatt = bluetoothGatt ?: return
        writeCharacteristic = gatt.getService(UUID.fromString(config.polServiceUuid))
            ?.getCharacteristic(currentWriteUuid)
        if (writeCharacteristic == null || _connectionState.value !is ConnectionState.Ready) {
            Log.error { "Cannot send, not in ready state." }
            // If we can't start the write, unblock any potential listener otherwise, the caller could wait forever.
            externalScope.launch { characteristicWriteSignal.trySend(false) }
            return
        }
        writeDataInternal(gatt, writeCharacteristic!!, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
    }

    internal fun setTransactionUuids(writeUuid: String, indicateUuid: String) {
        this.currentWriteUuid = UUID.fromString(writeUuid)
        this.currentIndicateUuid = UUID.fromString(indicateUuid)
    }

    // Public close function using the helper
    internal fun close() {
        if (isScanning) stopScan()
        bluetoothGatt?.close()
        bluetoothGatt = null
        writeCharacteristic = null
        indicateCharacteristic = null
        if (_connectionState.value != ConnectionState.Disconnected) {
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (!isScanning) return
            externalScope.launch { _scanResults.emit(result) }
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
            _connectionState.value = ConnectionState.Failed("Scan Failed with code: $errorCode")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    // State remains Connecting, wait for MTU and services
                    Log.info { "Connected to $deviceAddress. Requesting MTU..." }
                    gatt.requestMtu(config.mtu)
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    _connectionState.value = ConnectionState.Disconnected
                    close()
                }
            } else {
                val errorMsg = gattStatusToString(status)
                Log.error { "Connection state change failed: $errorMsg" }
                _connectionState.value = ConnectionState.Failed("Connection Failed: $errorMsg")
                close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val serviceUuid = config.polServiceUuid
                val service = gatt.getService(UUID.fromString(serviceUuid))

                if (service != null) {
                    Log.info { "Main service $serviceUuid found. Device is ready." }
                    _connectionState.value = ConnectionState.Ready(gatt.device.address)
                } else {
                    Log.error { "Required Polaris service not found." }
                    _connectionState.value = ConnectionState.Failed("Required Polaris service not found.")
                    close()
                }
            } else {
                val errorMsg = gattStatusToString(status)
                Log.error { "Service discovery failed: $errorMsg" }
                _connectionState.value = ConnectionState.Failed("Service Discovery Failed: $errorMsg")
                close()
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            val stat = gattStatusToString(status)
            Log.info { "MTU changed to $mtu" }
            if (status != BluetoothGatt.GATT_SUCCESS) {
                // MTU change failure is not critical. The system just uses the default.
                Log.warn { "MTU change failed: $stat. Proceeding anyway." }
            }
            _mtu.value = mtu
            gatt.discoverServices()
        }

        @Deprecated("Use onCharacteristicChanged(gatt, characteristic, value) for API 33+")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                handleCharacteristicChange(characteristic.uuid, characteristic.value)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            handleCharacteristicChange(characteristic.uuid, value)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.debug { "Write successful for one chunk." }
                // Send a "true" signal to unblock the waiting coroutine.
                externalScope.launch { characteristicWriteSignal.send(true) }
            } else {
                val errorMsg = gattStatusToString(status)
                Log.error { "Characteristic write failed: $errorMsg" }
                // Send a "false" signal to indicate failure.
                externalScope.launch { characteristicWriteSignal.send(false) }

                // Tear down the connection on a write failure.
                _connectionState.value = ConnectionState.Failed("Write Failed: $errorMsg")
                close()
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (descriptor.uuid == cccdUuid) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.debug { "Descriptor write successful for ${descriptor.characteristic.uuid}" }
                    externalScope.launch { descriptorWriteSignal.send(true) }
                } else {
                    Log.error { "Failed to write descriptor (CCCD write failed): ${gattStatusToString(status)}" }
                    externalScope.launch { descriptorWriteSignal.send(false) }
                }
            }
        }

        @Deprecated("Use onCharacteristicRead(gatt, characteristic, value, status) for API 33+")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    @Suppress("DEPRECATION")
                    val value = characteristic.value ?: byteArrayOf()
                    handleCharacteristicRead(characteristic.uuid, value, status)
                } else {
                    handleCharacteristicRead(characteristic.uuid, byteArrayOf(), status)
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int,
        ) {
            handleCharacteristicRead(characteristic.uuid, value, status)
        }

        // Helper to process characteristic changes
        private fun handleCharacteristicChange(uuid: UUID?, value: ByteArray) {
            if (uuid == currentIndicateUuid) {
                Log.debug { "Received ${value.size} bytes on indicate char." }
                externalScope.launch { _receivedData.emit(value) }
            }
        }

        private fun handleCharacteristicRead(uuid: UUID?, value: ByteArray, status: Int) {
            val valueHex = value.joinToString(separator = " ") { "%02X".format(it) }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.debug { "Read from $uuid successful. Value: $valueHex" }
            } else {
                Log.error { "Read from $uuid failed, status: $status" }
            }
        }
    }

    // Helper for writing CCCD descriptor, handling API levels
    @SuppressLint("MissingPermission")
    private fun writeCccdDescriptor(gatt: BluetoothGatt, cccd: BluetoothGattDescriptor, value: ByteArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+ uses new method
            val result = gatt.writeDescriptor(cccd, value)
            Log.debug { "gatt.writeDescriptor(cccd, value) called for API 33+, result code: $result" }
            // Completion is signaled by onDescriptorWrite callback.
        } else {
            // Fallback for pre-API 33 (deprecated methods)
            @Suppress("DEPRECATION")
            cccd.value = value
            @Suppress("DEPRECATION")
            val success = gatt.writeDescriptor(cccd)
            Log.debug { "gatt.writeDescriptor(cccd) called for API < 33, success: $success" }
        }
    }

    // Internal helper for writing characteristic data, handling API levels
    @Suppress("SameParameterValue")
    @SuppressLint("MissingPermission")
    private fun writeDataInternal(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray,
        writeType: Int,
    ) {
        lastWrittenValue = data
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val result = gatt.writeCharacteristic(characteristic, data, writeType)
            Log.debug { "gatt.writeCharacteristic(char, data, type) called for API 33+, result code: $result" }
        } else {
            characteristic.writeType = writeType
            @Suppress("DEPRECATION")
            characteristic.value = data
            @Suppress("DEPRECATION")
            val success = gatt.writeCharacteristic(characteristic)
            Log.debug { "gatt.writeCharacteristic(char) called for API < 33, success: $success" }
        }
    }
}