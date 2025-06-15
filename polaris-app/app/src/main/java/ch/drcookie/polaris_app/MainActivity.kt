package ch.drcookie.polaris_app

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanResult
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import ch.drcookie.polaris_app.Utils.toHexString
import ch.drcookie.polaris_app.databinding.ActivityMainBinding
import androidx.lifecycle.lifecycleScope
import ch.drcookie.polaris_app.dto.PhoneRegistrationRequestDto
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import androidx.core.content.edit
import ch.drcookie.polaris_app.dto.BeaconProvisioningDto

@OptIn(ExperimentalUnsignedTypes::class)
class MainActivity : AppCompatActivity(), BleListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bleManager: BleManager
    private val prefs: SharedPreferences by lazy {
        getSharedPreferences("polaris_prefs", MODE_PRIVATE)
    }
    private var lastSentRequest: PoLRequest? = null
    private var lastPolToken: PoLToken? = null
    private var knownBeacons: List<BeaconProvisioningDto> = emptyList()
    private var targetBeacon: BeaconProvisioningDto? = null
//    private val targetBeaconEd25519Pk: UByteArray by lazy { // FIXME: This will be fetched from the server
//        hexStringToUByteArray("A3540D31912B89B101B4FA69F37ACFA49E3B1BAA0D1D04C8202BFD1B20B741D3")
//    }

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
        binding.registerButton.setOnClickListener {
            lifecycleScope.launch {
                registerPhone()
            }
        }
        binding.fetchBeaconsButton.setOnClickListener {
            lifecycleScope.launch {
                fetchBeacons()
            }
        }
        binding.sendTokenButton.setOnClickListener {
            lifecycleScope.launch {
                sendToken()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBleFlow() {
        if (!isBluetoothEnabled()) {
            appendLog("Bluetooth is disabled.")
            return
        }

        if (bleManager.isConnected()) {
            appendLog("Already connected.")
            return
        }

        if (knownBeacons.isEmpty()) {
            appendLog("No known beacons. Please fetch or register first.")
            return
        }
        targetBeacon = null
        bleManager.startScan()
    }

    private suspend fun registerPhone() {
        try {
            val (pk, _) = Crypto.getOrGeneratePhoneKeyPair()
            val req = PhoneRegistrationRequestDto(
                publicKey = pk,
                deviceModel = Build.MODEL,
                osVersion = Build.VERSION.RELEASE,
                appVersion = "1.0"
            )
            val resp = ApiService.registerPhone(req)
            prefs.edit {
                putString("API_KEY", resp.apiKey)
                putLong("PHONE_ID", resp.assignedPhoneId ?: -1L)
            }
            knownBeacons = resp.beacons.beacons
        } catch (e: Exception) {
            appendLog("Register failed: ${e.message}")
        }
    }

    private suspend fun fetchBeacons() {
        val key = prefs.getString("API_KEY", null)
        if (key == null) {
            appendLog("No API key; register first.")
            return
        }
        try {
            val beaconListDto = ApiService.fetchBeacons(key)
            knownBeacons = beaconListDto.beacons
            appendLog(
                "Beacons fetched (${knownBeacons.size}):\n" +
                        knownBeacons.joinToString("\n") { "• ID: ${it.beaconId}, Name: ${it.name}, PK: ${it.publicKey.toHexString()}" })
            if (knownBeacons.isEmpty()) {
                appendLog("No beacons provisioned for this phone.")
            }
        } catch (e: Exception) {
            appendLog("Fetch failed: ${e.message}")
        }
    }

    private suspend fun sendToken() {
        val key = prefs.getString("API_KEY", null)
        val token = lastPolToken
        if (key == null || token == null) {
            appendLog("Need both API key and a generated token.")
            return
        }
        try {
            val ok = ApiService.sendPoLToken(token, key)
            appendLog("Token send ${if (ok) "succeeded" else "failed"}")
        } catch (e: Exception) {
            appendLog("Send token error: ${e.message}")
        }
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
                if (originalRequest != null && targetBeacon != null) {
                    if (!Crypto.isInitialized) {
                        appendLog("ERROR: Crypto not initialized. Cannot verify response.")
                        return@runOnUiThread
                    }
                    val isValid = Crypto.verifyPoLResponse(response, originalRequest, targetBeacon!!.publicKey)
                    appendLog("Response signature valid: $isValid")
                    if (isValid) {
                        binding.messageBox.text = "Beacon Counter: ${response.counter}, Sig Valid: $isValid"
                        // Create and send the PoLToken
                        lastPolToken = PoLToken.create(originalRequest, response)
                        val jsonFormat = Json { prettyPrint = true }
                        val polTokenJsonString = jsonFormat.encodeToString(PoLToken.serializer(), lastPolToken!!)
                        Log.i("MainActivity", "Created PoLToken: $polTokenJsonString")
                        lifecycleScope.launch { // Network call on a background thread
                            sendToken()
                        }
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
                val phoneId = prefs.getLong("PHONE_ID", 0)
                if (phoneId == 0L) {
                    appendLog("Error Phone ID not fetched from the server. Call /register route")
                    return@runOnUiThread
                }

                var request = PoLRequest(
                    flags = 0x00u,
                    phoneId = phoneId.toULong(),
                    beaconId = targetBeacon?.beaconId?:0.toUInt(),
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

    @SuppressLint("MissingPermission") // For bleManager.connectToDevice and stopScan
    override fun onBeaconAdvertised(scanResult: ScanResult, detectedBeaconId: UInt?) {
        runOnUiThread {
            val device = scanResult.device
            val deviceName = try {
                device.name ?: "Unnamed"
            } catch (e: SecurityException) {
                "N/A (Permission?)"
            }
            val deviceAddress = device.address

            if (detectedBeaconId != null) {
                appendLog("Candidate Beacon: Name='$deviceName', Addr=$deviceAddress, Adv ID=$detectedBeaconId")

                // Only proceed if we aren't already trying to connect to one from this scan session
                if (targetBeacon != null) {
                    appendLog("Already targeting beacon ${targetBeacon?.beaconId}, ignoring further scan results for now.")
                    return@runOnUiThread
                }
                val matchedKnownBeacon = knownBeacons.find { it.beaconId == detectedBeaconId }
                if (matchedKnownBeacon != null) {
                    appendLog("Found KNOWN beacon: ID=${matchedKnownBeacon.beaconId}, Name='${matchedKnownBeacon.name}'. Attempting to connect.")
                    targetBeacon = matchedKnownBeacon
                    bleManager.stopScan() // Stop scanning now that we've found one we want
                    bleManager.connectToDevice(device)
                } else {
                    appendLog("Beacon ID $detectedBeaconId is not in our known list.")
                }
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
