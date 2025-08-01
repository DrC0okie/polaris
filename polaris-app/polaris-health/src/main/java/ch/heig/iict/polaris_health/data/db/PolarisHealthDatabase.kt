package ch.heig.iict.polaris_health.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ch.heig.iict.polaris_health.data.dao.PatientDao
import ch.heig.iict.polaris_health.data.dao.PoLTokenDao
import ch.heig.iict.polaris_health.data.model.PatientEntity
import ch.heig.iict.polaris_health.data.model.PoLTokenEntity

@Database(entities = [PatientEntity::class, PoLTokenEntity::class], version = 1)
abstract class PolarisHealthDatabase : RoomDatabase() {

    abstract fun patientDao(): PatientDao
    abstract fun polTokenDao(): PoLTokenDao

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