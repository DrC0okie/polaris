package ch.heig.iict.polaris_health.ui.tour

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.drcookie.polaris_sdk.api.SdkResult
import ch.drcookie.polaris_sdk.api.message
import ch.drcookie.polaris_sdk.api.use_case.*
import ch.heig.iict.polaris_health.domain.repositories.VisitRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TourViewModel(
    private val visitRepository: VisitRepository,
    private val monitorBroadcasts: MonitorBroadcasts
) : ViewModel() {

    private val _uiState = MutableStateFlow(TourUiState())
    val uiState: StateFlow<TourUiState> = _uiState.asStateFlow()

    private var monitoringJob: Job? = null
    private val watchdogJobs = mutableMapOf<Int, Job>()

    init {
        viewModelScope.launch{ visitRepository.seedWithDemoData()}

        visitRepository.getTodaysVisits()
            .onEach { visits ->
                _uiState.update { it.copy(isLoading = false, visits = visits) }
            }
            .launchIn(viewModelScope)

        startMonitoring()
    }

    fun startMonitoring() {
        if (monitoringJob?.isActive == true) return

        _uiState.update { it.copy(isMonitoring = true) }

        monitoringJob = monitorBroadcasts.startMonitoring()
            .onEach { result ->
                when (result) {
                    is SdkResult.Success -> {
                        val broadcast = result.value
                        if (broadcast.isSignatureValid) {
                            val beaconId = broadcast.payload.beaconId.toInt()

                            // On a reçu un broadcast valide, on annule le re-verrouillage
                            watchdogJobs[beaconId]?.cancel()

                            // On déverrouille
                            visitRepository.updateLockStatusForBeacon(beaconId, isLocked = false)

                            // On programme le re-verrouillage dans 10 secondes
                            watchdogJobs[beaconId] = viewModelScope.launch {
                                delay(10000)
                                visitRepository.updateLockStatusForBeacon(beaconId, isLocked = true)
                            }
                        }
                    }
                    is SdkResult.Failure -> {
                        _uiState.update { it.copy(errorMessage = result.error.message()) }
                        stopMonitoring()
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        watchdogJobs.values.forEach { it.cancel() }
        watchdogJobs.clear()
        _uiState.update { it.copy(isMonitoring = false) }
    }

    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
    }
}