package ch.drcookie.polaris_app.ui.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.drcookie.polaris_app.domain.model.dto.AckRequestDto
import ch.drcookie.polaris_app.domain.interactor.*
import ch.drcookie.polaris_app.domain.repository.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.collections.toUByteArray

private val Log = KotlinLogging.logger {}

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
    private val registerDevice: RegisterDeviceInteractor,
    private val scanForBeacon: ScanConnectableBeaconInteractor,
    private val performPolTransaction: PolTransactionInteractor,
    private val deliverSecurePayload: DeliverPayloadInteractor,
    private val monitorBroadcasts: MonitorBroadcastsInteractor
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()
    private var monitoringJob: Job? = null
    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

    fun register() {
        runFlow("Registration") {
            appendLog("Registering phone with server...")
            // The ViewModel doesn't know how registration works. It just calls the interactor.
            val beaconCount = registerDevice(Build.MODEL, Build.VERSION.RELEASE, "1.0")
            appendLog("Registration successful. Found $beaconCount known beacons.")
        }
    }

    fun findAndExecuteTokenFlow() {
        runFlow("PoL Token Flow") {
            if (authRepository.knownBeacons.isEmpty()) {
                appendLog("No known beacons. Please register first.")
                return@runFlow
            }

            appendLog("Scanning for first known beacon...")

            val foundBeacon = scanForBeacon()

            if (foundBeacon == null) {
                appendLog("Scan timed out. No known beacons found.")
                return@runFlow
            }
            appendLog("Found beacon: ${foundBeacon.name}. Performing transaction...")

            val token = performPolTransaction(foundBeacon)

            appendLog("PoL transaction successful. Submitting token...")
            authRepository.submitPoLToken(token)
            appendLog("Token submitted successfully!")
        }
    }

    fun toggleBroadcastMonitoring() {
        if (monitoringJob?.isActive == true) {
            monitoringJob?.cancel() // This will cancel the collection of the flow
            monitoringJob = null
            _uiState.update { it.copy(isMonitoring = false) }
            appendLog("Broadcast monitoring stopped.")
            return
        }

        appendLog("Starting broadcast monitoring...")
        _uiState.update { it.copy(isMonitoring = true) }

        monitoringJob = monitorBroadcasts.startMonitoring()
            .onEach { verifiedBroadcast ->
                val payload = verifiedBroadcast.payload
                val verificationStatus = if (verifiedBroadcast.isSignatureValid) "VALID" else "INVALID SIG"
                appendLog("Broadcast from #${payload.beaconId}: Counter=${payload.counter} [${verificationStatus}]")
            }
            .onCompletion {
                // This will be called on cancellation or if the flow naturally ends
                Log.info{"Broadcast monitoring flow completed."}
                _uiState.update { it.copy(isMonitoring = false) }
            }
            .catch { e ->
                // Handle any unexpected errors from the flow itself
                appendLog("ERROR during monitoring: ${e.message}")
            }
            .launchIn(viewModelScope) // Use launchIn for a concise launch
    }

    fun processPayloadFlow() {
        runFlow("Secure Payload Delivery") {
            appendLog("Checking for pending payloads...")
            val payloads = authRepository.getPayloadsForDelivery()
            if (payloads.isEmpty()) {
                appendLog("No pending payloads.")
                return@runFlow
            }
            val job = payloads.first()
            appendLog("Found payload #${job.deliveryId} for beacon #${job.beaconId}.")
            appendLog("Attempting to deliver...")

            val ackBlob = deliverSecurePayload(job)

            if (ackBlob == null) {
                appendLog("Failed to deliver payload: Target beacon not found.")
                return@runFlow
            }

            appendLog("Payload delivered, received ACK/ERR blob (${ackBlob.size} bytes). Submitting to server...")
            val ackRequest = AckRequestDto(job.deliveryId, ackBlob.toUByteArray())
            authRepository.submitSecureAck(ackRequest)
            appendLog("ACK/ERR submitted successfully.")
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
