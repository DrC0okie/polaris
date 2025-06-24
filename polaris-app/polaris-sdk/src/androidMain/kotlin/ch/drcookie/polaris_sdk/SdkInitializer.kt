package ch.drcookie.polaris_sdk

import ch.drcookie.polaris_sdk.data.datasource.ble.BleDataSourceImpl
import ch.drcookie.polaris_sdk.data.datasource.local.UserPreferences
import ch.drcookie.polaris_sdk.data.datasource.remote.RemoteDataSource
import ch.drcookie.polaris_sdk.data.repository.AuthRepositoryImpl
import ch.drcookie.polaris_sdk.data.repository.KeyRepositoryImpl
import ch.drcookie.polaris_sdk.data.repository.ProtocolRepositoryImpl
import ch.drcookie.polaris_sdk.domain.interactor.logic.BeaconDataParser
import ch.drcookie.polaris_sdk.domain.interactor.logic.CryptoManager
import io.github.oshai.kotlinlogging.KotlinLogging
import ch.drcookie.polaris_sdk.domain.repository.AuthRepository
import ch.drcookie.polaris_sdk.domain.repository.BleDataSource
import ch.drcookie.polaris_sdk.domain.repository.KeyRepository
import ch.drcookie.polaris_sdk.domain.repository.LocalPreferences
import ch.drcookie.polaris_sdk.domain.repository.ProtocolRepository
import com.ionspin.kotlin.crypto.LibsodiumInitializer

private val logger = KotlinLogging.logger {}

// Provide the 'actual' implementation for the SdkInitializer
actual class SdkInitializer {

    // This is a simple data class to hold our initialized repositories.
    // It implements the common interface.
    private class AndroidSdk(
        override val authRepository: AuthRepository,
        override val keyRepository: KeyRepository,
        override val protocolRepository: ProtocolRepository,
        override val bleDataSource: BleDataSource,
        // Also hold references to things that need to be shut down
        private val remoteDataSource: RemoteDataSource
    ) : PolarisApi {
        fun performShutdown() {
            bleDataSource.cancelAll()
            remoteDataSource.shutdown()
        }
    }

    private var sdkInstance: AndroidSdk? = null

    actual suspend fun initialize(context: PlatformContext): PolarisApi {
        logger.info { "Initializing Polaris SDK for Android..." }

        try {
            LibsodiumInitializer.initialize()
        } catch (e: Exception) {
            logger.error(e) { "FATAL: Libsodium initialization failed." }
            throw e // Re-throw to fail initialization
        }

        val cryptoManager = CryptoManager
        val beaconDataParser = BeaconDataParser
        val localPreferences: LocalPreferences = UserPreferences(context)
        val remoteDataSource = RemoteDataSource()

        val authRepositoryImpl = AuthRepositoryImpl(remoteDataSource, localPreferences)
        val keyRepositoryImpl = KeyRepositoryImpl(localPreferences, cryptoManager)
        val protocolRepositoryImpl = ProtocolRepositoryImpl(cryptoManager)
        val bleDataSourceImpl = BleDataSourceImpl(context, beaconDataParser)

        // Create the holder object and store it.
        val instance = AndroidSdk(
            authRepository = authRepositoryImpl,
            keyRepository = keyRepositoryImpl,
            protocolRepository = protocolRepositoryImpl,
            bleDataSource = bleDataSourceImpl,
            remoteDataSource = remoteDataSource
        )
        this.sdkInstance = instance
        return instance
    }

    actual fun shutdown() {
        logger.info { "Shutting down Polaris SDK for Android..." }
        sdkInstance?.performShutdown()
        sdkInstance = null
    }
}