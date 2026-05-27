package com.example.saleslogdevice.data.local

import androidx.room.withTransaction

/** UseCase が atomically 実行したい複合操作を包むためのトランザクション境界。 */
class Transactor(private val db: AppDatabase) {
    suspend fun <T> run(block: suspend () -> T): T = db.withTransaction(block)
}
