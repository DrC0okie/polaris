package ch.heig.iict.polaris_health.domain.repositories

import ch.heig.iict.polaris_health.domain.model.VisitDetails
import kotlinx.coroutines.flow.Flow

interface VisitRepository {
    fun getTodaysVisits(): Flow<List<VisitDetails>>
    suspend fun updateLockStatusForBeacon(beaconId: Int, isLocked: Boolean)
    suspend fun seedWithDemoData()
}