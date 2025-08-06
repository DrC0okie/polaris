package ch.heig.iict.polaris_health.ui.visit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ch.drcookie.polaris_sdk.api.use_case.*
import ch.heig.iict.polaris_health.di.AppContainer
import ch.heig.iict.polaris_health.domain.repositories.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class VisitDetailViewModel(
    private val visitId: Long,
    private val visitRepository: VisitRepository,
    private val tokenRepository: TokenRepository,
    private val scanForBeacon: ScanForBeacon,
    private val polTransaction: PolTransaction

) : ViewModel() {

    private val _uiState = MutableStateFlow(VisitDetailUiState())
    val uiState: StateFlow<VisitDetailUiState> = _uiState.asStateFlow()

    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault())

    init {
        viewModelScope.launch {
            visitRepository.getTodaysVisits().collect { visits ->
                val currentVisit = visits.find { it.visitId == visitId }
                if (currentVisit != null) {
                    _uiState.update {
                        it.copy(
                            patientFullName = currentVisit.patientFullName,
                            patientDetails = "Beacon ID: ${currentVisit.associatedBeaconId}",
                            isTokenButtonEnabled = !currentVisit.isLocked
                        )
                    }
                }
            }
        }
    }

    fun onGenerateTokenClicked() {
        throw NotImplementedError("Not yet implemented")
    }

    fun onSyncClicked() {
        throw NotImplementedError("Not yet implemented")
    }

    fun onCheckMaintenanceClicked() {
        throw NotImplementedError("Not yet implemented")
    }

    private fun appendLog(message: String) {
        val timestamp = formatter.format(Instant.now())
        _uiState.update {
            val newLog = if (it.log.isEmpty()) "$timestamp: $message" else "${it.log}\n$timestamp: $message"
            it.copy(log = newLog)
        }
    }

    private fun runFlow(flowName: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            appendLog("--- Démarrage: $flowName ---")
            try {
                block()
            } catch (e: Exception) {
                appendLog("--- ERREUR dans $flowName: ${e.message} ---")
            } finally {
                _uiState.update { it.copy(isBusy = false) }
                appendLog("--- Terminé: $flowName ---")
            }
        }
    }

    companion object {
        fun provideFactory(visitId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                VisitDetailViewModel(
                    visitId,
                    AppContainer.visitRepository,
                    AppContainer.tokenRepository,
                    AppContainer.scanForBeacon,
                    AppContainer.polTransaction
                )
            }
        }
    }
}