package ch.heig.iict.polaris_health.data.repositories

import ch.drcookie.polaris_sdk.api.Polaris
import ch.drcookie.polaris_sdk.api.SdkError
import ch.drcookie.polaris_sdk.api.SdkResult
import ch.drcookie.polaris_sdk.protocol.model.PoLToken
import ch.heig.iict.polaris_health.data.dao.PoLTokenDao
import ch.heig.iict.polaris_health.data.entities.PoLTokenEntity
import ch.heig.iict.polaris_health.domain.repositories.TokenRepository

class TokenRepositoryImpl( private val tokenDao: PoLTokenDao) : TokenRepository {

    private val networkClient = Polaris.networkClient

    override suspend fun storeToken(visitId: Long, token: PoLToken) {
        val entity = token.toEntity(visitId)
        tokenDao.insert(entity)
    }

    override suspend fun syncPendingTokens(): SdkResult<Int, SdkError> {
        val unsyncedTokens = tokenDao.getUnsyncedTokens()
        if (unsyncedTokens.isEmpty()) {
            return SdkResult.Success(0)
        }

        val successfullySyncedIds = mutableListOf<Long>()

        for (tokenEntity in unsyncedTokens) {
            val sdkToken = tokenEntity.toPoLToken()
            when (networkClient.submitPoLToken(sdkToken)) {
                is SdkResult.Success -> {
                    successfullySyncedIds.add(tokenEntity.id)
                }
                is SdkResult.Failure -> {
                    return SdkResult.Failure(SdkError.NetworkError("Failed to sync token ${tokenEntity.id}"))
                }
            }
        }

        if (successfullySyncedIds.isNotEmpty()) {
            tokenDao.markAsSynced(successfullySyncedIds)
        }

        return SdkResult.Success(successfullySyncedIds.size)
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
private fun PoLToken.toEntity(visitId: Long): PoLTokenEntity {
    return PoLTokenEntity(
        visitId = visitId,
        flags = this.flags.toByte(),
        phoneId = this.phoneId.toLong(),
        beaconId = this.beaconId.toInt(),
        beaconCounter = this.beaconCounter.toLong(),
        nonce = this.nonce.asByteArray(),
        phonePk = this.phonePk.asByteArray(),
        beaconPk = this.beaconPk.asByteArray(),
        phoneSig = this.phoneSig.asByteArray(),
        beaconSig = this.beaconSig.asByteArray()
    )
}

@OptIn(ExperimentalUnsignedTypes::class)
private fun PoLTokenEntity.toPoLToken(): PoLToken {
    return PoLToken(
        this.flags.toUByte(),
        this.phoneId.toULong(),
        this.beaconId.toUInt(),
        this.beaconCounter.toULong(),
        this.nonce.asUByteArray(),
        this.phonePk.asUByteArray(),
        this.beaconPk.asUByteArray(),
        this.phoneSig.asUByteArray(),
        this.beaconSig.asUByteArray()
    )
}