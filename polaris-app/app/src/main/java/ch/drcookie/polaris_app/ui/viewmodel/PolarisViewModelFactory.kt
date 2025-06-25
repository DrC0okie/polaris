package ch.drcookie.polaris_app.ui.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ch.drcookie.polaris_sdk.api.Polaris
import ch.drcookie.polaris_sdk.api.flows.DeliverPayloadFlow
import ch.drcookie.polaris_sdk.api.flows.MonitorBroadcastsFlow
import ch.drcookie.polaris_sdk.api.flows.PolTransactionFlow
import ch.drcookie.polaris_sdk.api.flows.RegisterDeviceFlow
import ch.drcookie.polaris_sdk.api.flows.ScanForBeaconFlow

class PolarisViewModelFactory() : ViewModelProvider.Factory {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PolarisViewModel::class.java)) {

            val registerDevice = RegisterDeviceFlow(Polaris.apiClient, Polaris.keyStore)
            val scanForBeacon = ScanForBeaconFlow(Polaris.bleController, Polaris.apiClient)
            val deliverSecurePayload =
                DeliverPayloadFlow(Polaris.bleController, Polaris.apiClient, scanForBeacon)
            val performPolTransaction = PolTransactionFlow(
                Polaris.bleController,
                Polaris.apiClient,
                Polaris.keyStore,
                Polaris.protocolHandler
            )
            val monitorBroadcasts =
                MonitorBroadcastsFlow(Polaris.bleController, Polaris.apiClient, Polaris.protocolHandler)

            @Suppress("UNCHECKED_CAST")
            return PolarisViewModel(
                apiClient = Polaris.apiClient,
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