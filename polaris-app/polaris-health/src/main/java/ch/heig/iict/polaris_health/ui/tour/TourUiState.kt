package ch.heig.iict.polaris_health.ui.tour

import ch.heig.iict.polaris_health.domain.model.VisitDetails

data class TourUiState(
    val isLoading: Boolean = true,
    val visits: List<VisitDetails> = emptyList(),
    val isMonitoring: Boolean = false,
    val errorMessage: String? = null
)