package com.moxmose.moxequiplog.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.moxmose.moxequiplog.utils.AppConstants
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
        MeasurementUnit::class,
        ReportFilter::class
    ], 
    version = 38,
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
    abstract fun reportFilterDao(): ReportFilterDao

    companion object {
        val MIGRATION_37_38 = object : Migration(37, 38) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE equipments ADD COLUMN isResettable INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE operation_types ADD COLUMN isSystem INTEGER NOT NULL DEFAULT 0")
                
                // Inserimento dell'operazione di sistema "Reset"
                db.execSQL(
                    "INSERT OR IGNORE INTO operation_types (id, description, dismissed, isSystem, displayOrder) VALUES (?, ?, ?, ?, ?)",
                    arrayOf(AppConstants.SYSTEM_OPERATION_RESET_ID, "Reset UdM", 0, 1, -1)
                )
            }
        }

        val CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.beginTransaction()
                try {
                    // Popolamento iniziale delle unità di misura
                    AppConstants.INITIAL_MEASUREMENT_UNITS.forEachIndexed { index, unit ->
                        db.execSQL(
                            "INSERT INTO measurement_units (id, label, description, isSystem, isHidden, displayOrder) VALUES (?, ?, ?, ?, ?, ?)",
                            arrayOf(unit.id, unit.label, unit.description, if (unit.isSystem) 1 else 0, if (unit.isHidden) 1 else 0, index)
                        )
                    }
                    
                    // Popolamento iniziale operazione di sistema
                    db.execSQL(
                        "INSERT OR IGNORE INTO operation_types (id, description, dismissed, isSystem, displayOrder) VALUES (?, ?, ?, ?, ?)",
                        arrayOf(AppConstants.SYSTEM_OPERATION_RESET_ID, "Reset UdM", 0, 1, -1)
                    )

                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }
        }
    }
}
