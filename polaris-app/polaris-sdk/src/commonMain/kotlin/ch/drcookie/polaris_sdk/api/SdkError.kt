package ch.drcookie.polaris_sdk.api

/**
 * Represents a structured error that can occur during an SDK operation.
 *
 * Used as the failure type in an [SdkResult].
 *
 * ### Example:
 * ```kotlin
 * when (val result = Polaris.networkClient.fetchBeacons()) {
 *     is SdkResult.Success -> { /* ... */ }
 *     is SdkResult.Failure -> {
 *         when (result.error) {
 *             is SdkError.BleError -> log("BLE issue: ${result.error.message}")
 *             is SdkError.NetworkError -> log("Network issue: ${result.error.message}")
 *             is SdkError.PreconditionError -> log("Setup issue: ${result.error.message}")
 *             is SdkError.ProtocolError -> log("Protocol issue: ${result.error.message}")
 *             is SdkError.GenericError -> log("Unexpected issue: ${result.error.throwable}")
 *         }
 *     }
 * }
 * ```
 */
public sealed class SdkError {

    /**
     * An error related to Bluetooth Low Energy operations.
     *
     * @property message A message describing the BLE failure.
     */
    public data class BleError(public val message: String) : SdkError()

    /**
     * An error related to network operations.
     *
     * @property message A message describing the network failure.
     */
    public data class NetworkError(public val message: String) : SdkError()

    /**
     * An error related to the cryptographic protocol logic.
     *
     * @property message A message describing the protocol failure.
     */
    public data class ProtocolError(public val message: String) : SdkError()

    /**
     * An error indicating the SDK was used in an invalid state or a necessary precondition was not met.
     *
     * @property message A message explaining the precondition that failed.
     */
    public data class PreconditionError(public val message: String) : SdkError()

    /**
     * A wrapper for unexpected exceptions that are not part of the SDK's predictable failure modes.
     * This acts as a catch-all to prevent crashes and ensure all failures are channeled through [SdkResult].
     *
     * @property throwable The original `Throwable` that was caught.
     */
    public data class GenericError(public val throwable: Throwable) : SdkError()
}

/**
 * A display-ready message for any [SdkError].
 */
public fun SdkError.message(): String {
    return when (this) {
        is SdkError.BleError -> "A Bluetooth error occurred: $message"
        is SdkError.NetworkError -> "A network error occurred: $message"
        is SdkError.ProtocolError -> "A protocol error occurred: $message"
        is SdkError.PreconditionError -> message
        is SdkError.GenericError -> "An unexpected error occurred: ${throwable.message}"
    }
}