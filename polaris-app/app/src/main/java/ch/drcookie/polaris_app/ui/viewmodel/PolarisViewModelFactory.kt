package ch.drcookie.polaris_app.ui.viewmodel

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ch.drcookie.polaris_app.data.datasource.local.UserPreferences
import ch.drcookie.polaris_app.data.repository.AuthRepositoryImpl
import ch.drcookie.polaris_app.domain.interactor.*
import ch.drcookie.polaris_app.domain.interactor.logic.*
import ch.drcookie.polaris_app.domain.repository.*
import ch.drcookie.polaris_app.ui.PolarisApplication

class PolarisViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PolarisViewModel::class.java)) {
            val polarisApplication = application as PolarisApplication
            val bleDataSource = polarisApplication.bleDataSource
            val remoteDataSource = polarisApplication.remoteDataSource
            val localPreferences: LocalPreferences = UserPreferences(application.applicationContext)
            val authRepository: AuthRepository = AuthRepositoryImpl(remoteDataSource, localPreferences)

            val cryptoManager = CryptoManager
            val beaconDataParser = BeaconDataParser
            val signatureVerifier = SignatureVerifier

            val registerDevice = RegisterDeviceInteractor(authRepository, cryptoManager)
            val scanForBeacon = ScanConnectableBeaconInteractor(bleDataSource, authRepository, beaconDataParser)
            val deliverSecurePayload = DeliverPayloadInteractor(bleDataSource, authRepository, scanForBeacon)
            val performPolTransaction = PolTransactionInteractor(bleDataSource, localPreferences, cryptoManager)
            val monitorBroadcasts =
                MonitorBroadcastsInteractor(bleDataSource, authRepository, beaconDataParser, signatureVerifier)

            @Suppress("UNCHECKED_CAST")
            return PolarisViewModel(
                authRepository = authRepository,
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