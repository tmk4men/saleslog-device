package com.example.saleslogdevice.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.example.saleslogdevice.domain.model.LatLng
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

/**
 * チェックポイント方式の位置情報取得。常時監視はしない。
 *  - 営業開始時
 *  - チェックイン時
 *  - チェックアウト時
 *  - 営業終了時
 * のみ getCurrent() を呼ぶ前提。失敗・権限なし・GPS OFF などは null を返す。
 */
class LocationProvider(private val context: Context) {

    private val fused by lazy { LocationServices.getFusedLocationProviderClient(context) }

    fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun isLocationEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /** 現在地を取得。権限なし/GPS OFF/失敗 はすべて null。 */
    suspend fun getCurrent(): LatLng? {
        if (!hasPermission() || !isLocationEnabled()) return null
        return runCatching {
            val location = fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
            location?.let { LatLng(it.latitude, it.longitude) }
        }.getOrNull()
    }
}
