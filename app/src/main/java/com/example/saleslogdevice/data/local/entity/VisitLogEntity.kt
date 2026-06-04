package com.example.saleslogdevice.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.saleslogdevice.domain.model.VisitStatus

@Entity(
    tableName = "visit_logs",
    foreignKeys = [
        ForeignKey(
            entity = WorkSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["workSessionId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("workSessionId")],
)
data class VisitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val workSessionId: Long,
    val clientName: String,
    val memo: String = "",
    val status: VisitStatus,
    val checkInTime: Long,
    val checkOutTime: Long? = null,
    val checkInLatitude: Double? = null,
    val checkInLongitude: Double? = null,
    val checkInAddress: String? = null,
    val checkOutLatitude: Double? = null,
    val checkOutLongitude: Double? = null,
    val checkOutAddress: String? = null,
    val durationMinutes: Int? = null,
    val recorderTextId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
