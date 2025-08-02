package ch.heig.iict.polaris_health.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import ch.heig.iict.polaris_health.data.entities.PoLTokenEntity

@Dao
interface PoLTokenDao {
    @Insert
    suspend fun insert(token: PoLTokenEntity)

    @Query("SELECT * FROM pol_tokens WHERE is_synced = 0")
    suspend fun getUnsyncedTokens(): List<PoLTokenEntity>

    @Query("UPDATE pol_tokens SET is_synced = 1 WHERE id IN (:tokenIds)")
    suspend fun markAsSynced(tokenIds: List<Long>)
}