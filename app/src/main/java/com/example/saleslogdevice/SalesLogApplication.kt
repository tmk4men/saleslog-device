package com.example.saleslogdevice

import android.app.Application
import com.example.saleslogdevice.di.ServiceLocator

class SalesLogApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.get(this)
    }
}
