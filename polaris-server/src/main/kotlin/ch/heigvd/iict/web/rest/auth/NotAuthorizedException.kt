package ch.heigvd.iict.web.rest.auth

import ch.heigvd.iict.web.rest.mappers.NotAuthorizedExceptionMapper

/**
 * A custom runtime exception thrown by the [ApiKeyFilter] when authentication fails.
 *
 * This exception is caught by the [NotAuthorizedExceptionMapper] to produce a
 * standardized 401 Unauthorized HTTP response.
 *
 * @param message A message describing the cause of the authorization failure.
 */
class NotAuthorizedException(message: String) : RuntimeException(message)