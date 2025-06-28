package ch.drcookie.polaris_sdk.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android

/**
 * Provides the Ktor Android engine.
 */
internal actual fun getHttpClientEngine(): HttpClientEngine = Android.create()