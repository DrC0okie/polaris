package ch.drcookie.polaris_sdk.network

import ch.drcookie.polaris_sdk.api.SdkResult
import ch.drcookie.polaris_sdk.api.SdkError
import ch.drcookie.polaris_sdk.api.config.ApiConfig
import ch.drcookie.polaris_sdk.api.config.AuthMode
import ch.drcookie.polaris_sdk.ble.model.Beacon
import ch.drcookie.polaris_sdk.ble.model.DeliveryAck
import ch.drcookie.polaris_sdk.ble.model.EncryptedPayload
import ch.drcookie.polaris_sdk.model.PoLToken
import ch.drcookie.polaris_sdk.network.dto.AckRequestDto
import ch.drcookie.polaris_sdk.network.dto.PhoneRegistrationRequestDto
import com.liftric.kvault.KVault

internal class KtorApiClient(
    private val store: KVault,
    private val config: ApiConfig,
) : ApiClient {

    private val factory = KtorClientFactory(config)

    private companion object {
        const val KEY_API_KEY = "polaris_api_key"
        const val KEY_PHONE_ID = "polaris_phone_id"
    }

    private val unknownErr = "Unknown network error"

    private var _knownBeacons = mutableListOf<Beacon>()

    // Holds the list of beacons after a successful registration/fetch
    override val knownBeacons: List<Beacon>
        get() = _knownBeacons

    override fun getPhoneId(): Long {
        // It simply delegates the call to the underlying preference storage.
        return store.long(forKey = KEY_PHONE_ID) ?: -1L
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun registerPhone(
        publicKey: UByteArray,
        deviceModel: String,
        osVersion: String,
        appVersion: String,
    ): SdkResult<List<Beacon>, SdkError> {
        return runCatching {

            val requestDto = PhoneRegistrationRequestDto(
                publicKey = publicKey,
                deviceModel = deviceModel,
                osVersion = osVersion,
                appVersion = appVersion
            )

            val response = factory.registerPhone(requestDto)

            // only save the key if we are in ManagedApiKey mode
            if (config.authMode is AuthMode.ManagedApiKey) {
                store.set(KEY_API_KEY, response.apiKey)
            }
            store.set(KEY_PHONE_ID, response.assignedPhoneId ?: -1L)

            // Map the DTOs to Public Models and store/return them
            val newBeacons = response.beacons.beacons.map { it.toBeaconInfo() }
            _knownBeacons.clear()
            _knownBeacons.addAll(newBeacons)
            newBeacons
        }.fold(
            onSuccess = { SdkResult.Success(it) },
            onFailure = {
                SdkResult.Failure(
                    SdkError.NetworkError(
                        it.message ?: "$unknownErr during registration"
                    )
                )
            }
        )
    }

    override suspend fun submitPoLToken(token: PoLToken): SdkResult<Unit, SdkError> {

        return runCatching {

            val apiKeyResult = getApiKeyForRequest()
            val apiKey = when(apiKeyResult) {
                is SdkResult.Success -> apiKeyResult.value
                is SdkResult.Failure -> return apiKeyResult
            }

            factory.sendPoLToken(token, apiKey)
        }.fold(
            onSuccess = { isSuccess ->
                if (isSuccess) SdkResult.Success(Unit)
                else SdkResult.Failure(SdkError.NetworkError("Server rejected PoL token."))
            },
            onFailure = { throwable ->
                SdkResult.Failure(
                    SdkError.NetworkError(throwable.message ?: "$unknownErr during PoL token submission")
                )
            }
        )
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun submitSecureAck(ack: DeliveryAck): SdkResult<Unit, SdkError> {

        return runCatching {

            val apiKeyResult = getApiKeyForRequest()
            val apiKey = when(apiKeyResult) {
                is SdkResult.Success -> apiKeyResult.value
                is SdkResult.Failure -> return apiKeyResult
            }

            val ackDto = AckRequestDto(
                deliveryId = ack.deliveryId,
                ackBlob = ack.ackBlob
            )

            factory.postAck(ackDto, apiKey)
        }.fold(
            onSuccess = { isSuccess ->
                if (isSuccess) {
                    SdkResult.Success(Unit)
                } else {
                    SdkResult.Failure(SdkError.NetworkError("Server rejected the payload ack."))
                }
            },
            onFailure = { throwable ->
                SdkResult.Failure(
                    SdkError.NetworkError(throwable.message ?: "$unknownErr during payload ack")
                )
            }
        )
    }

    override suspend fun getPayloadsForDelivery(): SdkResult<List<EncryptedPayload>, SdkError> {
        return runCatching {

            val apiKeyResult = getApiKeyForRequest()
            val apiKey = when(apiKeyResult) {
                is SdkResult.Success -> apiKeyResult.value
                is SdkResult.Failure -> return apiKeyResult
            }

            val dtoList = factory.getPayloads(apiKey)
            dtoList.payloads.map { it.toEncryptedPayload() }
        }.fold(
            onSuccess = { payloads ->
                SdkResult.Success(payloads)
            },
            onFailure = { throwable ->
                SdkResult.Failure(
                    SdkError.NetworkError(throwable.message ?: "$unknownErr while fetching payloads")
                )
            }
        )
    }

    override fun closeClient() {
        factory.closeClient()
    }

    private fun getApiKeyForRequest(): SdkResult<String?, SdkError> {
        val apiKey = when (val mode = config.authMode) {
            is AuthMode.None -> null
            is AuthMode.ManagedApiKey -> store.string(forKey = KEY_API_KEY)
            is AuthMode.StaticApiKey -> mode.apiKey
        }

        if (config.authMode != AuthMode.None && apiKey == null) {
            return SdkResult.Failure(SdkError.PreconditionError("API key is required but not found."))
        }
        return SdkResult.Success(apiKey)
    }
}