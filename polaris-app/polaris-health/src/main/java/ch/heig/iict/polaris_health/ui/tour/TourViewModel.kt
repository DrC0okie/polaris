package ch.heig.iict.polaris_health.ui.tour

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ch.drcookie.polaris_sdk.api.SdkResult
import ch.drcookie.polaris_sdk.api.message
import ch.drcookie.polaris_sdk.api.use_case.*
import ch.heig.iict.polaris_health.di.AppContainer
import ch.heig.iict.polaris_health.domain.repositories.VisitRepository
import ch.heig.iict.polaris_health.ui.shared.Event
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class TourViewModel(
    private val visitRepository: VisitRepository,
    private val monitorBroadcasts: MonitorBroadcasts
) : ViewModel() {

    private val _uiState = MutableStateFlow(TourUiState())
    val uiState: StateFlow<TourUiState> = _uiState.asStateFlow()

    private val _errorEvent = MutableSharedFlow<Event<String>>()
    val errorEvent: SharedFlow<Event<String>> = _errorEvent.asSharedFlow()

    private var proximityMonitoringJob: Job? = null
    private val watchdogJobs = mutableMapOf<Int, Job>()

    companion object {
        const val SCAN_DURATION_MS = 3000L
        const val SCAN_INTERVAL_MS = 10000L
        const val WATCHDOG_DELAY_MS = 15000L
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                TourViewModel(
                    AppContainer.visitRepository,
                    AppContainer.monitorBroadcasts
                )
            }
        }
    }

    init {
        viewModelScope.launch { visitRepository.seedWithDemoData() }

        visitRepository.getTodaysVisits()
            .onEach { visits -> _uiState.update { it.copy(isLoading = false, visits = visits) } }
            .launchIn(viewModelScope)
    }

    /**
     * Manages activation/deactivation of proximityMode.
     * Called By the swipe-to-refresh action in the Fragment
     */
    fun toggleProximityMode() {
        if (_uiState.value.isInProximityMode) {
            exitProximityMode()
        } else {
            enterProximityMode()
        }
    }

    private fun enterProximityMode() {
        if (proximityMonitoringJob?.isActive == true) return

        _uiState.update { it.copy(isInProximityMode = true) }

        proximityMonitoringJob = viewModelScope.launch {
            while (isActive) {
                scanForBeaconsPeriodically()
                delay(SCAN_INTERVAL_MS)
            }
        }
    }

    private fun exitProximityMode() {
        proximityMonitoringJob?.cancel()
        proximityMonitoringJob = null
        watchdogJobs.values.forEach { it.cancel() }
        watchdogJobs.clear()

        viewModelScope.launch {
            _uiState.value.visits.forEach { visit ->
                if (!visit.isLocked) {
                    visit.associatedBeaconId?.let { beaconId ->
                        visitRepository.updateLockStatusForBeacon(beaconId, isLocked = true)
                    }
                }
            }
        }

        _uiState.update { it.copy(isInProximityMode = false, isRefreshing = false) }
    }

    private suspend fun scanForBeaconsPeriodically() {
        if (_uiState.value.isRefreshing) return

        _uiState.update { it.copy(isRefreshing = true) }
        var hasFoundAtLeastOneBeacon = false

        val scanJob = monitorBroadcasts.startMonitoring()
            .onEach { result ->
                when (result) {
                    is SdkResult.Success -> {
                        val broadcast = result.value
                        if (broadcast.isSignatureValid) {
                            if (!hasFoundAtLeastOneBeacon) {
                                hasFoundAtLeastOneBeacon = true
                                // On arrête l'indicateur visuel, mais pas le scan de fond
                                _uiState.update { it.copy(isRefreshing = false) }
                            }
                            handleValidBroadcast(broadcast.payload.beaconId.toInt())
                        }
                    }
                    is SdkResult.Failure -> {
                        _errorEvent.emit(Event(result.error.message()))
                        exitProximityMode()
                    }
                }
            }
            .onCompletion {
                if (!hasFoundAtLeastOneBeacon) {
                    _uiState.update { it.copy(isRefreshing = false) }
                }
            }
            .launchIn(viewModelScope)

        delay(SCAN_DURATION_MS)
        scanJob.cancel()
    }

    private fun handleValidBroadcast(beaconId: Int) {
        watchdogJobs[beaconId]?.cancel()
        viewModelScope.launch {
            visitRepository.updateLockStatusForBeacon(beaconId, isLocked = false)
        }
        watchdogJobs[beaconId] = viewModelScope.launch {
            delay(WATCHDOG_DELAY_MS)
            visitRepository.updateLockStatusForBeacon(beaconId, isLocked = true)
        }
    }

    override fun onCleared() {
        super.onCleared()
        exitProximityMode()
    }
}