package ch.drcookie.polaris_sdk.api.config

import ch.drcookie.polaris_sdk.api.Polaris

/**
 * An immutable configuration holder for the Polaris network client.
 *
 * This object is created by the [NetworkConfigBuilder] within the `api { ... }` block
 * of the [Polaris.initialize] function.
 *
 * @property baseUrl The base URL of the Polaris backend server (e.g., "https://polaris.example.com").
 * @property authMode The authentication strategy to use for network requests. See [AuthMode] for options.
 * @property registrationPath The API path for device registration.
 * @property beaconsPath The API path for fetching the list of known beacons for an already-registered device.
 * @property tokensPath The API path for submitting Proof-of-Location tokens.
 * @property fetchPayloadsPath The API path for fetching secure payloads from the server (Server -> Beacon).
 * @property forwardPayloadPath The API path for forwarding secure payloads from a beacon to the server (Beacon -> Server).
 * @property ackPath The API path for submitting acknowledgements for server-to-beacon payloads.
 */
public data class NetworkConfig(
    val baseUrl: String,
    val authMode: AuthMode,
    val registrationPath: String,
    val beaconsPath: String,
    val tokensPath: String,
    val fetchPayloadsPath: String,
    val forwardPayloadPath: String,
    val ackPath: String
)

/**
 * A builder for creating an [NetworkConfig] for the network client.
 *
 * Use this builder within the `api { ... }` block of the [Polaris.initialize] function
 * to configure and enable all server communication features.
 *
 * ### Example:
 * ```
 * Polaris.initialize(context) {
 *     api {
 *         baseUrl = "https://my-polaris-server.com"
 *         authMode = AuthMode.ManagedApiKey
 *     }
 * }
 * ```
 */
public class NetworkConfigBuilder() {
    /** The base URL of the Polaris backend server (e.g., "https://polaris.example.com"). This property is mandatory. */
    public lateinit var baseUrl: String

    /**
     * The authentication strategy to use. Defaults to [AuthMode.ManagedApiKey].
     * @see AuthMode for all available options.
     */
    public var authMode: AuthMode = AuthMode.ManagedApiKey

    /** The API path for device registration. */
    public var registrationPath: String = "/api/v1/register"

    /** The API path for fetching the active beacons from the server. */
    public var beaconsPath: String = "/api/v1/beacons"

    /** The API path for submitting PoL tokens. */
    public var tokensPath: String = "/api/v1/tokens"

    /** The API path for fetching and acknowledging secure payloads. */
    public var fetchPayloadsPath: String = "/api/v1/payloads"

    /** The API path for fetching and acknowledging secure payloads. */
    public var forwardPayloadPath: String = "/api/v1/payloads" // We will POST here

    public var ackPath: String = "/api/v1/payloads/ack"

    internal fun build(): NetworkConfig {
        // This check runs when the DSL block is finished, ensuring the user provided the mandatory fields.
        check(::baseUrl.isInitialized) { "baseUrl must be set in the api { ... } block." }
        return NetworkConfig(
            baseUrl = baseUrl,
            authMode = authMode,
            registrationPath = registrationPath,
            beaconsPath = beaconsPath,
            tokensPath = tokensPath,
            fetchPayloadsPath = fetchPayloadsPath,
            forwardPayloadPath = forwardPayloadPath,
            ackPath = ackPath
        )
    }
}