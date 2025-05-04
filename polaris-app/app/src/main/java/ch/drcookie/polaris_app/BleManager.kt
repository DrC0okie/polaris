package ch.drcookie.polaris_app

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import androidx.annotation.RequiresPermission
import java.util.*

class BleManager(
    private val context: Context,
    private val listener: Listener
) {

    interface Listener {
        fun onDebugMessage(message: String)
        fun onMessageReceived(message: String)
        fun onDeviceNotFound()
        fun onReady()
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val scanner = bluetoothAdapter.bluetoothLeScanner
    private var bluetoothGatt: BluetoothGatt? = null
    private var timeoutRunnable: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())

    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var indicateCharacteristic: BluetoothGattCharacteristic? = null

    private val serviceUuid = UUID.fromString("f44dce36-ffb2-565b-8494-25fa5a7a7cd6")
    private val writeCharUuid = UUID.fromString("8e8c14b7-d9f0-5e5c-9da8-6961e1f33d6b")
    private val indicateCharUuid = UUID.fromString("d234a7d8-ea1f-5299-8221-9cf2f942d3df")
    private val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    private var connectionRetries = 0
    private val maxRetries = 1

    private val scanCallback = object : ScanCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            stopScan()
            listener.onDebugMessage("Device found: ${result.device.address}")
            connectToDevice(result.device)
        }

        override fun onScanFailed(errorCode: Int) {
            listener.onDebugMessage("Scan failed with code: $errorCode")
            listener.onDeviceNotFound()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun start() {
        listener.onDebugMessage("Starting BLE scan...")
        val filters = listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(serviceUuid)).build())
        val mode = ScanSettings.SCAN_MODE_LOW_LATENCY
        val settings = ScanSettings.Builder().setScanMode(mode).build()
        scanner.startScan(filters, settings, scanCallback)

        timeoutRunnable = Runnable {
            stopScan()
            listener.onDeviceNotFound()
        }
        handler.postDelayed(timeoutRunnable!!, 10000)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun stopScan() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        scanner.stopScan(scanCallback)
        timeoutRunnable = null
        listener.onDebugMessage("Stopping scan...")
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        listener.onDebugMessage("Connecting to GATT server...")
        handler.postDelayed({
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
        }, 300)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device?.address ?: "Unknown"
            listener.onDebugMessage("onConnectionStateChange: status=$status, newState=$newState (device: $deviceAddress)")

            if (status == 133 && connectionRetries < maxRetries) {
                listener.onDebugMessage("GATT error 133: retrying connection...")
                gatt.close()
                connectionRetries++
                Handler(Looper.getMainLooper()).postDelayed({
                    connectToDevice(gatt.device)
                }, 300)
                return
            }

            connectionRetries = 0

            if (status == BluetoothGatt.GATT_SUCCESS) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        listener.onDebugMessage("Connected to GATT server.")
                        Handler(Looper.getMainLooper()).postDelayed({
                            gatt.requestMtu(517)
                        }, 100)
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        listener.onDebugMessage("Disconnected from GATT server.")
                    }
                }
            } else {
                listener.onDebugMessage("GATT connection failed with status: $status")
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            listener.onDebugMessage("MTU changed to $mtu. Discovering services...")
            gatt.discoverServices()
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            listener.onDebugMessage("Services discovered.")
            val service = gatt.getService(serviceUuid)
            if (service != null) {
                writeCharacteristic = service.getCharacteristic(writeCharUuid)
                indicateCharacteristic = service.getCharacteristic(indicateCharUuid)

                if (writeCharacteristic == null || indicateCharacteristic == null) {
                    listener.onDebugMessage("Required characteristics not found.")
                    return
                }

                listener.onDebugMessage("Characteristics ready.")
                listener.onReady()
            } else {
                listener.onDebugMessage("Service not found.")
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            val message = value.decodeToString()
            listener.onDebugMessage("Indication received: $message")
            listener.onMessageReceived(message)
            close()
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            listener.onDebugMessage("Write completed with status: $status")
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            listener.onDebugMessage("CCCD descriptor write status: $status")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun enableIndication() {
        val gatt = bluetoothGatt
        val characteristic = indicateCharacteristic
        if (gatt != null && characteristic != null) {
            enableIndication(gatt, characteristic)
        } else {
            listener.onDebugMessage("Cannot enable indication: not connected or characteristic missing.")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun sendData(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, data: ByteArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(characteristic, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        } else {
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            @Suppress("DEPRECATION")
            characteristic.value = data
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(characteristic)
        }

        listener.onDebugMessage("Sending ${data.size} bytes")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun send(data: ByteArray) {
        val gatt = bluetoothGatt
        val characteristic = writeCharacteristic
        if (gatt != null && characteristic != null) {
            sendData(gatt, characteristic, data)
        } else {
            listener.onDebugMessage("Cannot send: not connected or characteristic missing.")
        }
    }

    fun isReady(): Boolean {
        return bluetoothGatt != null && writeCharacteristic != null && indicateCharacteristic != null
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun enableIndication(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
        val cccd = characteristic.getDescriptor(cccdUuid)
        val value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(cccd, value)
        } else {
            // fallback for pre-API 33
            @Suppress("DEPRECATION")
            cccd.value = value
            @Suppress("DEPRECATION")
            gatt.writeDescriptor(cccd)
        }
        listener.onDebugMessage("Indication enabled.")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun sendData(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, message: String) {
        val data = message.toByteArray()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(characteristic, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        } else {
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            @Suppress("DEPRECATION")
            characteristic.value = data
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(characteristic)
        }

        listener.onDebugMessage("Sending: $message")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun close() {
        bluetoothGatt?.close()
        bluetoothGatt = null
        listener.onDebugMessage("GATT connection closed.")
    }
}