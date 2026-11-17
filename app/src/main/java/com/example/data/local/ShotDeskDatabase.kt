package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProjectEntity::class, ShotEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ShotDeskDatabase : RoomDatabase() {
    abstract fun productionDao(): ProductionDao

    companion object {
        const val DATABASE_NAME = "shotdesk_production.db"

        @Volatile
        private var INSTANCE: ShotDeskDatabase? = null

        fun getInstance(context: Context): ShotDeskDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ShotDeskDatabase::class.java,
                    DATABASE_NAME,
                ).build().also { INSTANCE = it }
            }
        }
    }
}
