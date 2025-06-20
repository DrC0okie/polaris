package ch.drcookie.polaris_app.data.remote

import android.util.Log
import ch.drcookie.polaris_app.data.model.PoLToken
import ch.drcookie.polaris_app.data.model.dto.AckRequestDto
import ch.drcookie.polaris_app.data.model.dto.BeaconProvisioningListDto
import ch.drcookie.polaris_app.data.model.dto.EncryptedPayloadListDto
import ch.drcookie.polaris_app.data.model.dto.PhoneRegistrationRequestDto
import ch.drcookie.polaris_app.data.model.dto.PhoneRegistrationResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.LoggingConfig
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object ApiService {
    private const val TAG = "ApiService"
    private const val BASE_URL = "https://polaris.iict-heig-vd.ch"

    // Configure the HttpClient
    @OptIn(ExperimentalUnsignedTypes::class)
    val client = HttpClient(Android) {
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
                    Log.v(TAG, "KtorLog: $message")
                }
            }
            logger = Logger.Companion.DEFAULT
            level = LogLevel.ALL
//            filter { request -> request.url.host.contains("ktor.io") }
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }

        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }

    suspend fun registerPhone(request: PhoneRegistrationRequestDto)
            : PhoneRegistrationResponseDto =
        client.post("$BASE_URL/api/v1/register") {
            setBody(request)
        }.body<PhoneRegistrationResponseDto>()

    suspend fun fetchBeacons(apiKey: String)
            : BeaconProvisioningListDto =
        client.get("$BASE_URL/api/v1/beacons") {
            header("x-api-key", apiKey)
        }.body<BeaconProvisioningListDto>()

    suspend fun sendPoLToken(token: PoLToken, apiKey: String): Boolean {
        val resp = client.post("$BASE_URL/api/v1/tokens") {
            header("x-api-key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(token)
        }
        Log.d(TAG, "Sending PoLToken to $BASE_URL/api/v1/tokens")
        Log.d(TAG, "Response from the server: ${resp.bodyAsText()}")
        return resp.status.isSuccess()
    }

    suspend fun getPayloads(apiKey: String): EncryptedPayloadListDto =
        client.get("$BASE_URL/api/v1/payloads") {
            header("x-api-key", apiKey)
        }.body<EncryptedPayloadListDto>()

    suspend fun postAck(apiKey: String, request: AckRequestDto): Boolean {
        val resp = client.post("$BASE_URL/api/v1/payloads/ack") {
            header("x-api-key", apiKey)
            setBody(request)
        }
        return resp.status.isSuccess()
    }

    fun closeClient() {
        client.close()
        Log.d(TAG, "Ktor HTTP client closed.")
    }
}