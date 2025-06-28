package ch.drcookie.polaris_sdk.api

import android.content.Context

/**
 * Platform-specific implementation of [PlatformContext] for Android.
 *
 * This typealias maps the common [PlatformContext] to the standard Android `Context`,
 * which is required to initialize Android-specific components like `KVault` and the `BleController`.
 */
internal actual typealias PlatformContext = Context