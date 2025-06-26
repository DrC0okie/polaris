package ch.drcookie.polaris_sdk.api.config

/**
 * Defines the authentication mode to be used for network requests.
 */
public sealed class AuthMode {
    /**
     * No authentication will be used. Endpoints are assumed to be open.
     */
    public object None : AuthMode()

    /**
     * The SDK will manage the API key automatically. It will be retrieved from the
     * /register endpoint and used for all subsequent protected calls.
     * This is the standard, recommended mode for Polaris.
     */
    public object ManagedApiKey : AuthMode()

    /**
     * A static, predefined API key will be used for all protected requests.
     * @property apiKey The static API key.
     */
    public data class StaticApiKey(public val apiKey: String) : AuthMode()
}