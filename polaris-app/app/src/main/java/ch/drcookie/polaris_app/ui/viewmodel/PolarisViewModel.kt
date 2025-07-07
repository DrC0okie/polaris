package ch.drcookie.polaris_app.ui.viewmodel

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ch.drcookie.polaris_sdk.api.Polaris.networkClient
import ch.drcookie.polaris_sdk.api.Polaris.bleController
import ch.drcookie.polaris_sdk.api.Polaris.initialize
import ch.drcookie.polaris_sdk.api.Polaris.keyStore
import ch.drcookie.polaris_sdk.api.Polaris.protocolHandler
import ch.drcookie.polaris_sdk.api.SdkResult
import ch.drcookie.polaris_sdk.api.config.AuthMode
import ch.drcookie.polaris_sdk.api.use_case.DeliverPayload
import ch.drcookie.polaris_sdk.api.use_case.MonitorBroadcasts
import ch.drcookie.polaris_sdk.api.use_case.PolTransaction
import ch.drcookie.polaris_sdk.api.use_case.PullAndForward
import ch.drcookie.polaris_sdk.api.use_case.FetchBeacons
import ch.drcookie.polaris_sdk.api.use_case.ScanForBeacon
import ch.drcookie.polaris_sdk.api.message
import ch.drcookie.polaris_sdk.ble.model.ConnectionState
import ch.drcookie.polaris_sdk.ble.model.DeliveryAck
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.toUByteArray

/**
 * Represents the state of the main UI, including logs and the status of ongoing operations.
 *
 * @property log The cumulative string of log messages to display on the screen.
 * @property isBusy True if a one-shot, blocking flow (like a PoL transaction) is currently running. Used to disable other buttons.
 * @property isMonitoring True if the continuous broadcast monitoring flow is active.
 */
data class UiState(
    val log: String = "",
    val isBusy: Boolean = false,
    val canStart: Boolean = true,
    val isMonitoring: Boolean = false,
)

/**
 * The ViewModel for the main activity, acting as a bridge between the UI and the Polaris SDK.
 *
 * @param fetchBeacons Use case for registering a new device or fetching beacons for a known one.
 * @param scanForBeacon Use case for performing a one-shot scan for a connectable beacon.
 * @param performPolTransaction Use case for executing a full Proof-of-Location transaction.
 * @param deliverSecurePayload Use case for delivering a server-originated payload to a beacon.
 * @param monitorBroadcasts Use case for listening to non-connectable beacon advertisements.
 * @param pullAndForwardData Use case for pulling data from a beacon and forwarding it to the server.
 */
