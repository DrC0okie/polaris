package ch.drcookie.polaris_app

import android.Manifest
import android.annotation.SuppressLint
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

        appendLog("Starting BLE scan...")
        bleManager.start()
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

    override fun onMessageReceived(message: String) {
        runOnUiThread {
            binding.messageBox.text = "Message from beacon: $message"
        }
    }

    override fun onDeviceNotFound() {
        runOnUiThread {
            appendLog("No device found.")
        }
    }

    @SuppressLint("MissingPermission")
    override fun onReady() {
        runOnUiThread {
            appendLog("Device ready. Sending message...")
            bleManager.enableIndication()
            bleManager.send(Payload.data)
        }
    }

    private fun appendLog(text: String) {
        val current = binding.debugLog.text.toString()
        binding.debugLog.text = "$current\n$text".trim()
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        bleManager.close()
        super.onDestroy()
    }
}
