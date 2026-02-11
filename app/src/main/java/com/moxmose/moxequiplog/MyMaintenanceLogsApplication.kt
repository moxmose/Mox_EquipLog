package com.moxmose.moxequiplog

import android.app.Application
import com.moxmose.moxequiplog.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MyMaintenanceLogsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MyMaintenanceLogsApplication)
            modules(appModule)
        }
    }
}
