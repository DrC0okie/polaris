package ch.drcookie.polaris_app

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import ch.drcookie.polaris_app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), BleManager.Listener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bleManager: BleManager

    private val permissions = mutableListOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    ).apply {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            add(Manifest.permission.BLUETOOTH)
            add(Manifest.permission.BLUETOOTH_ADMIN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }.toTypedArray()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            if (granted.values.all { it }) {
                startBleFlow()
            } else {
                appendLog("Permissions denied.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bleManager = BleManager(this, this)

        binding.startButton.setOnClickListener {
            if (hasPermissions()) {
                startBleFlow()
            } else {
                permissionLauncher.launch(permissions)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBleFlow() {
        if (!isBluetoothEnabled()) {
            appendLog("Bluetooth is disabled.")
            return
        }

        if (bleManager.isConnected() || bleManager.isReady()) {
            appendLog("Already connected or ready.")
            return
        }

        bleManager.startScan()
    }

    private fun isBluetoothEnabled(): Boolean {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter?.isEnabled == true
    }

    private fun hasPermissions(): Boolean {
        return permissions.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onDebugMessage(message: String) {
        runOnUiThread {
            appendLog(message)
        }
    }

    override fun onMessageReceived(data: ByteArray) {
        runOnUiThread {
            val hexString = data.joinToString(separator = " ") { "%02X".format(it) }
            val messageText = "Message from beacon (${data.size} bytes): $hexString"
            binding.messageBox.text = messageText
            appendLog("Message Received: $hexString")
        }
    }

    override fun onDeviceNotFound() {
        runOnUiThread {
            appendLog("Scan finished: No suitable device found.")
        }
    }

    override fun onConnectionFailed(status: Int) {
        runOnUiThread {
            val statusText = when(status) {
                BluetoothGatt.GATT_SUCCESS -> "Disconnected normally"
                8 -> "Connection failed: Insufficient Encryption (Bonding required?)"
                15 -> "Connection failed: Insufficient Authentication (Bonding required?)"
                133 -> "Connection failed: GATT Error 133 (Timeout, resource issue, device busy?)"
                257 -> "Connection failed: GATT Internal Error (Stack issue?)"
                else -> "Connection failed or lost: Status $status"
            }
            appendLog(statusText)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onReady() {
        runOnUiThread {
            appendLog("Device ready. Enabling indications...")
            bleManager.enableIndication()
            appendLog("Sent ${Payload.data.size} bytes.")
        }
    }

    @SuppressLint("MissingPermission")
    override fun onIndicationEnabled() {
        runOnUiThread {
            appendLog("Indications enabled. Sending message...")
            try {
                val dataToSend = Payload.data
                bleManager.send(dataToSend)
            } catch (e: Exception) {
                appendLog("Error preparing data to send: ${e.message}")
                bleManager.close()
            }
        }
    }

    private fun appendLog(text: String) {
        val current = binding.debugLog.text.toString()
        binding.debugLog.text = "$current\n$text".trim()
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        appendLog("Activity destroying. Closing BLE connection...")
        bleManager.close()
        super.onDestroy()
    }
}
