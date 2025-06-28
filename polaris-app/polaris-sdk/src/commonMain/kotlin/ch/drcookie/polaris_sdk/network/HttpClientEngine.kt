package ch.drcookie.polaris_sdk.network

import io.ktor.client.engine.HttpClientEngine

/**
 * Provides a platform-specific Ktor [HttpClientEngine].
 *
 * This pattern allows the shared Ktor client configuration in `commonMain` to be
 * instantiated with the appropriate engine for each target platform
 *
 * @return The default [HttpClientEngine] for the current platform.
 */
internal expect fun getHttpClientEngine(): HttpClientEngine