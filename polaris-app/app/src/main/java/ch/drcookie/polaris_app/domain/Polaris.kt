package ch.drcookie.polaris_app.domain

import android.content.Context
import ch.drcookie.polaris_app.data.datasource.ble.BleDataSourceImpl
import com.ionspin.kotlin.crypto.LibsodiumInitializer
import ch.drcookie.polaris_app.data.datasource.local.UserPreferences
import ch.drcookie.polaris_app.data.datasource.remote.RemoteDataSource
import ch.drcookie.polaris_app.data.repository.AuthRepositoryImpl
import ch.drcookie.polaris_app.data.repository.KeyRepositoryImpl
import ch.drcookie.polaris_app.data.repository.ProtocolRepositoryImpl
import ch.drcookie.polaris_app.domain.interactor.logic.BeaconDataParser
import ch.drcookie.polaris_app.domain.interactor.logic.CryptoManager
import ch.drcookie.polaris_app.domain.repository.*
import io.github.oshai.kotlinlogging.KotlinLogging

private val Log = KotlinLogging.logger {}

/**
 * The main entry point for the Polaris SDK.
 * This object must be initialized once in the Application's onCreate method.
 */
object Polaris {

    // Hold the instances of the repositories after initialization
    lateinit var authRepository: AuthRepository
        private set
    lateinit var keyRepository: KeyRepository
        private set
    lateinit var protocolRepository: ProtocolRepository
        private set
    lateinit var bleDataSource: BleDataSource
        private set
    lateinit var remoteDataSource: RemoteDataSource
        private set

    private var isInitialized = false

    /**
     * Initializes the SDK and all its components.
     * This must be called once, typically from the Application's onCreate method.
     *
     * @param context The Android Application context.
     */
    suspend fun initialize(context: Context) {
        if (isInitialized) {
            Log.warn { "PolarisSdk already initialized." }
            return
        }

        // Initialize critical libraries
        try {
            LibsodiumInitializer.initialize()
            Log.debug { "Libsodium initialized successfully." }
        } catch (e: Exception) {
            Log.error(e) { "FATAL: Libsodium initialization failed. The SDK will not work." }
            return // Stop initialization if crypto fails
        }

        // Build the dependency graph
        val cryptoManager = CryptoManager
        val beaconDataParser = BeaconDataParser
        val localPreferences: LocalPreferences = UserPreferences(context)

        // Repositories
        authRepository = AuthRepositoryImpl(RemoteDataSource(), localPreferences)
        keyRepository = KeyRepositoryImpl(localPreferences, cryptoManager)
        protocolRepository = ProtocolRepositoryImpl(cryptoManager)
        bleDataSource = BleDataSourceImpl(context, beaconDataParser)

        isInitialized = true
        Log.info { "PolarisSdk initialized successfully." }
    }

    /**
     * Shuts down the SDK, closing network clients and BLE connections.
     * This should be called from the Application's onTerminate method.
     */
    fun shutdown() {
        if (!isInitialized) return

        Log.info { "Shutting down PolarisSdk..." }
        bleDataSource.cancelAll()
        remoteDataSource.shutdown()

        isInitialized = false
    }
}