package com.moxmose.moxequiplog.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Equipment::class, OperationType::class, MaintenanceLog::class, Media::class, Category::class, AppColor::class], version = 29, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun equipmentDao(): EquipmentDao
    abstract fun operationTypeDao(): OperationTypeDao
    abstract fun maintenanceLogDao(): MaintenanceLogDao
    abstract fun mediaDao(): MediaDao
    abstract fun categoryDao(): CategoryDao
    abstract fun appColorDao(): AppColorDao
}
