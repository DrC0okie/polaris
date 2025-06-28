package ch.drcookie.polaris_sdk.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

/**
 * Provides the Ktor Darwin engine.
 */
internal actual fun getHttpClientEngine(): HttpClientEngine = Darwin.create()