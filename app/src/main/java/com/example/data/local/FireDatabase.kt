package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.IncidentEntity

@Database(
    entities = [HotspotEntity::class, AuditLogEntity::class, IncidentEntity::class],
    version = 2,
    exportSchema = false
)
abstract class FireDatabase : RoomDatabase() {

    abstract fun hotspotDao(): HotspotDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun incidentDao(): IncidentDao

    companion object {
        @Volatile
        private var INSTANCE: FireDatabase? = null

        fun getInstance(context: Context): FireDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FireDatabase::class.java,
                    "hardi_mantangai_fire.db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
