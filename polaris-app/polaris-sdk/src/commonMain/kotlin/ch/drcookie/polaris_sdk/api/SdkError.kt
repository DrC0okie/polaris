package ch.drcookie.polaris_sdk.api

/**
 * Represents the different types of predictable errors that can occur within the Polaris SDK.
 * This sealed class allows for handling of failure cases by the SDK consumer.
 */
public sealed class SdkError {
    /** An error related to Bluetooth Low Energy operations (scanning, connecting, GATT communication). */
    public data class BleError(public val message: String) : SdkError()

    /** An error related to network operations (e.g., HTTP errors, parsing issues). */
    public data class NetworkError(public val message: String) : SdkError()

    /** An error related to the cryptographic protocol (e.g., signature verification failed). */
    public data class ProtocolError(public val message: String) : SdkError()

    /** An error indicating the SDK was used in an invalid state (e.g., calling a protected function before configuration). */
    public data class PreconditionError(public val message: String) : SdkError()

    /** A wrapper for unexpected exceptions that are not part of the predictable business logic. */
    public data class GenericError(public val throwable: Throwable) : SdkError()
}

public fun SdkError.message(): String {
    return when (this) {
        is SdkError.BleError -> "A Bluetooth error occurred: $message"
        is SdkError.NetworkError -> "A network error occurred: $message"
        is SdkError.ProtocolError -> "A protocol error occurred: $message"
        is SdkError.PreconditionError -> message
        is SdkError.GenericError -> "An unexpected error occurred: ${throwable.message}"
    }
}