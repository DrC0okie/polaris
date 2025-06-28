package ch.drcookie.polaris_sdk.api.config

/**
 * Defines the authentication strategy to be used for network requests to protected endpoints.
 *
 * This is configured via the `authMode` property in the `api { ... }` block during SDK initialization.
 */
public sealed class AuthMode {
    /**
     * No authentication will be used. The SDK will not add any authorization headers to requests.
     */
    public object None : AuthMode()

    /**
     * The SDK will manage the API key automatically.
     * It expects the [NetworkConfig.registrationPath] endpoint to return an API key, which it will securely store
     * and automatically use for all subsequent protected calls.
     */
    public object ManagedApiKey : AuthMode()

    /**
     * A static, predefined API key will be used for all protected requests.
     * @property apiKey The static API key to include in the `x-api-key` header.
     */
    public data class StaticApiKey(public val apiKey: String) : AuthMode()
}