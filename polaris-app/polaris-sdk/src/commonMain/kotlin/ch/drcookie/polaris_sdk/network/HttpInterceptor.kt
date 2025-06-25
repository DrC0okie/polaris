package ch.drcookie.polaris_sdk.network

import io.ktor.client.request.HttpRequestBuilder

/**
 * An interceptor that can modify an outgoing HTTP request, typically for authentication.
 */
public fun interface HttpInterceptor {
    /**
     * Intercepts and modifies the [HttpRequestBuilder] before it is sent.
     * @param builder The request builder to modify.
     */
    public fun intercept(builder: HttpRequestBuilder)
}

/**
 * Creates an interceptor that adds a static API key to the 'x-api-key' header.
 * @param apiKey The static API key to use for all requests.
 */
public fun ApiKeyInterceptor(apiKey: String): HttpInterceptor {
    return HttpInterceptor { builder -> builder.headers.append("x-api-key", apiKey) }
}

/**
 * Creates an interceptor that dynamically retrieves an API key from the given provider
 * and adds it to the 'x-api-key' header.
 * @param keyProvider A function that returns the current API key. If it returns null, no header is added.
 */
public fun ApiKeyProviderInterceptor(keyProvider: () -> String?): HttpInterceptor {
    return HttpInterceptor { builder -> keyProvider()?.let { builder.headers.append("x-api-key", it) } }
}