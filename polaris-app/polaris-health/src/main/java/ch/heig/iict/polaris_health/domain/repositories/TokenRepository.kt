package ch.heig.iict.polaris_health.domain.repositories

import ch.drcookie.polaris_sdk.api.SdkError
import ch.drcookie.polaris_sdk.api.SdkResult
import ch.drcookie.polaris_sdk.protocol.model.PoLToken

interface TokenRepository {

    suspend fun storeToken(visitId: Long, token: PoLToken)
    suspend fun syncPendingTokens(): SdkResult<Int, SdkError>
}