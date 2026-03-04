package com.moxmose.moxequiplog.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Equipment::class, OperationType::class, MaintenanceLog::class, Image::class, Category::class, AppColor::class, AppPreference::class], version = 31, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun equipmentDao(): EquipmentDao
    abstract fun operationTypeDao(): OperationTypeDao
    abstract fun maintenanceLogDao(): MaintenanceLogDao
    abstract fun imageDao(): ImageDao
    abstract fun categoryDao(): CategoryDao
    abstract fun appColorDao(): AppColorDao
    abstract fun appPreferenceDao(): AppPreferenceDao
}
