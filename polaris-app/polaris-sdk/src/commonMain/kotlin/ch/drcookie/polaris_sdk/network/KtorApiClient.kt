package ch.drcookie.polaris_sdk.network

import ch.drcookie.polaris_sdk.api.config.ApiConfig
import ch.drcookie.polaris_sdk.ble.model.Beacon
import ch.drcookie.polaris_sdk.ble.model.DeliveryAck
import ch.drcookie.polaris_sdk.ble.model.EncryptedPayload
import ch.drcookie.polaris_sdk.model.PoLToken
import ch.drcookie.polaris_sdk.network.dto.AckRequestDto
import ch.drcookie.polaris_sdk.network.dto.PhoneRegistrationRequestDto
import ch.drcookie.polaris_sdk.storage.SdkPreferences

internal class KtorApiClient(
    private val userPrefs: SdkPreferences,
    apiConfig: ApiConfig?,
) : ApiClient {

    private val notInitializedErr = "ApiClient not configured. Did you forget the api { ... } block in Polaris.initialize?"
    // Only create the client factory if config is not null
    private val api: KtorClientFactory? = apiConfig?.let { KtorClientFactory(it) }

    private var _knownBeacons = mutableListOf<Beacon>()

    // Holds the list of beacons after a successful registration/fetch
    override val knownBeacons: List<Beacon>
        get() = _knownBeacons

    override fun getPhoneId(): Long {
        // It simply delegates the call to the underlying preference storage.
        return userPrefs.phoneId
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun registerPhone(
        publicKey: UByteArray,
        deviceModel: String,
        osVersion: String,
        appVersion: String,
    ): List<Beacon> {

        val requestDto = PhoneRegistrationRequestDto(
            publicKey = publicKey,
            deviceModel = deviceModel,
            osVersion = osVersion,
            appVersion = appVersion
        )

        val response = api?.registerPhone(requestDto)?: throw IllegalStateException(notInitializedErr)

        // Store credentials
        userPrefs.apiKey = response.apiKey
        userPrefs.phoneId = response.assignedPhoneId ?: -1L

        // Map the DTOs to Public Models and store/return them
        val newBeacons = response.beacons.beacons.map { it.toBeaconInfo() }
        _knownBeacons.clear()
        _knownBeacons.addAll(newBeacons)
        return newBeacons
    }

    override suspend fun submitPoLToken(token: PoLToken) {
        val apiKey = userPrefs.apiKey ?: throw IllegalStateException("API Key not available.")
        api?.sendPoLToken(token, apiKey)?:throw IllegalStateException(notInitializedErr)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun submitSecureAck(ack: DeliveryAck) {
        val apiKey = userPrefs.apiKey ?: throw IllegalStateException("API Key not available.")
        // MAP the Public Model to an internal DTO to send to the server
        val ackDto = AckRequestDto(
            deliveryId = ack.deliveryId,
            ackBlob = ack.ackBlob
        )
        api?.postAck(apiKey, ackDto)?: throw IllegalStateException(notInitializedErr)
    }

    override suspend fun getPayloadsForDelivery(): List<EncryptedPayload> {
        val apiKey = userPrefs.apiKey ?: throw IllegalStateException("API Key not available.")
        val dtoList = api?.getPayloads(apiKey)?.payloads?: throw IllegalStateException(notInitializedErr)
        // MAP the DTOs to Public Models before returning
        return dtoList.map { it.toEncryptedPayload() }
    }

    override fun closeClient() {
        api?.closeClient()?: throw IllegalStateException(notInitializedErr)
    }
}