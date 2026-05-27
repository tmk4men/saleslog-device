package com.example.saleslogdevice.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.saleslogdevice.data.local.entity.RecorderTextEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecorderTextDao {

    @Query("SELECT * FROM recorder_texts WHERE workSessionId = :sessionId ORDER BY createdAt ASC")
    fun observeBySession(sessionId: Long): Flow<List<RecorderTextEntity>>

    @Query("SELECT * FROM recorder_texts WHERE workSessionId = :sessionId ORDER BY createdAt ASC")
    suspend fun getBySession(sessionId: Long): List<RecorderTextEntity>

    @Query("SELECT * FROM recorder_texts WHERE visitLogId = :visitId ORDER BY createdAt ASC")
    suspend fun getByVisit(visitId: Long): List<RecorderTextEntity>

    @Query("SELECT COUNT(*) FROM recorder_texts WHERE workSessionId = :sessionId")
    suspend fun countBySession(sessionId: Long): Int

    @Insert
    suspend fun insert(entity: RecorderTextEntity): Long

    @Update
    suspend fun update(entity: RecorderTextEntity)
}
