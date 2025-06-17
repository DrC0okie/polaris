package ch.drcookie.polaris_app.ui

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ch.drcookie.polaris_app.data.ble.BleDataSource
import ch.drcookie.polaris_app.data.ble.ConnectionState
import ch.drcookie.polaris_app.data.local.UserPreferences
import ch.drcookie.polaris_app.data.model.dto.*
import ch.drcookie.polaris_app.data.remote.RemoteDataSource
import ch.drcookie.polaris_app.repository.PolarisRepository
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
class PolarisViewModel(private val repository: PolarisRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.O)
    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

    init {
        // Observe connection state from the repository
        viewModelScope.launch {
            repository.connectionState.collect { state ->
                val statusText = when(state) {
                    is ConnectionState.Connecting -> "Connecting to ${state.deviceAddress}"
                    is ConnectionState.Ready -> "Ready (${state.deviceAddress})"
                    is ConnectionState.Disconnected -> "Disconnected"
                    is ConnectionState.Failed -> "Failed: ${state.error}"
                    is ConnectionState.Scanning -> "Scanning..."
                }
                _uiState.update { it.copy(connectionStatus = statusText) }
            }
        }
    }

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
            val beacons = repository.registerPhoneAndFetchBeacons(req)
            appendLog("Registration successful. API key stored.")
            appendLog("Received ${beacons.size} provisioned beacons.")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun findAndRequestToken() {
        runFlow("Token Request") {
            if (repository.knownBeacons.isEmpty()) {
                appendLog("No known beacons. Please register first.")
                return@runFlow
            }

            appendLog("Scanning for first known beacon...")
            val scanResultPair = withTimeoutOrNull(10000) { // 10-second timeout
                repository.findFirstKnownBeacon().first()
            }

            // Check if the timeout occurred
            if (scanResultPair == null) {
                appendLog("Scan timed out. No known beacons found in the vicinity.")
                repository.disconnect() // Ensure BLE is stopped if it was still scanning
                return@runFlow
            }

            val (scanResult, beacon) = scanResultPair
            appendLog("Found beacon: ${beacon.name} (${scanResult.device.address}). Connecting...")

            val token = repository.connectAndRequestToken(scanResult, beacon)
            appendLog("Successfully created PoLToken for beacon ${token.beaconId}.")

            appendLog("Submitting token to server...")
            repository.submitToken(token)
            appendLog("Flow complete! Token submitted successfully.")
        }
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
                repository.disconnect() // Ensure we are disconnected on error
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

    override fun onCleared() {
        super.onCleared()
        repository.shutdown()
        Log.d("PolarisViewModel", "ViewModel cleared, repository shut down.")
    }

}


// Create a ViewModel Factory for dependency injection
class PolarisViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PolarisViewModel::class.java)) {
            val bleDataSource = BleDataSource(context)
            val remoteDataSource = RemoteDataSource()
            val userPrefs = UserPreferences(context)
            val repository = PolarisRepository(bleDataSource, remoteDataSource, userPrefs)
            @Suppress("UNCHECKED_CAST")
            return PolarisViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}