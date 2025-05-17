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
import ch.drcookie.polaris_app.Utils.hexStringToUByteArray
import ch.drcookie.polaris_app.Utils.toHexString
import ch.drcookie.polaris_app.databinding.ActivityMainBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalUnsignedTypes::class)
class MainActivity : AppCompatActivity(), BleManager.Listener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bleManager: BleManager
    private var lastSentRequest: PoLRequest? = null // To verify response against
    private val targetBeaconEd25519Pk: UByteArray by lazy {
        hexStringToUByteArray("A3540D31912B89B101B4FA69F37ACFA49E3B1BAA0D1D04C8202BFD1B20B741D3")
    }

    private val permissions = mutableListOf<String>().apply {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            add(Manifest.permission.BLUETOOTH)
            add(Manifest.permission.BLUETOOTH_ADMIN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
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

        lifecycleScope.launch {
            Crypto.initialize()
            appendLog("Crypto Initialized: ${Crypto.isInitialized}") // Verify init
        }

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
            val hexString = data.toHexString()
            appendLog("Message Received (${data.size} bytes): $hexString")
            binding.messageBox.text = "Raw: $hexString"

            val response = PoLResponse.fromBytes(data)
            if (response != null) {
                appendLog("Parsed PoLResponse: $response")
                val originalRequest = lastSentRequest
                if (originalRequest != null) {
                    if (!Crypto.isInitialized) {
                        appendLog("ERROR: Crypto not initialized. Cannot verify response.")
                        return@runOnUiThread
                    }
                    val isValid = Crypto.verifyPoLResponse(response, originalRequest, targetBeaconEd25519Pk)
                    appendLog("Response signature valid: $isValid")
                    if (isValid) {
                        binding.messageBox.text = "Beacon Counter: ${response.counter}, Sig Valid: $isValid"
                    } else {
                        binding.messageBox.text = "INVALID SIG! Counter: ${response.counter}"
                    }
                } else {
                    appendLog("Error: No original request found to verify response against.")
                }
            } else {
                appendLog("Failed to parse PoLResponse.")
            }
        }
    }

    override fun onDeviceNotFound() {
        runOnUiThread {
            appendLog("Scan finished: No suitable device found.")
        }
    }

    override fun onConnectionFailed(status: Int) {
        runOnUiThread {
            val statusText = when (status) {
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
            appendLog("Indications enabled. Preparing and sending PoLRequest...")
            if (!Crypto.isInitialized) {
                appendLog("ERROR: Crypto not initialized. Cannot send request.")
                bleManager.close()
                return@runOnUiThread
            }
            try {
                val (phonePk, phoneSk) = Crypto.getOrGeneratePhoneKeyPair()
                val requestNonce = Crypto.generateNonce()

                val targetBeaconId = 0x00000001u

                var request = PoLRequest(
                    flags = 0x00u,
                    phoneId = 1234567890UL,
                    beaconId = targetBeaconId,
                    nonce = requestNonce,
                    phonePk = phonePk
                )
                request = Crypto.signPoLRequest(request, phoneSk)
                lastSentRequest = request

                val dataToSend = request.toBytes()
                appendLog("Sending PoLRequest (${dataToSend.size} bytes): ${dataToSend.toHexString()}")
                bleManager.send(dataToSend)

            } catch (e: Exception) {
                appendLog("Error preparing/sending PoLRequest: ${e.message}")
                e.printStackTrace()
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
