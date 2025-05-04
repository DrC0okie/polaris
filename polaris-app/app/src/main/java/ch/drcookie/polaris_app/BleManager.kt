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
    private val handler = Handler(Looper.getMainLooper())

    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var indicateCharacteristic: BluetoothGattCharacteristic? = null

    private val serviceUuid = UUID.fromString("f44dce36-ffb2-565b-8494-25fa5a7a7cd6")
    private val writeCharUuid = UUID.fromString("8e8c14b7-d9f0-5e5c-9da8-6961e1f33d6b")
    private val indicateCharUuid = UUID.fromString("d234a7d8-ea1f-5299-8221-9cf2f942d3df")
    private val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private val commandQueue: Queue<() -> Unit> = LinkedList()
    private var commandInProgress = false

    private fun enqueueCommand(command: () -> Unit) {
        commandQueue.offer(command)
        if (!commandInProgress) executeNextCommand()
    }

    private fun executeNextCommand() {
        val next = commandQueue.poll() ?: return.also { commandInProgress = false }
        commandInProgress = true
        next()
    }

    private fun commandCompleted() {
        commandInProgress = false
        executeNextCommand()
    }

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
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanner.startScan(filters, settings, scanCallback)

        handler.postDelayed({
            stopScan()
            listener.onDeviceNotFound()
        }, 10000)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun stopScan() {
        scanner.stopScan(scanCallback)
        listener.onDebugMessage("Stopping scan...")
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        listener.onDebugMessage("Connecting to GATT server...")
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            listener.onDebugMessage("onConnectionStateChange: status=$status, newState=$newState")
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                enqueueCommand { gatt.requestMtu(517) }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                listener.onDebugMessage("Disconnected from GATT server.")
            } else {
                listener.onDebugMessage("Connection failed with status $status")
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            listener.onDebugMessage("MTU changed to $mtu")
            commandCompleted()
            enqueueCommand { gatt.discoverServices() }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            listener.onDebugMessage("Services discovered")
            commandCompleted()
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

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            listener.onDebugMessage("Descriptor written")
            commandCompleted()
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            listener.onDebugMessage("Write completed with status: $status")
            commandCompleted()
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            listener.onMessageReceived(value.decodeToString())
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun enableIndication() {
        val gatt = bluetoothGatt ?: return
        val characteristic = indicateCharacteristic ?: return

        enqueueCommand {
            gatt.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.getDescriptor(cccdUuid)
            val value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, value)
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = value
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun send(data: ByteArray) {
        val gatt = bluetoothGatt ?: return
        val characteristic = writeCharacteristic ?: return

        enqueueCommand {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(characteristic, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            } else {
                @Suppress("DEPRECATION")
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                characteristic.value = data
                gatt.writeCharacteristic(characteristic)
            }
            listener.onDebugMessage("Sending ${data.size} bytes")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun close() {
        bluetoothGatt?.close()
        bluetoothGatt = null
        listener.onDebugMessage("GATT connection closed.")
    }
}
