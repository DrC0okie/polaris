package ch.drcookie.polaris_sdk.network

import ch.drcookie.polaris_sdk.api.config.NetworkConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import ch.drcookie.polaris_sdk.model.PoLToken
import ch.drcookie.polaris_sdk.network.dto.AckDto
import ch.drcookie.polaris_sdk.network.dto.BeaconProvisioningListDto
import ch.drcookie.polaris_sdk.network.dto.EncryptedPayloadListDto
import ch.drcookie.polaris_sdk.network.dto.PhoneRegistrationRequestDto
import ch.drcookie.polaris_sdk.network.dto.PhoneRegistrationResponseDto
import ch.drcookie.polaris_sdk.network.dto.BeaconPayloadDto
import ch.drcookie.polaris_sdk.network.dto.RawDataDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private val Log = KotlinLogging.logger {}

/**
 * Factory responsible for creating and configuring the Ktor HTTP client and executing raw network requests.
 *
 * @property config The network configuration containing the base URL and API paths.
 */
internal class KtorClientFactory(private val config: NetworkConfig) {

    /** The configured Ktor [HttpClient] instance. */
    internal val client = HttpClient(getHttpClientEngine()) {
        expectSuccess = true

        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.info { message }
                }
            }
            level = LogLevel.ALL
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }

        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }

    /** Performs a POST request to the registration endpoint. */
    internal suspend fun registerPhone(request: PhoneRegistrationRequestDto) : PhoneRegistrationResponseDto =
        client.post("${config.baseUrl}${config.registrationPath}") {
            setBody(request)
        }.body<PhoneRegistrationResponseDto>()


    /** Performs a GET request to the beacons list endpoint. */
    internal suspend fun fetchBeacons(apiKey: String?): BeaconProvisioningListDto =
        client.get("${config.baseUrl}${config.beaconsPath}") {
            apiKey?.let { header("x-api-key", it) }
        }.body<BeaconProvisioningListDto>()

    /** Performs a POST request to submit a PoL token. */
    internal suspend fun sendPoLToken(token: PoLToken, apiKey: String?): Boolean {
        val path = config.tokensPath
        val resp = client.post("${config.baseUrl}$path") {
            apiKey?.let { header("x-api-key", it) }
            contentType(ContentType.Application.Json)
            setBody(token)
        }
        val responseBody = resp.bodyAsText()
        Log.debug { "Sending PoLToken to $path" }
        Log.debug { "Response from the server: $responseBody" }
        return resp.status.isSuccess()
    }

    /** Performs a GET request to fetch pending server-to-beacon payloads. */
    internal suspend fun getPayload(apiKey: String?): EncryptedPayloadListDto =
        client.get("${config.baseUrl}${config.fetchPayloadsPath}") {
            apiKey?.let { header("x-api-key", it) }
        }.body<EncryptedPayloadListDto>()

    /** Performs a POST request to forward a beacon-to-server payload. */
    internal suspend fun forwardPayload(request: BeaconPayloadDto, apiKey: String?): RawDataDto =
        client.post("${config.baseUrl}${config.forwardPayloadPath}") {
            apiKey?.let { header("x-api-key", it) }
            setBody(request)
        }.body<RawDataDto>()

    /** Performs a POST request to submit an acknowledgement. */
    internal suspend fun postAck(request: AckDto, apiKey: String?): Boolean {
        val resp = client.post("${config.baseUrl}${config.ackPath}") {
            apiKey?.let { header("x-api-key", it) }
            setBody(request)
        }
        return resp.status.isSuccess()
    }

    internal fun closeClient() {
        client.close()
        Log.debug { "Ktor HTTP client closed." }
    }
}