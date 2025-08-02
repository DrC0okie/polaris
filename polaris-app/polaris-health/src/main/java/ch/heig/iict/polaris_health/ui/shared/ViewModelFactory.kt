package ch.heig.iict.polaris_health.ui.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ch.drcookie.polaris_sdk.api.Polaris
import ch.drcookie.polaris_sdk.api.use_case.*
import ch.heig.iict.polaris_health.domain.repositories.TokenRepository
import ch.heig.iict.polaris_health.domain.repositories.VisitRepository
import ch.heig.iict.polaris_health.ui.tour.TourViewModel
import ch.heig.iict.polaris_health.ui.visit.VisitDetailViewModel

class ViewModelFactory(
    private val visitRepository: VisitRepository,
    private val tokenRepository: TokenRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val scanForBeacon = ScanForBeacon(Polaris.bleController, Polaris.networkClient)
        val performPolTransaction =
            PolTransaction(Polaris.bleController, Polaris.networkClient, Polaris.keyStore, Polaris.protocolHandler)
        val deliverSecurePayload = DeliverPayload(Polaris.bleController, Polaris.networkClient, scanForBeacon)
        val monitorBroadcasts = MonitorBroadcasts(Polaris.bleController, Polaris.networkClient, Polaris.protocolHandler)
        val pullAndForwardData = PullAndForward(Polaris.bleController, Polaris.networkClient)
        val fetchBeacons = FetchBeacons(Polaris.networkClient, Polaris.keyStore)

        return when {
            modelClass.isAssignableFrom(TourViewModel::class.java) -> {
                TourViewModel(visitRepository, monitorBroadcasts) as T
            }
            modelClass.isAssignableFrom(VisitDetailViewModel::class.java) -> {
                // FIXME : Inject visitID dynamically
                VisitDetailViewModel(0, visitRepository, tokenRepository, scanForBeacon, performPolTransaction) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}