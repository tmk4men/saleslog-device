package com.example.saleslogdevice.domain.model

enum class SyncTarget {
    DRIVE,
    SHEETS,
}

enum class SyncStatus {
    PENDING,
    SUCCESS,
    FAILED,
}