@OptIn(ExperimentalUnsignedTypes::class)
class PolarisViewModel(
    private val scanForBeacon: ScanForBeacon,
    private val performPolTransaction: PolTransaction,
    private val deliverSecurePayload: DeliverPayload,
    private val monitorBroadcasts: MonitorBroadcasts,
    private val pullAndForwardData: PullAndForward,
    private val fetchBeacons: FetchBeacons,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()
    private var monitoringJob: Job? = null
    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

    /**
     * Initiates the device onboarding flow. If the device is new, it will register.
     * If it's already registered, it will simply fetch the latest beacon list from the server.
     */
    fun fetchBeacons() {
        runFlow("Fetch beacons") {
            appendLog("Registering phone with server...")

            when (val result = fetchBeacons(Build.MODEL, Build.VERSION.RELEASE, "1.0")) {
                is SdkResult.Success -> {
                    val beaconCount = result.value
                    appendLog("Fetch success, found $beaconCount known beacons.")
                }

                is SdkResult.Failure -> {
                    appendLog("--- ERROR: Fetch failed: ${result.error.message()} ---")
                }
            }
        }
    }

    /**
     * Executes the complete Proof-of-Location flow: scans for a beacon, performs the PoL
     * transaction, and submits the resulting token to the server.
     */
    fun findAndExecuteTokenFlow() {
        runFlow("PoL Token Flow") {
            if (networkClient.knownBeacons.isEmpty()) {
                appendLog("No known beacons. Please register first.")
                return@runFlow
            }

            appendLog("Scanning for first known beacon...")

            // Check the scan result
            val foundBeacon = when (val scanResult = scanForBeacon()) {
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
            val token = when (val transactionResult = performPolTransaction(foundBeacon)) {
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

    /**
     * Toggles the continuous monitoring of beacon broadcasts.
     * If not running, it starts a flow that listens for broadcasts and verifies their signatures.
     * If already running, it cancels the flow.
     */
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
                        val verificationStatus =
                            if (verifiedBroadcast.isSignatureValid) "VALID" else "INVALID SIG"
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

    /**
     * Executes the full server-to-beacon payload delivery flow.
     */
    fun processPayloadFlow() {
        runFlow("Secure Payload Delivery") {
            appendLog("Checking for pending payloads...")

            // Get payloads
            val payloads = when (val payloadsResult = networkClient.getPayloadsForDelivery()) {
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
            val ackBlob = when (val deliveryResult = deliverSecurePayload(job)) {
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

    /**
     * Executes a one-shot attempt to find a beacon with pending data and process it.
     */
    fun pullDataFromBeacon() {
        runFlow("Pull Data from Beacon") {

            appendLog("Scanning for beacons with data pending...")

            // Scan for any beacon. We don't filter for a specific one.
            val foundBeacon = when (val scanResult = scanForBeacon()) {
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

    /**
     * Executes a complete end-to-end test flow withing a single BLE connection to the beacon.
     */
    fun runEndToEndStatusCheckFlow() {
        runFlow("End-to-End Check") {
            appendLog("Fetching payload from server...")

            // Get payloads
            val serverJob = when (val result = networkClient.getPayloadsForDelivery()) {
                is SdkResult.Success -> {
                    if (result.value.isEmpty()) {
                        appendLog("No pending payloads from server to start the check.")
                        return@runFlow
                    }
                    result.value.first()
                }

                is SdkResult.Failure -> {
                    appendLog("--- ERROR: Could not get payloads: ${result.error.message()} ---")
                    return@runFlow
                }
            }
            appendLog("   -> Found payload #${serverJob.deliveryId} for beacon #${serverJob.beaconId}.")


            // Find the specific beacon required for this job
            appendLog("Finding target beacon...")
            val targetBeaconInfo = networkClient.knownBeacons.find { it.id == serverJob.beaconId }
            if (targetBeaconInfo == null) {
                appendLog("--- ERROR: Beacon #${serverJob.beaconId} is known by server but not by the client. ---")
                return@runFlow
            }

            // Scan for the beacon
            val foundBeacon =
                when (val result = scanForBeacon(beaconsToFind = listOf(targetBeaconInfo))) {
                    is SdkResult.Success -> result.value
                    is SdkResult.Failure -> {
                        appendLog("--- ERROR: Scan failed: ${result.error.message()} ---")
                        return@runFlow
                    }
                }

            if (foundBeacon == null) {
                appendLog("--- ERROR: Target beacon #${serverJob.beaconId} not found in range. ---")
                return@runFlow
            }
            appendLog("   -> Found beacon '${foundBeacon.name}'.")

            // Connect to the beacon
            try {
                appendLog("Connecting to beacon...")
                when (val connectResult = bleController.connect(foundBeacon.address)) {
                    is SdkResult.Failure -> {
                        appendLog("Connection error: ${connectResult.error.message()}")
                        return@runFlow
                    }

                    is SdkResult.Success -> {} /* Continue */

                }

                // Await a "Ready" or "Failed" state, with a timeout.
                val status = withTimeoutOrNull(10000L) {
                    bleController.connectionState
                        .filter { it is ConnectionState.Ready || it is ConnectionState.Failed }
                        .first()
                }

                // Handle the outcome of the connection attempt.
                when (status) {
                    is ConnectionState.Ready -> {
                        appendLog("   -> Connected.")
                    }

                    is ConnectionState.Failed -> {
                        appendLog("Connection failed: ${status.error}")
                        return@runFlow
                    }

                    null -> {
                        appendLog("Connection timed out.")
                        return@runFlow
                    }

                    else -> {
                        appendLog("Unexpected connection state: $status")
                        return@runFlow
                    }
                }

                // Deliver server payload to beacon
                appendLog("Delivering server payload to beacon...")
                val ackBlob = when (val result =
                    bleController.exchangeSecurePayload(serverJob.blob.asByteArray())) {
                    is SdkResult.Success -> result.value
                    is SdkResult.Failure -> {
                        appendLog("--- ERROR: Server->Beacon delivery failed: ${result.error.message()} ---")
                        return@runFlow
                    }
                }
                appendLog("   -> Beacon acknowledged receipt.")

                // Submit the acknowledgement
                val ack = DeliveryAck(serverJob.deliveryId, ackBlob.toUByteArray())
                when (val result = networkClient.submitSecureAck(ack)) {
                    is SdkResult.Success -> appendLog("   -> Server received the ACK.")
                    is SdkResult.Failure -> {
                        appendLog("--- ERROR: Failed to submit ACK: ${result.error.message()} ---")
                        return@runFlow
                    }
                }

                // Pull the data
                appendLog("Pulling status data from beacon...")
                val beaconData = when (val pullResult = bleController.pullEncryptedData()) {
                    is SdkResult.Success -> pullResult.value
                    is SdkResult.Failure -> {
                        appendLog("Error pulling the data: ${pullResult.error.message()}")
                        return@runFlow
                    }
                }
                appendLog("   -> Received status data from beacon.")

                // Forward data to the server
                appendLog("Forwarding beacon status to server...")
                val serverAck =
                    when (val result =
                        networkClient.forwardBeaconPayload(foundBeacon.info.id, beaconData)) {
                        is SdkResult.Success -> result.value
                        is SdkResult.Failure -> {
                            appendLog("--- ERROR: Failed to forward beacon data: ${result.error.message()} ---")
                            return@runFlow
                        }
                    }
                appendLog("   -> Server acknowledged status.")

                // Relay the server's ACK back to the beacon
                appendLog("Relaying final ACK to beacon...")
                when (val result = bleController.postSecurePayload(serverAck)) {
                    is SdkResult.Success -> appendLog("   -> Final ACK posted to beacon.")
                    is SdkResult.Failure -> {
                        appendLog("--- ERROR: Failed to post final ACK: ${result.error.message()} ---")
                        return@runFlow
                    }
                }

                appendLog("--- SUCCESS: End-to-end flow completed. ---")

            } finally {
                appendLog("9. Disconnecting from beacon...")
                bleController.disconnect()
            }
        }
    }

    /**
     * A helper to wrap one-shot SDK flows, managing the `isBusy` UI state
     * and providing a top-level try-catch for any unexpected errors.
     */
    private fun runFlow(flowName: String, block: suspend () -> Unit) {

        if (_uiState.value.isBusy) {
            appendLog("--- Another flow is already in progress. Please wait. ---")
            return
        }

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

    /** Appends a timestamped message to the UI log. */
    private fun appendLog(message: String) {
        val timestamp = formatter.format(Instant.now())
        _uiState.update {
            val newLog =
                if (it.log.isEmpty()) "$timestamp: $message" else "${it.log}\n$timestamp: $message"
            it.copy(log = newLog)
        }
    }
}

/**
 * A simple factory for creating [PolarisViewModel] instances.
 */
class PolarisViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    // Flag that ensures single initialization
    private companion object {
        private val isSdkInitialized = AtomicBoolean(false)
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PolarisViewModel::class.java)) {

            synchronized(this) {
                if (!isSdkInitialized.get()) {
                    runBlocking {
                        initialize(context) {
                            ble {
                                polServiceUuid = "f44dce36-ffb2-565b-8494-25fa5a7a7cd6"
                                tokenWriteUuid = "8e8c14b7-d9f0-5e5c-9da8-6961e1f33d6b"
                                tokenIndicateUuid = "d234a7d8-ea1f-5299-8221-9cf2f942d3df"
                                encryptedWriteUuid = "8ed72380-5adb-4d2d-81fb-ae6610122ee8"
                                encryptedIndicateUuid = "079b34dd-2310-4b61-89bb-494cc67e097f"
                                pullDataWriteUuid = "e914a8e4-843a-4b72-8f2a-f9175d71cf88"
                                manufacturerId = 0xFFFF
                                mtu = 517
                            }
                            network {
                                baseUrl = "https://polaris.iict-heig-vd.ch" // mandatory
                                authMode = AuthMode.ManagedApiKey
                                registrationPath = "/api/v1/register"
                                beaconsPath = "/api/v1/beacons"
                                tokensPath = "/api/v1/tokens"
                                fetchPayloadsPath = "/api/v1/payloads"
                                forwardPayloadPath = "/api/v1/payloads"
                                ackPath = "/api/v1/payloads/ack"
                            }
                        }
                    }
                    isSdkInitialized.set(true)
                }
            }

            val scanForBeacon = ScanForBeacon(bleController, networkClient)
            val performPolTransaction =
                PolTransaction(bleController, networkClient, keyStore, protocolHandler)
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
