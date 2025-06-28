package ch.drcookie.polaris_sdk.api

/**
 * Represents a platform-specific context required for SDK initialization.
 *
 * This is a typealias that maps to a platform-specific class on each target.
 * - On **Android**, this corresponds to `android.content.Context`.
 * - On **iOS**, this is a placeholder and is not currently used.
 *
 * You must provide an instance of the appropriate context when calling [Polaris.initialize].
 *
 * @see Polaris.initialize
 */
public expect abstract class PlatformContext()