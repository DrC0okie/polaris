package ch.drcookie.polaris_app.ui.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ch.drcookie.polaris_sdk.api.Polaris.networkClient
import ch.drcookie.polaris_sdk.api.Polaris.bleController
import ch.drcookie.polaris_sdk.api.Polaris.keyStore
import ch.drcookie.polaris_sdk.api.Polaris.protocolHandler
import ch.drcookie.polaris_sdk.api.SdkResult
import ch.drcookie.polaris_sdk.api.use_case.DeliverPayload
import ch.drcookie.polaris_sdk.api.use_case.MonitorBroadcasts
import ch.drcookie.polaris_sdk.api.use_case.PolTransaction
import ch.drcookie.polaris_sdk.api.use_case.PullAndForward
import ch.drcookie.polaris_sdk.api.use_case.FetchBeacons
import ch.drcookie.polaris_sdk.api.use_case.ScanForBeacon
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

/**
 * The ViewModel for the main activity, acting as a bridge between the UI and the Polaris SDK.
 *
 * @param scanForBeacon Use case for performing a one-shot scan for a connectable beacon.
 * @param performPolTransaction Use case for executing a full Proof-of-Location transaction.
 * @param deliverSecurePayload Use case for delivering a server-originated payload to a beacon.
 * @param monitorBroadcasts Use case for listening to non-connectable beacon advertisements.
 * @param pullAndForwardData Use case for pulling data from a beacon and forwarding it to the server.
 * @param registerDevice Use case for registering a phone to the server
 */
@OptIn(ExperimentalUnsignedTypes::class)
@RequiresApi(Build.VERSION_CODES.O)
class PolarisViewModel(
    private val scanForBeacon: ScanForBeacon,
    private val performPolTransaction: PolTransaction,
    private val deliverSecurePayload: DeliverPayload,
    private val monitorBroadcasts: MonitorBroadcasts,
    private val pullAndForwardData: PullAndForward,
    private val registerDevice: FetchBeacons,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()
    private var monitoringJob: Job? = null
    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

    fun fetchBeacons() {
        runFlow("Fetch beacons") {
            if (networkClient.getPhoneId() > 0) {
                // We are already registered, just fetch beacons.
                val fetchResult = networkClient.fetchBeacons()
                when (fetchResult) {
                    is SdkResult.Success -> {
                        val beaconCount = fetchResult.value.size
                        appendLog("Fetch successful. Found $beaconCount known beacons.")
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
            if (networkClient.knownBeacons.isEmpty()) {
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
            when (val submitResult = networkClient.submitPoLToken(token)) {
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
            val payloadsResult = networkClient.getPayloadsForDelivery()
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
            when (val ackResult = networkClient.submitSecureAck(ackRequest)) {
                is SdkResult.Success -> {
                    appendLog("ACK/ERR submitted successfully.")
                }

                is SdkResult.Failure -> {
                    appendLog("--- ERROR: Failed to submit ACK: ${ackResult.error.message()} ---")
                }
            }
        }
    }

    fun pullDataFromBeacon() {
        runFlow("Pull Data from Beacon") {

            appendLog("Scanning for beacons with data pending...")

            // Scan for any beacon. We don't filter for a specific one.
            val scanResult = scanForBeacon(timeoutMillis = 10000L)

            val foundBeacon = when (scanResult) {
                is SdkResult.Success -> scanResult.value
                is SdkResult.Failure -> {
                    appendLog("--- ERROR: Scan failed: ${scanResult.error.message()} ---")
                    return@runFlow
                }
            }

            // Check if a beacon was found and if it has data.
            if (foundBeacon == null) {
                appendLog("Scan timed out. No connectable beacons found.")
                return@runFlow
            }

            if (!foundBeacon.hasDataPending) {
                appendLog("Found beacon '${foundBeacon.name}', but it has no data pending.")
                return@runFlow
            }

            // We found a suitable beacon, Execute the flow.
            appendLog("Found beacon '${foundBeacon.name}' with data. Attempting to pull and forward...")

            when (val pullResult = pullAndForwardData(foundBeacon)) {
                is SdkResult.Success -> {
                    appendLog("Successfully pulled and forwarded data from ${foundBeacon.name}.")
                }

                is SdkResult.Failure -> {
                    appendLog("--- ERROR: Failed to process data from ${foundBeacon.name}: ${pullResult.error.message()} ---")
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

class PolarisViewModelFactory() : ViewModelProvider.Factory {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PolarisViewModel::class.java)) {

            val scanForBeacon = ScanForBeacon(bleController, networkClient)
            val performPolTransaction = PolTransaction(bleController, networkClient, keyStore, protocolHandler)
            val deliverSecurePayload = DeliverPayload(bleController, networkClient, scanForBeacon)
            val monitorBroadcasts = MonitorBroadcasts(bleController, networkClient, protocolHandler)
            val pullAndForwardData = PullAndForward(bleController, networkClient)
            val registerDevice = FetchBeacons(networkClient, keyStore)


            @Suppress("UNCHECKED_CAST")
            return PolarisViewModel(
                scanForBeacon,
                performPolTransaction,
                deliverSecurePayload,
                monitorBroadcasts,
                pullAndForwardData,
                registerDevice
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
