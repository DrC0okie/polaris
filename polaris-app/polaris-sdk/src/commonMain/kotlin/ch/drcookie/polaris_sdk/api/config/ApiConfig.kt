package ch.drcookie.polaris_sdk.api.config

import com.liftric.kvault.KVault

/**
 * Configuration for the Polaris network client.
 * Providing this configuration during SDK initialization enables server communication.
 *
 * @property baseUrl The base URL of the Polaris backend server (e.g., "https://polaris.example.com").
 * @property registrationPath The API path for device registration.
 * @property tokensPath The API path for submitting PoL tokens.
 * @property payloadsPath The API path for fetching and acknowledging secure payloads.
 */
public data class ApiConfig(
    val baseUrl: String,
    val authMode: AuthMode,
    val registrationPath: String,
    val tokensPath: String,
    val payloadsPath: String,
    val ackPath: String
)

/**
 * A builder for creating an [ApiConfig].
 * Used within the `Polaris.initialize` DSL to configure server communication.
 */
public class ApiConfigBuilder() {
    /** The base URL of the Polaris backend server (e.g., "https://polaris.example.com"). */
    public lateinit var baseUrl: String

    public var authMode: AuthMode = AuthMode.ManagedApiKey

    /** The API path for device registration. */
    public var registrationPath: String = "/api/v1/register"

    /** The API path for submitting PoL tokens. */
    public var tokensPath: String = "/api/v1/tokens"

    /** The API path for fetching and acknowledging secure payloads. */
    public var payloadsPath: String = "/api/v1/payloads"

    public var ackPath: String = "/api/v1/payloads/ack"

    internal fun build(): ApiConfig {
        // This check runs when the DSL block is finished, ensuring the user provided the mandatory fields.
        check(::baseUrl.isInitialized) { "baseUrl must be set in the api { ... } block." }
        return ApiConfig(
            baseUrl = baseUrl,
            authMode = authMode,
            registrationPath = registrationPath,
            tokensPath = tokensPath,
            payloadsPath = payloadsPath,
            ackPath = ackPath
        )
    }
}