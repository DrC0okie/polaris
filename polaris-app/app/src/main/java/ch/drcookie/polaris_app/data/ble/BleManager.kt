package ch.drcookie.polaris_app.data.ble

import android.Manifest
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
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import ch.drcookie.polaris_app.data.ble.BleUtils.isConnected
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

@SuppressLint("MissingPermission")
class BleManager(private val context: Context, private val externalScope: CoroutineScope) {

    val writeSignal = Channel<Boolean>(Channel.RENDEZVOUS)
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter
    private val scanner = bluetoothAdapter.bluetoothLeScanner
    private var lastWrittenValue: ByteArray? = null
    @Volatile
    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var indicateCharacteristic: BluetoothGattCharacteristic? = null
    private val serviceUuid = UUID.fromString("f44dce36-ffb2-565b-8494-25fa5a7a7cd6")
    private val writeCharUuid = UUID.fromString("8e8c14b7-d9f0-5e5c-9da8-6961e1f33d6b")
    private val indicateCharUuid = UUID.fromString("d234a7d8-ea1f-5299-8221-9cf2f942d3df")
    private val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    @Volatile
    private var isScanning = false

    // Flow variables
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    private val _scanResults = MutableSharedFlow<ScanResult>()
    val scanResults = _scanResults.asSharedFlow()

    private val _receivedData = MutableSharedFlow<ByteArray>()
    val receivedData = _receivedData.asSharedFlow()

    private val _mtu = MutableStateFlow(23) // Default GATT MTU
    val mtu = _mtu.asStateFlow()

    companion object{
        const val REQ_MTU = 23
        const val TAG = "BleManager"
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private val scanCallback = object : ScanCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (!isScanning) return
            externalScope.launch { _scanResults.emit(result) }
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
            _connectionState.value = ConnectionState.Failed("Scan Failed with code: $errorCode")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScan() {
        // Prevent starting scan if already scanning
        if (isScanning) return
        _connectionState.value = ConnectionState.Scanning
        isScanning = true

        val filters = listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(serviceUuid)).build())
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
            .build()

        // Start the scan
        try {
            scanner.startScan(filters, settings, scanCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start scan", e)
            _connectionState.value = ConnectionState.Failed("Failed to start scan")
            isScanning = false
            return
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScan() {
        if (!isScanning) return
        isScanning = false

        if (_connectionState.value is ConnectionState.Scanning) {
            _connectionState.value = ConnectionState.Disconnected
        }

        // Stop the scan - handle potential exceptions
        scanner?.stopScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    suspend fun connectToDevice(device: BluetoothDevice) {
        if (isScanning) stopScan()

        if (bluetoothGatt != null) {
            Log.w(TAG, "Connection attempt ignored. Already connected or connecting.")
            return
        }

        // Update the state immediately so the UI reflects the "Connecting" status.
        _connectionState.value = ConnectionState.Connecting(device.address)

        withContext(Dispatchers.Main) {
            Log.d(TAG, "Executing connectGatt on main thread for ${device.address}")
            try {
                bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
                if (bluetoothGatt == null) {
                    // This is a rare failure.
                    Log.e(TAG, "device.connectGatt returned null. BT adapter may be off or in a bad state.")
                    _connectionState.value = ConnectionState.Failed("connectGatt returned null")
                    close() // Ensure cleanup
                }
            } catch (e: SecurityException) {
                // This should not happen if permissions are checked
                Log.e(TAG, "SecurityException on connectGatt", e)
                _connectionState.value = ConnectionState.Failed("Permission denied for connectGatt")
                close()
            }
        }
    }

    // Public close function using the helper
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun close() {
        if (isScanning) stopScan()
        bluetoothGatt?.close()
        bluetoothGatt = null
        writeCharacteristic = null
        indicateCharacteristic = null
        if (_connectionState.value != ConnectionState.Disconnected) {
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    // State remains Connecting, wait for MTU and services
                    Log.i(TAG, "Connected to $deviceAddress. Requesting MTU...")
                    gatt.requestMtu(REQ_MTU)
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    _connectionState.value = ConnectionState.Disconnected
                    close()
                }
            } else {
                val errorMsg = gattStatusToString(status)
                Log.e(TAG, "Connection state change failed: $errorMsg")
                _connectionState.value = ConnectionState.Failed("Connection Failed: $errorMsg")
                close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Find characteristics
                val service = gatt.getService(serviceUuid)
                writeCharacteristic = service?.getCharacteristic(writeCharUuid)
                indicateCharacteristic = service?.getCharacteristic(indicateCharUuid)

                if (writeCharacteristic == null || indicateCharacteristic == null) {
                    Log.e(TAG, "Service discovery failed: Required characteristics not found.")
                    _connectionState.value = ConnectionState.Failed("Required characteristics not found.")
                    close()
                } else {
                    enableIndication()
                }
            } else {
                val errorMsg = gattStatusToString(status)
                Log.e(TAG, "Service discovery failed: $errorMsg")
                _connectionState.value = ConnectionState.Failed("Service Discovery Failed: $errorMsg")
                close()
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            val stat = gattStatusToString(status)
            Log.i(TAG, "MTU changed to $mtu")
            if (status != BluetoothGatt.GATT_SUCCESS) {
                // MTU change failure is not critical. The system just uses the default.
                Log.w(TAG, "MTU change failed: $stat. Proceeding anyway.")
            }
            _mtu.value = mtu
            gatt.discoverServices()
        }

        @Deprecated("Use onCharacteristicChanged(gatt, characteristic, value) for API 33+")
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                handleCharacteristicChange(characteristic.uuid, characteristic.value)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            handleCharacteristicChange(characteristic.uuid, value)
        }

        // Helper to process characteristic changes
        private fun handleCharacteristicChange(uuid: UUID?, value: ByteArray) {
            if (uuid == indicateCharUuid) {
                Log.d(TAG, "Received ${value.size} bytes on indicate char.")
                externalScope.launch { _receivedData.emit(value) }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Write successful for one chunk.")
                // Send a "true" signal to unblock the waiting coroutine.
                externalScope.launch { writeSignal.send(true) }
            } else {
                val errorMsg = gattStatusToString(status)
                Log.e(TAG, "Characteristic write failed: $errorMsg")
                // Send a "false" signal to indicate failure.
                externalScope.launch { writeSignal.send(false) }

                // Tear down the connection on a write failure.
                _connectionState.value = ConnectionState.Failed("Write Failed: $errorMsg")
                close()
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (descriptor.uuid == cccdUuid) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    _connectionState.value = ConnectionState.Ready(gatt.device.address)
                } else {
                    val errorMsg = gattStatusToString(status)
                    Log.e(TAG, "Failed to enable indications (CCCD write failed): $errorMsg")
                    _connectionState.value = ConnectionState.Failed("Enable Indications Failed: $errorMsg")
                    close()
                }
            }
        }

        @Deprecated("Use onCharacteristicRead(gatt, characteristic, value, status) for API 33+")
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
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

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            handleCharacteristicRead(characteristic.uuid, value, status)
        }

