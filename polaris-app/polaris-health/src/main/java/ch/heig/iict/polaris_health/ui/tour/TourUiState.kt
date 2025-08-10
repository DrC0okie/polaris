package ch.heig.iict.polaris_health.ui.tour

import ch.heig.iict.polaris_health.domain.model.VisitDetails

data class TourUiState(
    val isLoading: Boolean = true,
    val visits: List<VisitDetails> = emptyList(),
    val isRefreshing: Boolean = false, // True only during swipe/scan
    val isInProximityMode: Boolean = false, // True while we monitor the nearby beacon broadcasts
    val errorMessage: String? = null
)