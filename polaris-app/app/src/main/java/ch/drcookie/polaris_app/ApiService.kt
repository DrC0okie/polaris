package ch.drcookie.polaris_app

import android.util.Log
import ch.drcookie.polaris_app.dto.BeaconProvisioningListDto
import ch.drcookie.polaris_app.dto.PhoneRegistrationRequestDto
import ch.drcookie.polaris_app.dto.PhoneRegistrationResponseDto
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.logging.*

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
            logger = Logger.DEFAULT
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
        return resp.status.isSuccess()
    }

    suspend fun sendPoLToken(token: PoLToken): Boolean {
        val endpoint = "$BASE_URL/token"
        Log.d(TAG, "Sending PoLToken to $endpoint")
        try {
            val response: HttpResponse = client.post(endpoint) {
                contentType(ContentType.Application.Json)
                setBody(token)
            }
            Log.d(TAG, "Server response status: ${response.status}")
            val responseBody: String = response.bodyAsText()
            Log.d(TAG, "Server response body: $responseBody")
            return response.status.isSuccess()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending PoLToken", e)
            return false
        }
    }

    fun closeClient() {
        client.close()
        Log.d(TAG, "Ktor HTTP client closed.")
    }
}