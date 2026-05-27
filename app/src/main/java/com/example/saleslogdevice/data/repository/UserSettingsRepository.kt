package com.example.saleslogdevice.data.repository

import com.example.saleslogdevice.data.local.dao.UserSettingsDao
import com.example.saleslogdevice.data.local.entity.UserSettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserSettingsRepository(private val dao: UserSettingsDao) {

    fun observe(): Flow<UserSettingsEntity> =
        dao.observe().map { it ?: UserSettingsEntity() }

    suspend fun get(): UserSettingsEntity = dao.get() ?: UserSettingsEntity()

    suspend fun save(settings: UserSettingsEntity) {
        dao.upsert(settings.copy(updatedAt = System.currentTimeMillis()))
    }
}
