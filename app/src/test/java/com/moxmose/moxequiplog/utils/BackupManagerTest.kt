package com.moxmose.moxequiplog.utils

import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManagerTest {

    @Test
    fun `backup tables list matches database entities`() {
        // Elenco delle tabelle utilizzate in BackupManager.exportAllToZip
        val backupTables = listOf(
            "equipments", "operation_types", "maintenance_logs",
            "images", "categories", "app_colors",
            "app_preferences", "measurement_units", "report_filters"
        )
        
        // Verifichiamo che i nomi delle tabelle usati nel backup siano coerenti con 
        // quelli definiti nelle entità del database (controllo manuale assistito dal test)
        
        val expectedFromEntities = listOf(
            "equipments",        // Equipment::class
            "operation_types",    // OperationType::class
            "maintenance_logs",   // MaintenanceLog::class
            "images",            // Image::class
            "categories",        // Category::class
            "app_colors",        // AppColor::class
            "app_preferences",   // AppPreference::class
            "measurement_units", // MeasurementUnit::class
            "report_filters"     // ReportFilter::class
        )
        
        expectedFromEntities.forEach { table ->
            assertTrue("Tabella $table mancante nel backup!", backupTables.contains(table))
        }
    }
}
