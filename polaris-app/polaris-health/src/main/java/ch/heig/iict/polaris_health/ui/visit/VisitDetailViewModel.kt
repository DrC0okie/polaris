package ch.heig.iict.polaris_health.ui.visit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ch.drcookie.polaris_sdk.api.Polaris.networkClient
import ch.drcookie.polaris_sdk.api.SdkResult
import ch.drcookie.polaris_sdk.api.message
import ch.drcookie.polaris_sdk.api.use_case.*
import ch.drcookie.polaris_sdk.ble.model.DeliveryAck
import ch.heig.iict.polaris_health.di.AppContainer
import ch.heig.iict.polaris_health.domain.repositories.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val polTransaction: PolTransaction,
    private val deliverPayload: DeliverPayload,

) : ViewModel() {

    private val _uiState = MutableStateFlow(VisitDetailUiState())
    val uiState: StateFlow<VisitDetailUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    private var gracePeriodJob: Job? = null

    companion object {
        fun provideFactory(visitId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                VisitDetailViewModel(
                    visitId,
                    AppContainer.visitRepository,
                    AppContainer.tokenRepository,
                    AppContainer.scanForBeacon,
                    AppContainer.polTransaction,
                    AppContainer.deliverPayload
                )
            }
        }
        const val GRACE_PERIOD_MS = 5000L
    }

    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault())

    init {
        viewModelScope.launch {
            visitRepository.getTodaysVisits()
                .mapNotNull { visits -> visits.find { it.visitId == visitId } }
                .distinctUntilChanged()
                .collect { currentVisit ->
                    _uiState.update {
                        it.copy(
                            patientFullName = currentVisit.patientFullName,
                            patientDetails = "Beacon ID: ${currentVisit.associatedBeaconId}",
                            isTokenButtonEnabled = !currentVisit.isLocked
                        )
                    }
                    handleLockStateChange(currentVisit.isLocked)
                }
        }
    }

    private fun handleLockStateChange(isLocked: Boolean) {
        if (isLocked) {
            // Le dossier vient de se verrouiller. On lance le délai de grâce.
            gracePeriodJob?.cancel() // Annuler un ancien timer s'il existe
            gracePeriodJob = viewModelScope.launch {
                appendLog("Beacon hors de portée. Fermeture du dossier dans 5 secondes...")
                delay(GRACE_PERIOD_MS)
                // Si le job n'a pas été annulé, on déclenche le retour
                _navigationEvent.emit(NavigationEvent.NavigateBack)
            }
        } else {
            // Le dossier vient de se déverrouiller. On annule le délai de grâce.
            gracePeriodJob?.cancel()
            appendLog("Beacon détecté. Accès autorisé.")
        }
    }

    fun onGenerateTokenClicked() {
        runFlow("Génération de la Preuve de Visite") {
            val currentVisit = uiState.value.let { state ->
                // Récupérer la visite actuelle depuis le state de l'UI
                visitRepository.getTodaysVisits().first().find { it.visitId == visitId }
            }

            if (currentVisit?.associatedBeaconId == null) {
                appendLog("ERREUR : Aucun beacon n'est associé à ce patient.")
                return@runFlow
            }

            appendLog("Recherche du beacon #${currentVisit.associatedBeaconId}...")
            val beaconInfo =
                networkClient.knownBeacons.find { it.id.toInt() == currentVisit.associatedBeaconId }
            if (beaconInfo == null) {
                appendLog("ERREUR : Les informations du beacon #${currentVisit.associatedBeaconId} n'ont pas été trouvées.")
                return@runFlow
            }

            val scanResult = scanForBeacon(beaconsToFind = listOf(beaconInfo))
            val foundBeacon = when (scanResult) {
                is SdkResult.Success -> scanResult.value
                is SdkResult.Failure -> {
                    appendLog("--- ERREUR SCAN: ${scanResult.error.message()} ---")
                    return@runFlow
                }
            }

            if (foundBeacon == null) {
                appendLog("Le beacon du patient n'a pas été trouvé à proximité.")
                return@runFlow
            }

            appendLog("Beacon trouvé : ${foundBeacon.name}. Connexion et génération du token...")
            when (val transactionResult = polTransaction(foundBeacon)) {
                is SdkResult.Success -> {
                    val token = transactionResult.value
                    tokenRepository.storeToken(visitId, token)
                    appendLog("Preuve de visite générée et stockée localement !")
                }

                is SdkResult.Failure -> {
                    appendLog("--- ERREUR TRANSACTION: ${transactionResult.error.message()} ---")
                }
            }
        }
    }

    fun onSyncClicked() {
        runFlow("Synchronisation") {
            appendLog("Tentative de synchronisation des données...")

            // On peut synchroniser les tokens et rafraîchir la liste des beacons
            when (val result = tokenRepository.syncPendingTokens()) {
                is SdkResult.Success -> {
                    appendLog("${result.value} preuve(s) de visite synchronisée(s) avec succès.")
                }
                is SdkResult.Failure -> {
                    appendLog("--- ERREUR SYNCHRO: ${result.error.message()} ---")
                }
            }
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun onCheckMaintenanceClicked() {
        runFlow("Vérification de la Maintenance") {
            appendLog("Recherche de tâches de maintenance sur le serveur...")

            val payloadsResult = networkClient.getPayloadsForDelivery()
            val payloads = when (payloadsResult) {
                is SdkResult.Success -> payloadsResult.value
                is SdkResult.Failure -> {
                    appendLog("--- ERREUR RÉSEAU: ${payloadsResult.error.message()} ---")
                    return@runFlow
                }
            }

            if (payloads.isEmpty()) {
                appendLog("Aucune tâche de maintenance en attente.")
                return@runFlow
            }

            val job = payloads.first()
            appendLog("Tâche trouvée : #${job.deliveryId} pour beacon #${job.beaconId}. Tentative de livraison...")

            when (val deliveryResult = deliverPayload(job)) {
                is SdkResult.Success -> {
                    val ackBlob = deliveryResult.value
                    val ack = DeliveryAck(job.deliveryId, ackBlob.toUByteArray())
                    appendLog("Tâche livrée au beacon. Envoi de l'accusé de réception au serveur...")

                    when (val ackSubmitResult = networkClient.submitSecureAck(ack)) {
                        is SdkResult.Success -> appendLog("Tâche de maintenance terminée avec succès !")
                        is SdkResult.Failure -> appendLog("--- ERREUR ACK: ${ackSubmitResult.error.message()} ---")
                    }
                }
                is SdkResult.Failure -> {
                    appendLog("--- ERREUR LIVRAISON: ${deliveryResult.error.message()} ---")
                }
            }
        }
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

    sealed class NavigationEvent {
        object NavigateBack : NavigationEvent()
    }
}