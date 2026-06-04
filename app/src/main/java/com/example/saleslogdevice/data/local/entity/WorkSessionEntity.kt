package com.example.saleslogdevice.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.saleslogdevice.domain.model.SessionStatus

@Entity(
    tableName = "work_sessions",
    indices = [Index(value = ["date"], unique = true)],
)
data class WorkSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: String,
    val userName: String,
    val status: SessionStatus,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val startLatitude: Double? = null,
    val startLongitude: Double? = null,
    val startAddress: String? = null,
    val endLatitude: Double? = null,
    val endLongitude: Double? = null,
    val endAddress: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncedToDrive: Boolean = false,
    val syncedToSheets: Boolean = false,
)
