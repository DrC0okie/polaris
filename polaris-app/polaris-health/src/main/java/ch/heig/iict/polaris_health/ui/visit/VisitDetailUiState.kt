package ch.heig.iict.polaris_health.ui.visit

data class VisitDetailUiState(
    val patientFullName: String = "",
    val patientDetails: String = "",
    val log: String = "",
    val isBusy: Boolean = false,
    val isTokenButtonEnabled: Boolean = false
)