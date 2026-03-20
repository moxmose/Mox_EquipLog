package com.moxmose.moxequiplog.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Equipment::class, 
        OperationType::class, 
        MaintenanceLog::class, 
        Image::class, 
        Category::class, 
        AppColor::class, 
        AppPreference::class,
        MeasurementUnit::class
    ], 
    version = 32, 
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun equipmentDao(): EquipmentDao
    abstract fun operationTypeDao(): OperationTypeDao
    abstract fun maintenanceLogDao(): MaintenanceLogDao
    abstract fun imageDao(): ImageDao
    abstract fun categoryDao(): CategoryDao
    abstract fun appColorDao(): AppColorDao
    abstract fun appPreferenceDao(): AppPreferenceDao
    abstract fun measurementUnitDao(): MeasurementUnitDao

    companion object {
        val CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Popolamento iniziale delle unità di misura
                CoroutineScope(Dispatchers.IO).launch {
                    db.execSQL("INSERT INTO measurement_units (id, label, description, isSystem) VALUES (1, 'km', 'Kilometers', 1)")
                    db.execSQL("INSERT INTO measurement_units (id, label, description, isSystem) VALUES (2, 'hh', 'Hours', 1)")
                    db.execSQL("INSERT INTO measurement_units (id, label, description, isSystem) VALUES (3, 'lt', 'Liters', 1)")
                    db.execSQL("INSERT INTO measurement_units (id, label, description, isSystem) VALUES (4, 'dy', 'Days', 1)")
                    db.execSQL("INSERT INTO measurement_units (id, label, description, isSystem) VALUES (5, 'un', 'Units', 1)")
                }
            }
        }
    }
}
