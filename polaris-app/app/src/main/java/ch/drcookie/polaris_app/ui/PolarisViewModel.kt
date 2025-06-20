package ch.drcookie.polaris_app.ui

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ch.drcookie.polaris_app.PolarisApplication
import androidx.lifecycle.viewModelScope
import ch.drcookie.polaris_app.data.ble.BleDataSource
import ch.drcookie.polaris_app.data.local.UserPreferences
import ch.drcookie.polaris_app.data.model.FoundBeacon
import ch.drcookie.polaris_app.data.model.dto.*
import ch.drcookie.polaris_app.data.remote.RemoteDataSource
import ch.drcookie.polaris_app.repository.AuthRepository
import ch.drcookie.polaris_app.repository.BeaconInteractor
import ch.drcookie.polaris_app.repository.BeaconScanner
import ch.drcookie.polaris_app.util.Crypto
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class UiState(
    val log: String = "",
    val isBusy: Boolean = false,
    val connectionStatus: String = "Disconnected",
    val canStart: Boolean = true
)

@OptIn(ExperimentalUnsignedTypes::class)
class PolarisViewModel(
    private val repository: AuthRepository,
    private val scanner: BeaconScanner,
    private val interactorFactory: (FoundBeacon) -> BeaconInteractor
) :
    ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.O)
    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

    @RequiresApi(Build.VERSION_CODES.O)
    fun register() {
        runFlow("Registration") {
            appendLog("Generating key pair...")
            val (pk, _) = Crypto.getOrGeneratePhoneKeyPair()
            val req = PhoneRegistrationRequestDto(
                publicKey = pk,
                deviceModel = Build.MODEL,
                osVersion = Build.VERSION.RELEASE,
                appVersion = "1.0"
            )
            appendLog("Registering phone with server...")
            val beacons = repository.registerPhone(req)
            appendLog("Registration successful. Found ${beacons.size} known beacons.")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun findAndExecuteTokenFlow() {
        runFlow("PoL Token Flow") {
            if (repository.knownBeacons.isEmpty()) {
                appendLog("No known beacons. Please register first.")
                return@runFlow
            }

            // Scan for a beacon
            appendLog("Scanning for first known beacon...")
            val foundBeacon = withTimeoutOrNull(10000) {
                scanner.findKnownBeacons(repository.knownBeacons).first()
            }

            if (foundBeacon == null) {
                appendLog("Scan timed out. No known beacons found.")
                return@runFlow
            }
            appendLog("Found beacon: ${foundBeacon.name}")

            // Create an interactor and perform the transaction
            val interactor = interactorFactory(foundBeacon)
            try {
                appendLog("Connecting to ${foundBeacon.address}...")
                interactor.connect()
                appendLog("Connection successful. Performing PoL transaction...")
                val token = interactor.performPoLTransaction()
                appendLog("PoL transaction successful. Submitting token...")
                repository.submitPoLToken(token)
                appendLog("Token submitted successfully!")
            } finally {
                appendLog("Disconnecting...")
                interactor.disconnect()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun processPayloadFlow() {
        runFlow("Secure Payload Delivery") {
            // Fetch pending payloads from server
            appendLog("Checking for pending payloads...")
            val payloads = repository.getPayloadsForDelivery()
            if (payloads.isEmpty()) {
                appendLog("No pending payloads.")
                return@runFlow
            }
            val job = payloads.first()
            appendLog("Found payload #${job.deliveryId} for beacon #${job.beaconId}.")

            // Scan for the specific beacon needed for this job
            val targetBeaconInfo = repository.knownBeacons.find { it.beaconId == job.beaconId }
            if (targetBeaconInfo == null) {
                appendLog("Error: Beacon #${job.beaconId} is not in our known list.")
                return@runFlow
            }

            appendLog("Scanning for beacon ${targetBeaconInfo.name} (15s timeout)...")
            val foundBeacon = withTimeoutOrNull(10000) {
                scanner.findKnownBeacons(listOf(targetBeaconInfo)).first()
            }

            if (foundBeacon == null) {
                appendLog("Target beacon not found.")
                return@runFlow
            }
            appendLog("Found target beacon: ${foundBeacon.name}")

            // Create interactor and deliver the payload
            val interactor = interactorFactory(foundBeacon)
            try {
                appendLog("Connecting...")
                interactor.connect()
                appendLog("Connection successful. Delivering payload...")
                val ackBlob = interactor.deliverSecurePayload(job.encryptedBlob.asByteArray())
                appendLog("Payload delivered, received ACK/ERR blob (${ackBlob.size} bytes). Submitting to server...")
                val ackRequest = AckRequestDto(job.deliveryId, ackBlob.toUByteArray())
                repository.submitSecureAck(ackRequest)
                appendLog("ACK/ERR submitted successfully.")
            } finally {
                appendLog("Disconnecting...")
                interactor.disconnect()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun runFlow(flowName: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, canStart = false) }
            appendLog("--- Starting Flow: $flowName ---")
            try {
                block()
            } catch (e: Exception) {
                appendLog("--- ERROR in $flowName: ${e.message} ---")
                e.printStackTrace()
            } finally {
                _uiState.update { it.copy(isBusy = false, canStart = true) }
                appendLog("--- Finished Flow: $flowName ---")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun appendLog(message: String) {
        val timestamp = formatter.format(Instant.now())
        _uiState.update {
            val newLog = if (it.log.isEmpty()) "$timestamp: $message" else "${it.log}\n$timestamp: $message"
            it.copy(log = newLog)
        }
    }
}


// Create a ViewModel Factory for dependency injection
class PolarisViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PolarisViewModel::class.java)) {
            val polarisApplication = application as PolarisApplication

            val bleDataSource = polarisApplication.bleDataSource
            val remoteDataSource = polarisApplication.remoteDataSource
            val userPrefs = UserPreferences(application.applicationContext)
            val authRepository = AuthRepository(remoteDataSource, userPrefs)
            val beaconScanner = BeaconScanner(bleDataSource)

            val interactorFactory = { foundBeacon: FoundBeacon ->
                BeaconInteractor(foundBeacon, bleDataSource, userPrefs)
            }

            @Suppress("UNCHECKED_CAST")
            return PolarisViewModel(authRepository, beaconScanner, interactorFactory) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}