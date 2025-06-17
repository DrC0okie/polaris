package ch.drcookie.polaris_app.repository

import android.bluetooth.le.ScanResult
import ch.drcookie.polaris_app.data.ble.BleDataSource
import ch.drcookie.polaris_app.data.ble.ConnectionState
import ch.drcookie.polaris_app.data.local.UserPreferences
import ch.drcookie.polaris_app.data.model.PoLRequest
import ch.drcookie.polaris_app.data.model.PoLToken
import ch.drcookie.polaris_app.data.model.dto.*
import ch.drcookie.polaris_app.data.remote.RemoteDataSource
import ch.drcookie.polaris_app.util.Crypto
import ch.drcookie.polaris_app.util.PoLConstants
import ch.drcookie.polaris_app.util.Utils.toUIntLE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull

@OptIn(ExperimentalUnsignedTypes::class)
class PolarisRepository(
    private val bleDataSource: BleDataSource,
    private val remoteDataSource: RemoteDataSource,
    private val userPrefs: UserPreferences
) {
    val connectionState: StateFlow<ConnectionState> = bleDataSource.connectionState
    var knownBeacons: List<BeaconProvisioningDto> = emptyList()

    suspend fun registerPhoneAndFetchBeacons(req: PhoneRegistrationRequestDto): List<BeaconProvisioningDto> {
        val response = remoteDataSource.registerPhone(req)
        userPrefs.apiKey = response.apiKey
        userPrefs.phoneId = response.assignedPhoneId ?: -1L
        knownBeacons = response.beacons.beacons
        return knownBeacons
    }

    fun findFirstKnownBeacon(): Flow<Pair<ScanResult, BeaconProvisioningDto>> {
        return bleDataSource.scanForBeacons().mapNotNull { scanResult ->
            val manufData = scanResult.scanRecord?.getManufacturerSpecificData(PoLConstants.MANUFACTURER_ID)
            if (manufData != null && manufData.size >= 4) {
                val detectedId = manufData.toUByteArray().toUIntLE()
                val matchedBeacon = knownBeacons.find { it.beaconId == detectedId }
                if (matchedBeacon != null) {
                    return@mapNotNull scanResult to matchedBeacon
                }
            }
            null
        }
    }

    suspend fun connectAndRequestToken(scanResult: ScanResult, targetBeacon: BeaconProvisioningDto): PoLToken {
        bleDataSource.connect(scanResult.device.address)
        connectionState.first { it is ConnectionState.Ready }

        val (phonePk, phoneSk) = Crypto.getOrGeneratePhoneKeyPair()
        val phoneId = userPrefs.phoneId.toULong()

        var request = PoLRequest(
            flags = 0u,
            phoneId = phoneId,
            beaconId = targetBeacon.beaconId,
            nonce = Crypto.generateNonce(),
            phonePk = phonePk
        )
        request = Crypto.signPoLRequest(request, phoneSk)

        val response = bleDataSource.requestPoL(request)

        val isValid = Crypto.verifyPoLResponse(response, request, targetBeacon.publicKey)
        if (!isValid) throw SecurityException("Invalid beacon signature!")

        val token = PoLToken.create(request, response, targetBeacon.publicKey)
        bleDataSource.disconnect()
        return token
    }

    suspend fun submitToken(token: PoLToken) {
        val apiKey = userPrefs.apiKey ?: throw IllegalStateException("API Key not found")
        remoteDataSource.sendPoLToken(token, apiKey)
    }

    fun disconnect() = bleDataSource.disconnect()

    fun shutdown() {
        bleDataSource.cancelAll()
        remoteDataSource.shutdown()
    }

}