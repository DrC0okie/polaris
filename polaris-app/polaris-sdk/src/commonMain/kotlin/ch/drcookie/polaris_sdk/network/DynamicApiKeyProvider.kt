package ch.drcookie.polaris_sdk.network

import ch.drcookie.polaris_sdk.storage.SdkPreferences

/**
 * A helper class for the common use case where the API key is discovered at runtime
 * (e.g., from a /register endpoint) and then used for subsequent requests.
 *
 * Create an instance of this class and pass it to the `authInterceptor` in the SDK configuration.
 * The SDK will automatically wire it up to its internal preferences storage.
 */
public class DynamicApiKeyProvider : HttpInterceptor {

    internal lateinit var preferences: SdkPreferences
    override fun intercept(builder: io.ktor.client.request.HttpRequestBuilder) {
        // Check if the preferences have been initialized by the SDK.
        if (!::preferences.isInitialized) {
            return
        }
        // Retrieve the key from the wired-up preferences and add the header.
        preferences.apiKey?.let { key ->
            builder.headers.append("x-api-key", key)
        }
    }
}