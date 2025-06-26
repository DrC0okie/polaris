package ch.drcookie.polaris_sdk.network

import ch.drcookie.polaris_sdk.api.config.ApiConfig
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

internal class KtorClientFactory(private val config: ApiConfig) {

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
        client.post("${config.baseUrl}${config.registrationPath}") {
            setBody(request)
        }.body<PhoneRegistrationResponseDto>()


    internal suspend fun fetchBeacons(apiKey: String?)
            : BeaconProvisioningListDto =
        client.get("${config.baseUrl}${config.tokensPath}") {
            apiKey?.let { header("x-api-key", it) }
        }.body<BeaconProvisioningListDto>()

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

    internal suspend fun getPayloads(apiKey: String?): EncryptedPayloadListDto =
        client.get("${config.baseUrl}${config.payloadsPath}") {
            apiKey?.let { header("x-api-key", it) }
        }.body<EncryptedPayloadListDto>()

    internal suspend fun postAck(request: AckRequestDto, apiKey: String?): Boolean {
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