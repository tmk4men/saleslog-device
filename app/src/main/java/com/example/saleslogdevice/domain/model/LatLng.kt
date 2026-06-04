package com.example.saleslogdevice.domain.model

data class LatLng(
    val latitude: Double,
    val longitude: Double,
    /** 逆ジオコーディングで解決した住所。取得できなければ null。 */
    val address: String? = null,
)
