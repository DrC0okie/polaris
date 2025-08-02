package ch.heig.iict.polaris_health.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ch.heig.iict.polaris_health.data.dao.BeaconDao
import ch.heig.iict.polaris_health.data.dao.PatientDao
import ch.heig.iict.polaris_health.data.dao.PoLTokenDao
import ch.heig.iict.polaris_health.data.dao.VisitDao
import ch.heig.iict.polaris_health.data.entities.BeaconEntity
import ch.heig.iict.polaris_health.data.entities.MedicalRecordEntity
import ch.heig.iict.polaris_health.data.entities.PatientEntity
import ch.heig.iict.polaris_health.data.entities.PoLTokenEntity
import ch.heig.iict.polaris_health.data.entities.VisitEntity

@Database(
    entities = [
        PatientEntity::class,
        MedicalRecordEntity::class,
        BeaconEntity::class,
        VisitEntity::class,
        PoLTokenEntity::class
    ],
    version = 1
)
@TypeConverters(Converters::class)
abstract class PolarisHealthDatabase : RoomDatabase() {

    abstract fun patientDao(): PatientDao
    abstract fun polTokenDao(): PoLTokenDao
    abstract fun visitDao(): VisitDao
    abstract fun beaconDao(): BeaconDao

    companion object{
        @Volatile
        private var INSTANCE : PolarisHealthDatabase? = null

        fun getDatabase(context: Context) : PolarisHealthDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PolarisHealthDatabase::class.java,
                    "polaris_health_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}