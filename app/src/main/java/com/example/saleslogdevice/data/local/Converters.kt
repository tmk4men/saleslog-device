package com.example.saleslogdevice.data.local

import androidx.room.TypeConverter
import com.example.saleslogdevice.domain.model.SessionStatus
import com.example.saleslogdevice.domain.model.SyncStatus
import com.example.saleslogdevice.domain.model.SyncTarget
import com.example.saleslogdevice.domain.model.VisitStatus

class Converters {
    @TypeConverter
    fun sessionStatusToString(value: SessionStatus): String = value.name

    @TypeConverter
    fun stringToSessionStatus(value: String): SessionStatus = SessionStatus.valueOf(value)

    @TypeConverter
    fun visitStatusToString(value: VisitStatus): String = value.name

    @TypeConverter
    fun stringToVisitStatus(value: String): VisitStatus = VisitStatus.valueOf(value)

    @TypeConverter
    fun syncTargetToString(value: SyncTarget): String = value.name

    @TypeConverter
    fun stringToSyncTarget(value: String): SyncTarget = SyncTarget.valueOf(value)

    @TypeConverter
    fun syncStatusToString(value: SyncStatus): String = value.name

    @TypeConverter
    fun stringToSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)
}
