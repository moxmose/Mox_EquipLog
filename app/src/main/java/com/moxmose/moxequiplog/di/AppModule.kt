package com.moxmose.moxequiplog.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.MediaRepository
import com.moxmose.moxequiplog.data.local.AppDatabase
import com.moxmose.moxequiplog.ui.equipments.EquipmentsViewModel
import com.moxmose.moxequiplog.ui.maintenancelog.MaintenanceLogViewModel
import com.moxmose.moxequiplog.ui.operations.OperationTypeViewModel
import com.moxmose.moxequiplog.ui.options.OptionsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

val appModule = module {

    // Default Data from Resources
    single(named("defaultUsername")) { androidContext().resources.getString(R.string.default_username) }
    single(named("defaultColors")) { androidContext().resources.getStringArray(R.array.default_colors) }
    single(named("defaultCategories")) { androidContext().resources.getStringArray(R.array.default_categories) }

    // DataStore
    single { androidContext().dataStore }

    // Database & DAOs
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "mox-maintenance-logs-db"
        )
        .fallbackToDestructiveMigration(true)
        .build()
    }

    single { get<AppDatabase>().equipmentDao() }
    single { get<AppDatabase>().operationTypeDao() }
    single { get<AppDatabase>().maintenanceLogDao() }
    single { get<AppDatabase>().mediaDao() }
    single { get<AppDatabase>().categoryDao() }
    single { get<AppDatabase>().appColorDao() }

    // Repositories
    single { MediaRepository(get(), get(), get(), get(named("defaultColors")), get(named("defaultCategories"))) }
    single { AppSettingsManager(get(), get(named("defaultUsername"))) }

    // ViewModels
    viewModelOf(::EquipmentsViewModel)
    viewModelOf(::OperationTypeViewModel)
    viewModelOf(::MaintenanceLogViewModel)
    viewModelOf(::OptionsViewModel)

}
