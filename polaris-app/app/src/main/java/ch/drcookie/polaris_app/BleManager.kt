package ch.drcookie.polaris_app

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import java.util.*

class BleManager(
    private val context: Context,
    private val bleListener: BleListener
) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val scanner = bluetoothAdapter.bluetoothLeScanner
    private var lastWrittenValue: ByteArray? = null
    @Volatile private var bluetoothGatt: BluetoothGatt? = null
    private val handler = Handler(Looper.getMainLooper())
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var indicateCharacteristic: BluetoothGattCharacteristic? = null
    private val serviceUuid = UUID.fromString("f44dce36-ffb2-565b-8494-25fa5a7a7cd6")
    private val writeCharUuid = UUID.fromString("8e8c14b7-d9f0-5e5c-9da8-6961e1f33d6b")
    private val indicateCharUuid = UUID.fromString("d234a7d8-ea1f-5299-8221-9cf2f942d3df")
    private val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    private var scanTimeoutRunnable: Runnable? = null
    private val scanTimeout = 10000L
    private val requestedMtu = 517
    @Volatile private var isScanning = false
    private val TAG = "BleManager"

    private fun logDebug(message: String) {
        Log.d(TAG, message)
        bleListener.onDebugMessage(message)
    }

    private fun logError(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
        bleListener.onDebugMessage("ERROR: $message")
    }

    private fun logWarn(message: String) {
        Log.w(TAG, message)
        bleListener.onDebugMessage("WARN: $message")
    }

    private val scanCallback = object : ScanCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            // Ignore results if not scanning anymore
            if (!isScanning) {
                logWarn("Scan result received after scan stopped for ${result.device.address}")
                return
            }
            stopScan() // Stop scanning as soon as a device is found
            logDebug("Device found: ${result.device.name ?: "Unnamed"} (${result.device.address})")
            connectToDevice(result.device)
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false // Ensure state is updated on failure
            logError("Scan failed with code: $errorCode")
            // Make sure to remove the timeout callback if scan fails
            scanTimeoutRunnable?.let { handler.removeCallbacks(it) }
            scanTimeoutRunnable = null
            bleListener.onDeviceNotFound() // Notify listener
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScan() {
        // Prevent starting scan if already scanning
        if (isScanning) {
            logWarn("Scan already in progress.")
            return
        }
        // Check if Bluetooth is enabled
        if (bluetoothAdapter?.isEnabled != true) {
            logError("Cannot start scan, Bluetooth is disabled.")
            bleListener.onDebugMessage("Bluetooth is disabled.") // Inform listener
            return
        }
        // Check if scanner is available (can be null if adapter is off)
        if (scanner == null) {
            logError("Cannot start scan, BluetoothLeScanner is unavailable.")
            bleListener.onDebugMessage("BLE Scanner unavailable.")
            return
        }

        logDebug("Starting BLE scan...")
        isScanning = true
        val filters = listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(serviceUuid)).build())
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH) // More efficient if you only need one
            .build()

        // Start the scan
        try {
            scanner.startScan(filters, settings, scanCallback)
        } catch (e: SecurityException) {
            logError("Missing BLUETOOTH_SCAN permission?", e)
            isScanning = false
            bleListener.onDebugMessage("Permission denied for scanning.")
            return // Exit early
        } catch (e: IllegalStateException) {
            logError("Could not start scan (adapter disabled?)", e)
            isScanning = false
            bleListener.onDebugMessage("Could not start scan (Bluetooth off?).")
            return // Exit early
        }


        // Setup scan timeout
        scanTimeoutRunnable = Runnable {
            if (isScanning) {
                logWarn("Scan timed out.")
                stopScan() // Ensure scan is stopped
                bleListener.onDeviceNotFound() // Notify listener
            }
        }
        handler.postDelayed(scanTimeoutRunnable!!, scanTimeout)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScan() {
        if (!isScanning) {
//            logDebug("Scan already stopped.") // Optional: can be noisy
            return
        }
        logDebug("Stopping scan...")
        isScanning = false
        // Remove timeout callback
        scanTimeoutRunnable?.let { handler.removeCallbacks(it) }
        scanTimeoutRunnable = null

        // Stop the scan - handle potential exceptions
        try {
            scanner?.stopScan(scanCallback)
        } catch (e: SecurityException) {
            logError("Missing BLUETOOTH_SCAN permission during stopScan?", e)
        } catch (e: IllegalStateException) {
            logError("Could not stop scan (adapter disabled?)", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        // Ensure scan is stopped before connecting
        if (isScanning) {
            logWarn("Scan was still active when connectToDevice called. Stopping scan.")
            stopScan()
        }

        // Prevent multiple connection attempts
        if (bluetoothGatt != null) {
            logWarn("Connection attempt ignored. Already connected or connecting to ${bluetoothGatt?.device?.address}")
            return
        }

        logDebug("Connecting to GATT server for ${device.address}...")
        handler.post {
            try {
                bluetoothGatt = device.connectGatt(context, false, gattCallback, TRANSPORT_LE)
                if (bluetoothGatt == null) {
                    logError("device.connectGatt returned null for ${device.address}. Potential BT adapter issue.")
                    // Notify failure if connectGatt itself fails immediately
                    bleListener.onConnectionFailed(BluetoothGatt.GATT_FAILURE)
                }
            } catch (e: SecurityException) {
                logError("Missing BLUETOOTH_CONNECT permission for connectGatt?", e)
                bleListener.onConnectionFailed(BluetoothGatt.GATT_FAILURE)
            }
        }
    }

    /** Helper function to close GATT connection and clean up resources. */
    @SuppressLint("MissingPermission")
    private fun closeGattAndCleanup(gattInstance: BluetoothGatt? = bluetoothGatt) {
        val gattToClose = gattInstance ?: bluetoothGatt
        val deviceAddress = gattToClose?.device?.address ?: "Unknown device"

        if (gattToClose != null) {
            logDebug("Closing GATT connection for beacon")
            try {
                gattToClose.close() // Release system resources
            } catch (e: SecurityException) {
                logError("Missing BLUETOOTH_CONNECT permission during close?", e)
            }

            if (gattToClose == this.bluetoothGatt) {
                this.bluetoothGatt = null
                writeCharacteristic = null
                indicateCharacteristic = null
                logDebug("GATT resources cleaned up for $deviceAddress.")
            } else {
                logDebug("Closed specific GATT instance for $deviceAddress, main instance remains unaffected (if any).")
            }
        } else {
            logDebug("closeGattAndCleanup called but GATT instance was already null.")
        }
    }

    // Public close function using the helper
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun close() {
        logDebug("External close() called.")
        // Also stop scanning if closing externally
        if (isScanning) {
            stopScan()
        }
        closeGattAndCleanup()
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device?.address ?: "Unknown" // Use gatt.device.address safely
            logDebug("onConnectionStateChange: device=$deviceAddress, status=$status, newState=$newState")

            handler.post {
                if (this@BleManager.bluetoothGatt == null && newState != BluetoothProfile.STATE_CONNECTED) {
                    // Ignore callbacks for GATT instances that we've already cleaned up,
                    // unless it's the initial connection callback itself.
                    logWarn("onConnectionStateChange received for a cleaned-up GATT instance. Ignoring.")
                    // Make sure to close the passed gatt instance if it's not null and not the one we expect
                    if (gatt != this@BleManager.bluetoothGatt) {
                        try { gatt.close() } catch (e: Exception) { /* Ignore */ }
                    }
                    return@post
                }

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    when (newState) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            logDebug("Connected successfully to GATT server $deviceAddress.")
                            if (bluetoothGatt != null && bluetoothGatt != gatt) {
                                logWarn("New GATT connection established while another existed. Closing old one.")
                                closeGattAndCleanup(bluetoothGatt)
                            }
                            bluetoothGatt = gatt

                            logDebug("Attempting to request MTU of $requestedMtu for beacon...")
                            if (!gatt.requestMtu(requestedMtu)) {
                                logError("Failed to initiate MTU request for beacon.")
                                // If MTU request fails to even start, treat as connection failure
                                bleListener.onConnectionFailed(BluetoothGatt.GATT_FAILURE)
                                closeGattAndCleanup(gatt)
                            } else {
                                logDebug("MTU request initiated.")
                            }
                        }
                        BluetoothProfile.STATE_DISCONNECTED -> {
                            logDebug("Disconnected from GATT server $deviceAddress.")
                            // Clean up resources when disconnected
                            // Pass 'gatt' to ensure we close the specific instance that disconnected
                            bleListener.onConnectionFailed(status) // Notify listener about disconnection
                            closeGattAndCleanup(gatt)
                        }
                    }
                } else {
                    // Connection attempt failed or existing connection lost with an error
                    val errorMsg = when(status) {
                        133 -> "GATT_ERROR (Generic failure, timeout, resource issue, bonding?)" // Most common vague error
                        8 -> "GATT_INSUFFICIENT_ENCRYPTION (Bonding/encryption required?)"
                        15 -> "GATT_INSUFFICIENT_AUTHENTICATION (Bonding/authentication required?)"
                        257 -> "GATT_INTERNAL_ERROR (Stack error?)"
                        5 -> "GATT_INSUFFICIENT_AUTHORIZATION"
                        // Add other relevant codes from BluetoothGatt constants if needed
                        else -> "Unknown error status"
                    }
                    logError("GATT connection error on $deviceAddress: status=$status ($errorMsg)")
                    // CRITICAL: Close the GATT object to release resources on any failure
                    // Pass 'gatt' to ensure we close the specific instance that failed
                    bleListener.onConnectionFailed(status) // Notify listener about the failure
                    closeGattAndCleanup(gatt)
                }
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val deviceAddress = gatt.device?.address ?: "Unknown"
            logDebug("onServicesDiscovered: device=$deviceAddress, status=$status")

            // Ensure callbacks to listener run on main thread
            handler.post {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    logDebug("Services discovered successfully for $deviceAddress.")
                    val service = gatt.getService(serviceUuid)
                    if (service != null) {
                        logDebug("Required service $serviceUuid found.")
                        // Assign characteristics
                        writeCharacteristic = service.getCharacteristic(writeCharUuid)
                        indicateCharacteristic = service.getCharacteristic(indicateCharUuid)

                        if (writeCharacteristic == null || indicateCharacteristic == null) {
                            logError("Required characteristics not found on $deviceAddress. WriteFound=${writeCharacteristic != null}, IndicateFound=${indicateCharacteristic != null}")
                            // Close connection if essential characteristics are missing
                            bleListener.onConnectionFailed(status) // Indicate failure reason
                            closeGattAndCleanup(gatt)
                            return@post
                        }

                        logDebug("Required characteristics found on $deviceAddress. Ready.")
                        // Notify listener that the device is ready for interaction
                        bleListener.onReady()
                    } else {
                        logError("Required service $serviceUuid not found on $deviceAddress.")
                        // Close connection if essential service is missing
                        bleListener.onConnectionFailed(status) // Indicate failure reason
                        closeGattAndCleanup(gatt)
                    }
                } else {
                    logError("Service discovery failed for $deviceAddress with status: $status")
                    // Close connection on discovery failure
                    bleListener.onConnectionFailed(status) // Indicate failure reason
                    closeGattAndCleanup(gatt)
                }
            }
        }

        // MTU Change - Currently commented out in original code, but good practice to handle
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            logDebug("onMtuChanged: beacon: mtu=$mtu, status=$status")

            handler.post {
                if (gatt != bluetoothGatt) {
                    logWarn("onMtuChanged received for an old/unexpected GATT instance. Ignoring.")
                    return@post
                }

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    logDebug("MTU changed successfully to $mtu for beacon.")
                } else {
                    logError("MTU change request failed for beacon, status: $status. Proceeding with discovery anyway.")
                }

                logDebug("Attempting service discovery after MTU negotiation for beacon...")
                try {
                    if (!gatt.discoverServices()) {
                        logError("Failed to initiate service discovery for beacon after MTU change.")
                        bleListener.onConnectionFailed(BluetoothGatt.GATT_FAILURE) // Generic failure
                        closeGattAndCleanup(gatt)
                    } else {
                        logDebug("Service discovery initiated after MTU negotiation.")
                    }
                } catch (e: SecurityException) {
                    logError("Missing BLUETOOTH_CONNECT permission for discoverServices?", e)
                    bleListener.onConnectionFailed(BluetoothGatt.GATT_FAILURE)
                    closeGattAndCleanup(gatt)
                }
            }
        }

        @Deprecated("Use onCharacteristicChanged(gatt, characteristic, value) for API 33+")
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                val value = characteristic.value ?: byteArrayOf() // Use characteristic.value
                handleCharacteristicChange(characteristic.uuid, value)
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleCharacteristicChange(characteristic.uuid, value)
        }

        // Helper to process characteristic changes
        @SuppressLint("MissingPermission")
        private fun handleCharacteristicChange(uuid: UUID?, value: ByteArray) {
            val valueHex = value.joinToString(separator = " ") { "%02X".format(it) }
            logDebug("Characteristic $uuid changed (Indication/Notification): RECEIVED ${value.size} bytes -> $valueHex")
            if (uuid == indicateCharUuid) {
                handler.post { bleListener.onMessageReceived(value) }
                handler.post { close() }
            } else {
                logWarn("Change received for unexpected characteristic: $uuid")
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            val valueWritten = lastWrittenValue
            val valueHex = valueWritten?.joinToString(separator = " ") { "%02X".format(it) } ?: "N/A"

            if (status == BluetoothGatt.GATT_SUCCESS) {
                logDebug("Write to beacon successful. Value: $valueHex")
            } else {
                logError("Write to beacon failed, status: $status")
            }
            lastWrittenValue = null
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            val deviceAddress = gatt.device?.address ?: "Unknown"
            val charUuid = descriptor.characteristic?.uuid ?: "Unknown Char"
            handler.post {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    logDebug("Write to descriptor ${descriptor.uuid} (for char $charUuid) successful for $deviceAddress.")
                    if (descriptor.uuid == cccdUuid && descriptor.characteristic.uuid == indicateCharUuid) {
                        logDebug("Indications enabled successfully (CCCD write acknowledged).")
                        bleListener.onIndicationEnabled()
                    }
                } else {
                    logError("Write to descriptor ${descriptor.uuid} (for char $charUuid) failed for $deviceAddress, status: $status")
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
                logDebug("Read from $uuid successful. Value: $valueHex")
            } else {
                logError("Read from $uuid failed, status: $status")
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun enableIndication() {
        val gatt = bluetoothGatt
        val characteristic = indicateCharacteristic

        if (gatt == null || !gatt.device.isConnected(context)) { // Added check for connection state
            logError("Cannot enable indication: GATT not connected.")
            return
        }
        if (characteristic == null) {
            logError("Cannot enable indication: Indicate characteristic is null.")
            return
        }
        // Check if characteristic actually supports indication
        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE == 0) {
            logError("Characteristic ${characteristic.uuid} does not support indication.")
            return
        }

        // Enable notification locally
        if (!gatt.setCharacteristicNotification(characteristic, true)) {
            logError("setCharacteristicNotification failed for ${characteristic.uuid}")
            return
        }

        // Write to CCCD descriptor
        val cccd = characteristic.getDescriptor(cccdUuid)
        if (cccd == null) {
            logError("CCCD descriptor not found for characteristic ${characteristic.uuid}")
            // Disable local notification if CCCD write fails
            gatt.setCharacteristicNotification(characteristic, false)
            return
        }

        val value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        logDebug("Writing ENABLE_INDICATION_VALUE to CCCD for ${characteristic.uuid}")
        writeCccdDescriptor(gatt, cccd, value)
    }

    // Helper for writing CCCD descriptor, handling API levels
    @SuppressLint("MissingPermission")
    private fun writeCccdDescriptor(gatt: BluetoothGatt, cccd: BluetoothGattDescriptor, value: ByteArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+ uses new method
            val result = gatt.writeDescriptor(cccd, value)
            logDebug("gatt.writeDescriptor(cccd, value) called for API 33+, result code: $result") // Log the return code (0 for success)
            // Completion is signaled by onDescriptorWrite callback.
        } else {
            // Fallback for pre-API 33 (deprecated methods)
            @Suppress("DEPRECATION")
            cccd.value = value
            @Suppress("DEPRECATION")
            val success = gatt.writeDescriptor(cccd)
            logDebug("gatt.writeDescriptor(cccd) called for API < 33, success: $success")
        }
    }


    // Simplified send using byte array directly
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun send(data: ByteArray) {
        val gatt = bluetoothGatt
        val characteristic = writeCharacteristic

        if (gatt == null || !gatt.device.isConnected(context)) {
            logError("Cannot send data: GATT not connected.")
            return
        }
        if (characteristic == null) {
            logError("Cannot send data: Write characteristic is null.")
            return
        }
        // Check if characteristic supports write (default or no response)
        val props = characteristic.properties
        if (props and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == 0) {
            logError("Characteristic ${characteristic.uuid} does not support writes.")
            return
        }

        // Determine write type (prefer default write with response)
        val writeType = if (props and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        } else {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        }

        logDebug("Sending ${data.size} bytes to the beacon")
        writeDataInternal(gatt, characteristic, data, writeType)
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
            logDebug("gatt.writeCharacteristic(char, data, type) called for API 33+, result code: $result")
        } else {
            characteristic.writeType = writeType
            @Suppress("DEPRECATION")
            characteristic.value = data
            @Suppress("DEPRECATION")
            val success = gatt.writeCharacteristic(characteristic)
            logDebug("gatt.writeCharacteristic(char) called for API < 33, success: $success")
        }
    }

    // isReady check remains useful
    fun isReady(): Boolean {
        // Check gatt connection state as well
        val gattConnected = bluetoothGatt?.let { gatt ->
            try {
                bluetoothManager.getConnectionState(gatt.device, BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTED
            } catch (e: SecurityException) {
                logError("Permission missing for getConnectionState", e)
                false
            }
        } ?: false
        return gattConnected && writeCharacteristic != null && indicateCharacteristic != null
    }

    // Helper to check connection state (requires BLUETOOTH_CONNECT)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun isConnected(): Boolean {
        return bluetoothGatt?.device?.let { device ->
            bluetoothManager.getConnectionState(device, BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTED
        } ?: false
    }

}

// Helper extension function (place outside BleManager or in a separate utility file)
@SuppressLint("MissingPermission")
fun BluetoothDevice.isConnected(context: Context): Boolean {
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    return try {
        bluetoothManager.getConnectionState(this, BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTED
    } catch (e: SecurityException) {
        Log.e("DeviceExt", "Permission missing for getConnectionState", e)
        false
    }
}