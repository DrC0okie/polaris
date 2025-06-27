package ch.drcookie.polaris_app.ui.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.drcookie.polaris_sdk.api.Polaris.apiClient
import ch.drcookie.polaris_sdk.api.Polaris.bleController
import ch.drcookie.polaris_sdk.api.Polaris.keyStore
import ch.drcookie.polaris_sdk.api.Polaris.protocolHandler
import ch.drcookie.polaris_sdk.api.SdkResult
import ch.drcookie.polaris_sdk.api.flows.DeliverPayloadFlow
import ch.drcookie.polaris_sdk.api.flows.MonitorBroadcastsFlow
import ch.drcookie.polaris_sdk.api.flows.PolTransactionFlow
import ch.drcookie.polaris_sdk.api.flows.RegisterDeviceFlow
import ch.drcookie.polaris_sdk.api.flows.ScanForBeaconFlow
import ch.drcookie.polaris_sdk.api.message
import ch.drcookie.polaris_sdk.ble.model.DeliveryAck
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.collections.toUByteArray

data class UiState(
    val log: String = "",
    val isBusy: Boolean = false,
    val canStart: Boolean = true,
    val isMonitoring: Boolean = false,
)

@OptIn(ExperimentalUnsignedTypes::class)
@RequiresApi(Build.VERSION_CODES.O)
class PolarisViewModel() : ViewModel() {

    private val api = apiClient
    private val registerDevice = RegisterDeviceFlow(api, keyStore)
    private val scanForBeacon = ScanForBeaconFlow(bleController, api)
    private val performPolTransaction = PolTransactionFlow(bleController, api, keyStore, protocolHandler)
    private val deliverSecurePayload = DeliverPayloadFlow(bleController, api, scanForBeacon)
    private val monitorBroadcasts = MonitorBroadcastsFlow(bleController, api, protocolHandler)

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()
    private var monitoringJob: Job? = null
    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

    fun fetchBeacons() {
        runFlow("Fetch beacons") {
            if (apiClient.getPhoneId() > 0) {
                // We are already registered, just fetch beacons.
                val fetchResult = apiClient.fetchBeacons()
                when (fetchResult) {
                    is SdkResult.Success -> {
                        val beaconCount = fetchResult.value.size
                        appendLog("Registration successful. Found $beaconCount known beacons.")
                    }

                    is SdkResult.Failure -> {
                        appendLog("--- ERROR: Registration failed: ${fetchResult.error.message()} ---")
                    }
                }
            } else {
                appendLog("Registering phone with server...")
                val result = registerDevice(Build.MODEL, Build.VERSION.RELEASE, "1.0")
                when (result) {
                    is SdkResult.Success -> {
                        val beaconCount = result.value
                        appendLog("Registration successful. Found $beaconCount known beacons.")
                    }

                    is SdkResult.Failure -> {
                        appendLog("--- ERROR: Registration failed: ${result.error.message()} ---")
                    }
                }
            }
        }
    }

    fun findAndExecuteTokenFlow() {
        runFlow("PoL Token Flow") {
            if (api.knownBeacons.isEmpty()) {
                appendLog("No known beacons. Please register first.")
                return@runFlow
            }

            appendLog("Scanning for first known beacon...")

            val scanResult = scanForBeacon()

            // Check the scan result
            val foundBeacon = when (scanResult) {
                is SdkResult.Success -> scanResult.value
                is SdkResult.Failure -> {
                    appendLog("--- ERROR: Scan failed: ${scanResult.error.message()} ---")
                    return@runFlow
                }
            }

            // Check if a beacon was actually found (vs. timeout)
            if (foundBeacon == null) {
                appendLog("Scan timed out. No known beacons found.")
                return@runFlow
            }
            appendLog("Found beacon: ${foundBeacon.name}. Performing transaction...")

            // PoL transaction
            val transactionResult = performPolTransaction(foundBeacon)
            val token = when (transactionResult) {
                is SdkResult.Success -> transactionResult.value
                is SdkResult.Failure -> {
                    appendLog("--- ERROR: PoL Transaction failed: ${transactionResult.error.message()} ---")
                    return@runFlow
                }
            }

            appendLog("PoL transaction successful. Submitting token...")

            // Submit the token to the server
            when (val submitResult = api.submitPoLToken(token)) {
                is SdkResult.Success -> {
                    appendLog("Token submitted successfully!")
                }

                is SdkResult.Failure -> {
                    appendLog("--- ERROR: Failed to submit token: ${submitResult.error.message()} ---")
                }
            }
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
            .onEach { result ->
                when (result) {
                    is SdkResult.Success -> {
                        val verifiedBroadcast = result.value
                        val payload = verifiedBroadcast.payload
                        val verificationStatus = if (verifiedBroadcast.isSignatureValid) "VALID" else "INVALID SIG"
                        appendLog("Broadcast from #${payload.beaconId}: Counter=${payload.counter} [${verificationStatus}]")
                    }

                    is SdkResult.Failure -> {
                        appendLog("--- ERROR: Monitoring stopped. Reason: ${result.error.message()} ---")

                        // Cancel the job to ensure the onCompletion block is called.
                        monitoringJob?.cancel()
                    }
                }
            }
            .onCompletion {
                _uiState.update { it.copy(isMonitoring = false) }
                appendLog("--- Finished Flow: Broadcast Monitoring ---")
            }
            .launchIn(viewModelScope)
    }

    fun processPayloadFlow() {
        runFlow("Secure Payload Delivery") {
            appendLog("Checking for pending payloads...")

            // Get payloads
            val payloadsResult = api.getPayloadsForDelivery()
            val payloads = when (payloadsResult) {
                is SdkResult.Success -> payloadsResult.value
                is SdkResult.Failure -> {
                    appendLog("--- ERROR: Could not get payloads: ${payloadsResult.error.message()} ---")
                    return@runFlow
                }
            }

            if (payloads.isEmpty()) {
                appendLog("No pending payloads.")
                return@runFlow
            }

            val job = payloads.first()
            appendLog("Found payload #${job.deliveryId} for beacon #${job.beaconId}.")

            appendLog("Attempting to deliver...")

            // Deliver the payload to the beacon
            val deliveryResult = deliverSecurePayload(job)
            val ackBlob = when (deliveryResult) {
                is SdkResult.Success -> deliveryResult.value
                is SdkResult.Failure -> {
                    appendLog("--- ERROR: Payload delivery failed: ${deliveryResult.error.message()} ---")
                    return@runFlow
                }
            }

            appendLog("Payload delivered, received ACK/ERR blob (${ackBlob.size} bytes). Submitting to server...")
            val ackRequest = DeliveryAck(job.deliveryId, ackBlob.toUByteArray())

            // Submit the acknowledgement
            when (val ackResult = api.submitSecureAck(ackRequest)) {
                is SdkResult.Success -> {
                    appendLog("ACK/ERR submitted successfully.")
                }

                is SdkResult.Failure -> {
                    appendLog("--- ERROR: Failed to submit ACK: ${ackResult.error.message()} ---")
                }
            }
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
