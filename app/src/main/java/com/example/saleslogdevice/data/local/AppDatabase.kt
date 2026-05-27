package com.example.saleslogdevice.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.saleslogdevice.data.local.dao.RecorderTextDao
import com.example.saleslogdevice.data.local.dao.SyncLogDao
import com.example.saleslogdevice.data.local.dao.UserSettingsDao
import com.example.saleslogdevice.data.local.dao.VisitLogDao
import com.example.saleslogdevice.data.local.dao.WorkSessionDao
import com.example.saleslogdevice.data.local.entity.RecorderTextEntity
import com.example.saleslogdevice.data.local.entity.SyncLogEntity
import com.example.saleslogdevice.data.local.entity.UserSettingsEntity
import com.example.saleslogdevice.data.local.entity.VisitLogEntity
import com.example.saleslogdevice.data.local.entity.WorkSessionEntity

@Database(
    entities = [
        UserSettingsEntity::class,
        WorkSessionEntity::class,
        VisitLogEntity::class,
        RecorderTextEntity::class,
        SyncLogEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun workSessionDao(): WorkSessionDao
    abstract fun visitLogDao(): VisitLogDao
    abstract fun recorderTextDao(): RecorderTextDao
    abstract fun syncLogDao(): SyncLogDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sales_log.db",
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
