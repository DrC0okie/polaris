package ch.drcookie.polaris_sdk.api

/**
 * A generic class that represents the result of an SDK operation, which can either
 * be a success containing a value or a failure containing an error.
 * This class is inspired by the Either/Result monad and is used to handle
 * errors explicitly without relying on exceptions for control flow.
 *
 * @param T The type of the success value.
 * @param E The type of the error value.
 */
public sealed class SdkResult<out T, out E> {
    /** Represents a successful result containing a value. */
    public data class Success<out T>(public val value: T) : SdkResult<T, Nothing>()

    /** Represents a failed result containing an error. */
    public data class Failure<out E>(public val error: E) : SdkResult<Nothing, E>()
}