        private fun handleCharacteristicRead(uuid: UUID?, value: ByteArray, status: Int) {
            val valueHex = value.joinToString(separator = " ") { "%02X".format(it) }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Read from $uuid successful. Value: $valueHex")
            } else {
                Log.e(TAG, "Read from $uuid failed, status: $status")
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun enableIndication() {
        val gatt = bluetoothGatt
        val characteristic = indicateCharacteristic

        if (gatt == null || !gatt.device.isConnected(context)) { // Added check for connection state
            Log.e(TAG, "Cannot enable indication: GATT not connected.")
            return
        }
        if (characteristic == null) {
            Log.e(TAG, "Cannot enable indication: Indicate characteristic is null.")
            return
        }
        // Check if characteristic actually supports indication
        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE == 0) {
            Log.e(TAG, "Characteristic ${characteristic.uuid} does not support indication.")
            return
        }

        // Enable notification locally
        if (!gatt.setCharacteristicNotification(characteristic, true)) {
            Log.e(TAG, "setCharacteristicNotification failed for ${characteristic.uuid}")
            return
        }

        // Write to CCCD descriptor
        val cccd = characteristic.getDescriptor(cccdUuid)
        if (cccd == null) {
            Log.e(TAG, "CCCD descriptor not found for characteristic ${characteristic.uuid}")
            // Disable local notification if CCCD write fails
            gatt.setCharacteristicNotification(characteristic, false)
            return
        }

        val value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        Log.d(TAG, "Writing ENABLE_INDICATION_VALUE to CCCD for ${characteristic.uuid}")
        writeCccdDescriptor(gatt, cccd, value)
    }

    // Helper for writing CCCD descriptor, handling API levels
    @SuppressLint("MissingPermission")
    private fun writeCccdDescriptor(gatt: BluetoothGatt, cccd: BluetoothGattDescriptor, value: ByteArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+ uses new method
            val result = gatt.writeDescriptor(cccd, value)
            Log.d(TAG, "gatt.writeDescriptor(cccd, value) called for API 33+, result code: $result") // Log the return code (0 for success)
            // Completion is signaled by onDescriptorWrite callback.
        } else {
            // Fallback for pre-API 33 (deprecated methods)
            @Suppress("DEPRECATION")
            cccd.value = value
            @Suppress("DEPRECATION")
            val success = gatt.writeDescriptor(cccd)
            Log.d(TAG, "gatt.writeDescriptor(cccd) called for API < 33, success: $success")
        }
    }


    // Send using byte array directly
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun send(data: ByteArray) {
        val gatt = bluetoothGatt
        val char = writeCharacteristic
        if (gatt == null || char == null || _connectionState.value !is ConnectionState.Ready) {
            Log.e(TAG, "Cannot send, not in ready state.")
            // If we can't start the write, unblock any potential listener otherwise, the caller could wait forever.
            externalScope.launch { writeSignal.trySend(false) }
            return
        }
        writeDataInternal(gatt, char, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
    }

    // Internal helper for writing characteristic data, handling API levels
    @SuppressLint("MissingPermission")
    private fun writeDataInternal(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray,
        writeType: Int
    ) {
        lastWrittenValue = data
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val result = gatt.writeCharacteristic(characteristic, data, writeType)
            Log.d(TAG, "gatt.writeCharacteristic(char, data, type) called for API 33+, result code: $result")
        } else {
            characteristic.writeType = writeType
            @Suppress("DEPRECATION")
            characteristic.value = data
            @Suppress("DEPRECATION")
            val success = gatt.writeCharacteristic(characteristic)
            Log.d(TAG, "gatt.writeCharacteristic(char) called for API < 33, success: $success")
        }
    }

    private fun gattStatusToString(status: Int): String {
        return when (status) {
            BluetoothGatt.GATT_SUCCESS -> "SUCCESS"
            8 -> "GATT_INSUFFICIENT_ENCRYPTION"
            15 -> "GATT_INSUFFICIENT_AUTHENTICATION"
            133 -> "GATT_ERROR (Timeout or resource issue)"
            257 -> "GATT_INTERNAL_ERROR (Stack issue)"
            5 -> "GATT_INSUFFICIENT_AUTHORIZATION"
            else -> "Unknown GATT Error ($status)"
        }
    }
}