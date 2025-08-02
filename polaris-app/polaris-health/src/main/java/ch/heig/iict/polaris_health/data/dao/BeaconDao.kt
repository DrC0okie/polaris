package ch.heig.iict.polaris_health.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ch.heig.iict.polaris_health.data.entities.BeaconEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BeaconDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeacon(beacon: BeaconEntity)

    @Query("SELECT * FROM beacons")
    fun getAllBeacons(): Flow<List<BeaconEntity>>
}