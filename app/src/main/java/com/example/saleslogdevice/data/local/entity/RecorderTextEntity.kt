package com.example.saleslogdevice.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recorder_texts",
    indices = [Index("workSessionId"), Index("visitLogId")],
)
data class RecorderTextEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val workSessionId: Long,
    val visitLogId: Long? = null,
    val title: String? = null,
    val rawText: String,
    val formattedMinutes: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
