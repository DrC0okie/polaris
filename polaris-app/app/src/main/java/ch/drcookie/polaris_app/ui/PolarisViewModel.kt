package ch.drcookie.polaris_app.ui

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ch.drcookie.polaris_app.PolarisApplication
import androidx.lifecycle.viewModelScope
import ch.drcookie.polaris_app.data.local.UserPreferences
import ch.drcookie.polaris_app.data.model.FoundBeacon
import ch.drcookie.polaris_app.data.model.dto.*
import ch.drcookie.polaris_app.repository.AuthRepository
import ch.drcookie.polaris_app.repository.BeaconInteractor
import ch.drcookie.polaris_app.repository.BeaconScanner
import ch.drcookie.polaris_app.repository.ScanCallbackType
import ch.drcookie.polaris_app.repository.ScanConfig
import ch.drcookie.polaris_app.repository.ScanMode
import ch.drcookie.polaris_app.util.Crypto
import ch.drcookie.polaris_app.util.PoLConstants
import ch.drcookie.polaris_app.util.SignatureVerifier
import ch.drcookie.polaris_app.util.ScanParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.collections.toUByteArray

data class UiState(
    val log: String = "",
    val isBusy: Boolean = false,
    val connectionStatus: String = "Disconnected",
    val canStart: Boolean = true,
    val isMonitoring: Boolean = false
)

@OptIn(ExperimentalUnsignedTypes::class)
@RequiresApi(Build.VERSION_CODES.O)
class PolarisViewModel(
    private val authRepository: AuthRepository,
    private val scanner: BeaconScanner,
    private val interactorFactory: (FoundBeacon) -> BeaconInteractor,
    private val dataParser: ScanParser,
    private val signatureVerifier: SignatureVerifier
) :
    ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()
    private var monitoringJob: Job? = null

    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

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
            val beacons = authRepository.registerPhone(req)
            appendLog("Registration successful. Found ${beacons.size} known beacons.")
        }
    }

    fun findAndExecuteTokenFlow() {
        runFlow("PoL Token Flow") {
            if (authRepository.knownBeacons.isEmpty()) {
                appendLog("No known beacons. Please register first.")
                return@runFlow
            }

            // Scan for a beacon
            appendLog("Scanning for first known beacon...")
            // Configure the scan
            val scanConfig = ScanConfig(
                filterByServiceUuid = PoLConstants.POL_SERVICE_UUID,
                callbackType = ScanCallbackType.FIRST_MATCH
            )
            
            val foundBeacon = withTimeoutOrNull(10000) {
                findFirstKnownConnectableBeacon(scanConfig).first()
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
                authRepository.submitPoLToken(token)
                appendLog("Token submitted successfully!")
            } finally {
                appendLog("Disconnecting...")
                interactor.disconnect()
            }
        }
    }

    fun toggleBroadcastMonitoring() {
        if (monitoringJob?.isActive == true) {
            monitoringJob?.cancel()
            monitoringJob = null
            _uiState.update { it.copy(isMonitoring = false) }
            appendLog("Broadcast monitoring stopped.")
            return
        }

        viewModelScope.launch {
            if (authRepository.knownBeacons.isEmpty()) {
                appendLog("Cannot monitor: No known beacons. Please register first.")
                return@launch
            }

            appendLog("Starting broadcast monitoring...")
            // Update the UI state to reflect that monitoring has started
            _uiState.update { it.copy(isMonitoring = true, canStart = false) }

            val scanConfig = ScanConfig(
                scanMode = ScanMode.LOW_LATENCY,
                callbackType = ScanCallbackType.ALL_MATCHES,
                scanLegacyOnly = false,
                useAllSupportedPhys = true
            )

            // Launch the collection in its own job and store it
            monitoringJob = viewModelScope.launch {
                try {
                    scanner.startBroadcastScan(scanConfig)
                        .mapNotNull { dataParser.parseBroadcastPayload(it) }
                        .distinctUntilChanged()
                        .collect { payload ->
                            val pk = authRepository.knownBeacons.find { it.beaconId == payload.beaconId }?.publicKey
                            val isVerified = if (pk != null) signatureVerifier.verifyBroadcast(payload, pk) else false
                            val verificationStatus = if (isVerified) "VALID" else "INVALID SIG"
                            appendLog("Broadcast from #${payload.beaconId}: Counter=${payload.counter} [${verificationStatus}]")
                        }
                } catch (e: CancellationException) {
                    Log.i("PolarisViewModel", "Broadcast monitoring job was cancelled successfully.")
                } catch (e: Exception) {
                    appendLog("ERROR during monitoring: ${e.message}")
                    _uiState.update { it.copy(isMonitoring = false, canStart = true) }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun processPayloadFlow() {
        runFlow("Secure Payload Delivery") {
            // Fetch pending payloads from server
            appendLog("Checking for pending payloads...")
            val payloads = authRepository.getPayloadsForDelivery()
            if (payloads.isEmpty()) {
                appendLog("No pending payloads.")
                return@runFlow
            }
            val job = payloads.first()
            appendLog("Found payload #${job.deliveryId} for beacon #${job.beaconId}.")

            // Scan for the specific beacon needed for this job
            val targetBeaconInfo = authRepository.knownBeacons.find { it.beaconId == job.beaconId }
            if (targetBeaconInfo == null) {
                appendLog("Error: Beacon #${job.beaconId} is not in our known list.")
                return@runFlow
            }

            appendLog("Scanning for beacon ${targetBeaconInfo.name}...")
            val scanConfig = ScanConfig(
                filterByServiceUuid = PoLConstants.POL_SERVICE_UUID,
                callbackType = ScanCallbackType.FIRST_MATCH
            )
            val foundBeacon = withTimeoutOrNull(10000) {
                findFirstKnownConnectableBeacon(scanConfig, listOf(targetBeaconInfo)).first()
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
                authRepository.submitSecureAck(ackRequest)
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

    /**
     * Creates a flow that scans for beacons using the given config and filters the results
     * to find beacons that are in the known list.
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    private fun findFirstKnownConnectableBeacon(
        scanConfig: ScanConfig,
        beaconsToFind: List<BeaconProvisioningDto> = authRepository.knownBeacons
    ): Flow<FoundBeacon> {
        return scanner.startConnectableScan(scanConfig)
            .mapNotNull { scanResult ->
                // Use the stateless parser to get the ID from the legacy advertisement
                val beaconId = dataParser.parseConnectableBeaconId(scanResult)
                if (beaconId != null) {
                    // Check if the parsed ID is in the list we're looking for
                    val matchedInfo = beaconsToFind.find { it.beaconId == beaconId }
                    if (matchedInfo != null) {
                        return@mapNotNull FoundBeacon(matchedInfo, scanResult)
                    }
                }
                null
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

    @RequiresApi(Build.VERSION_CODES.O)
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
            return PolarisViewModel(authRepository, beaconScanner, interactorFactory, ScanParser, SignatureVerifier) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}