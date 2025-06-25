package ch.drcookie.polaris_sdk.network

import io.github.oshai.kotlinlogging.KotlinLogging
import ch.drcookie.polaris_sdk.model.PoLToken
import ch.drcookie.polaris_sdk.network.dto.AckRequestDto
import ch.drcookie.polaris_sdk.network.dto.BeaconProvisioningListDto
import ch.drcookie.polaris_sdk.network.dto.EncryptedPayloadListDto
import ch.drcookie.polaris_sdk.network.dto.PhoneRegistrationRequestDto
import ch.drcookie.polaris_sdk.network.dto.PhoneRegistrationResponseDto
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

internal object KtorClientFactory {
    private const val BASE_URL = "https://polaris.iict-heig-vd.ch"

    // Configure the HttpClient
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

    internal suspend fun registerPhone(request: PhoneRegistrationRequestDto)
            : PhoneRegistrationResponseDto =
        client.post("$BASE_URL/api/v1/register") {
            setBody(request)
        }.body<PhoneRegistrationResponseDto>()


    internal suspend fun fetchBeacons(apiKey: String)
            : BeaconProvisioningListDto =
        client.get("$BASE_URL/api/v1/beacons") {
            header("x-api-key", apiKey)
        }.body<BeaconProvisioningListDto>()

    internal suspend fun sendPoLToken(token: PoLToken, apiKey: String): Boolean {
        val resp = client.post("$BASE_URL/api/v1/tokens") {
            header("x-api-key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(token)
        }
        val responseBody = resp.bodyAsText()
        Log.debug { "Sending PoLToken to $BASE_URL/api/v1/tokens" }
        Log.debug { "Response from the server: $responseBody" }
        return resp.status.isSuccess()
    }

    internal suspend fun getPayloads(apiKey: String): EncryptedPayloadListDto =
        client.get("$BASE_URL/api/v1/payloads") {
            header("x-api-key", apiKey)
        }.body<EncryptedPayloadListDto>()

    internal suspend fun postAck(apiKey: String, request: AckRequestDto): Boolean {
        val resp = client.post("$BASE_URL/api/v1/payloads/ack") {
            header("x-api-key", apiKey)
            setBody(request)
        }
        return resp.status.isSuccess()
    }

    internal fun closeClient() {
        client.close()
        Log.debug { "Ktor HTTP client closed." }
    }
}