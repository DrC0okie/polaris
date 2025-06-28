package ch.drcookie.polaris_sdk.api

/**
 * Encapsulates the result of a fallible SDK operation.
 *
 * An operation can either succeed, returning a [Success] containing the desired `value`,
 * or fail, returning a [Failure] containing a [SdkError].
 *
 * Used as the return type for all asynchronous operations in the SDK that can have a predictable failure.
 *
 * @param T The type of the value in case of success.
 * @param E The type of the error in case of failure, typically [SdkError].
 */
public sealed class SdkResult<out T, out E> {
    /**
     * Represents a successful result of an operation.
     * @property value The data returned by the successful operation.
     */
    public data class Success<out T>(public val value: T) : SdkResult<T, Nothing>()

    /**
     * Represents a failed result of an operation.
     * @property error A structured [SdkError] object containing details about the failure.
     */
    public data class Failure<out E>(public val error: E) : SdkResult<Nothing, E>()
}