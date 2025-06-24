package ch.drcookie.polaris_app.ui.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ch.drcookie.polaris_sdk.Polaris
import ch.drcookie.polaris_sdk.domain.interactor.*

class PolarisViewModelFactory() : ViewModelProvider.Factory {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PolarisViewModel::class.java)) {

            val registerDevice = RegisterDeviceInteractor(Polaris.authRepository, Polaris.keyRepository)
            val scanForBeacon = ScanConnectableBeaconInteractor(Polaris.bleDataSource, Polaris.authRepository)
            val deliverSecurePayload = DeliverPayloadInteractor(Polaris.bleDataSource, Polaris.authRepository, scanForBeacon)
            val performPolTransaction = PolTransactionInteractor(Polaris.bleDataSource, Polaris.authRepository, Polaris.keyRepository, Polaris.protocolRepository)
            val monitorBroadcasts = MonitorBroadcastsInteractor(Polaris.bleDataSource, Polaris.authRepository, Polaris.protocolRepository)

            @Suppress("UNCHECKED_CAST")
            return PolarisViewModel(
                authRepository = Polaris.authRepository,
                registerDevice = registerDevice,
                scanForBeacon = scanForBeacon,
                performPolTransaction = performPolTransaction,
                deliverSecurePayload = deliverSecurePayload,
                monitorBroadcasts = monitorBroadcasts
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}