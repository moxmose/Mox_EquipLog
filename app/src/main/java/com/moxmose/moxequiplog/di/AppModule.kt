package com.moxmose.moxequiplog.di

import androidx.room.Room
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.ImageRepository
import com.moxmose.moxequiplog.data.local.AppDatabase
import com.moxmose.moxequiplog.ui.equipments.EquipmentsViewModel
import com.moxmose.moxequiplog.ui.maintenancelog.MaintenanceLogViewModel
import com.moxmose.moxequiplog.ui.operations.OperationsTypeViewModel
import com.moxmose.moxequiplog.ui.options.OptionsViewModel
import com.moxmose.moxequiplog.ui.reports.ReportsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {

    // Default Data from Resources
    single(named("defaultUsername")) { androidContext().resources.getString(R.string.default_username) }
    single(named("defaultColors")) { androidContext().resources.getStringArray(R.array.default_colors) }
    single(named("defaultCategories")) { androidContext().resources.getStringArray(R.array.default_categories) }

    // Database & DAOs
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "mox-maintenance-logs-db"
        )
        .addCallback(AppDatabase.CALLBACK)
        .fallbackToDestructiveMigration(true)
        .build()
    }

    single { get<AppDatabase>().equipmentDao() }
    single { get<AppDatabase>().operationTypeDao() }
    single { get<AppDatabase>().maintenanceLogDao() }
    single { get<AppDatabase>().imageDao() }
    single { get<AppDatabase>().categoryDao() }
    single { get<AppDatabase>().appColorDao() }
    single { get<AppDatabase>().appPreferenceDao() }
    single { get<AppDatabase>().measurementUnitDao() }

    // Repositories
    single { ImageRepository(get(), get(), get(), get(), get(named("defaultColors")), get(named("defaultCategories"))) }
    single { AppSettingsManager(get(), get(named("defaultUsername"))) }

    // ViewModels
    viewModelOf(::EquipmentsViewModel)
    viewModelOf(::OperationsTypeViewModel)
    viewModelOf(::MaintenanceLogViewModel)
    viewModelOf(::OptionsViewModel)
    viewModelOf(::ReportsViewModel)

}
