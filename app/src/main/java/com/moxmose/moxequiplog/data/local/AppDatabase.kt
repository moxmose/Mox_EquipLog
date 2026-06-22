package com.moxmose.moxequiplog.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.moxmose.moxequiplog.utils.AppConstants

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
        ReportFilter::class,
        MaintenanceReminder::class,
        Section::class
    ], 
    version = 2,
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
    abstract fun maintenanceReminderDao(): MaintenanceReminderDao
    abstract fun sectionDao(): SectionDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Crea la tabella sections
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sections` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `iconIdentifier` TEXT, 
                        `photoUri` TEXT,
                        `color` TEXT, 
                        `displayOrder` INTEGER NOT NULL,
                        `dismissed` INTEGER NOT NULL DEFAULT 0,
                        `defaultUnitId` INTEGER NOT NULL DEFAULT 1,
                        `defaultUsageWindow` INTEGER NOT NULL DEFAULT 30,
                        `defaultUsageWindowUnit` TEXT NOT NULL DEFAULT 'DAYS',
                        `defaultVisibilityHorizon` INTEGER NOT NULL DEFAULT 30,
                        `defaultVisibilityHorizonUnit` TEXT NOT NULL DEFAULT 'DAYS',
                        `defaultEquipmentId` INTEGER,
                        `defaultOperationTypeId` INTEGER
                    )
                """.trimIndent())

                // 2. Inserisce la sezione di default
                db.execSQL(
                    "INSERT INTO sections (id, name, iconIdentifier, photoUri, color, displayOrder, dismissed, defaultUnitId, defaultUsageWindow, defaultUsageWindowUnit, defaultVisibilityHorizon, defaultVisibilityHorizonUnit, defaultEquipmentId, defaultOperationTypeId) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    arrayOf(
                        AppConstants.DEFAULT_SECTION_ID, 
                        AppConstants.DEFAULT_SECTION_NAME, 
                        AppConstants.DEFAULT_SECTION_ICON, 
                        null, 
                        AppConstants.DEFAULT_SECTION_COLOR, 
                        0,
                        0,
                        1,
                        30,
                        "DAYS",
                        30,
                        "DAYS",
                        null,
                        null
                    )
                )

                // 3. Aggiunge sectionId a equipments (nullable temporaneamente o con default)
                db.execSQL("ALTER TABLE equipments ADD COLUMN sectionId INTEGER NOT NULL DEFAULT ${AppConstants.DEFAULT_SECTION_ID}")
                
                // 4. Aggiunge sectionId a operation_types
                db.execSQL("ALTER TABLE operation_types ADD COLUMN sectionId INTEGER NOT NULL DEFAULT ${AppConstants.DEFAULT_SECTION_ID}")
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
                            "INSERT INTO measurement_units (id, label, description, isSystem, isHidden, displayOrder, decimalPlaces) VALUES (?, ?, ?, ?, ?, ?, ?)",
                            arrayOf(unit.id, unit.label, unit.description, if (unit.isSystem) 1 else 0, if (unit.isHidden) 1 else 0, index, unit.decimalPlaces)
                        )
                    }
                    
                    // Popolamento iniziale operazione di sistema (Reset)
                    db.execSQL(
                        "INSERT OR IGNORE INTO operation_types (id, description, dismissed, isSystem, displayOrder) VALUES (?, ?, ?, ?, ?)",
                        arrayOf(AppConstants.SYSTEM_OPERATION_RESET_ID, "Reset UoM", 0, 1, -1)
                    )

                    // Popolamento iniziale Sezione di default
                    db.execSQL(
                        "INSERT INTO sections (id, name, iconIdentifier, photoUri, color, displayOrder, dismissed, defaultUnitId, defaultUsageWindow, defaultUsageWindowUnit, defaultVisibilityHorizon, defaultVisibilityHorizonUnit, defaultEquipmentId, defaultOperationTypeId) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        arrayOf(
                            AppConstants.DEFAULT_SECTION_ID, 
                            AppConstants.DEFAULT_SECTION_NAME, 
                            AppConstants.DEFAULT_SECTION_ICON, 
                            null, 
                            AppConstants.DEFAULT_SECTION_COLOR, 
                            0,
                            0,
                            1,
                            30,
                            "DAYS",
                            30,
                            "DAYS",
                            null,
                            null
                        )
                    )

                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }
        }
    }
}
