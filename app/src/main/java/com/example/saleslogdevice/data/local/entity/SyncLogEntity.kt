package com.example.saleslogdevice.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.saleslogdevice.domain.model.SyncStatus
import com.example.saleslogdevice.domain.model.SyncTarget

@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val targetType: SyncTarget,
    val targetId: Long,
    val status: SyncStatus,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
