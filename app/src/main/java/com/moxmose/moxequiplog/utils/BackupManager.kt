package com.moxmose.moxequiplog.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.moxmose.moxequiplog.data.local.AppDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupManager(private val context: Context, private val database: AppDatabase) {

    companion object {
        private const val DATABASE_NAME = "mox_equiplog.db"
    }

    fun backupDatabase(destinationUri: Uri): Result<Unit> {
        return try {
            // Force checkpoint to ensure all data is in the main .db file
            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { cursor ->
                cursor.moveToFirst()
            }

            val dbFile = context.getDatabasePath(DATABASE_NAME)
            if (!dbFile.exists()) {
                return Result.failure(Exception("Database file not found"))
            }

            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                FileInputStream(dbFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return Result.failure(Exception("Failed to open output stream"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun restoreDatabase(sourceUri: Uri): Result<Unit> {
        return try {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            
            // Close database before replacing the file
            database.close()

            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(dbFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return Result.failure(Exception("Failed to open input stream"))

            // Clean up WAL and SHM files to ensure the new database is loaded correctly
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getSuggestedBackupFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "MoxEquipLog_Backup_$timestamp.db"
    }

    fun exportAllToZip(destinationUri: Uri): Result<Unit> {
        return try {
            val tempDir = File(context.cacheDir, "total_export_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            val tables = listOf(
                "equipment", "operation_type", "maintenance_log",
                "image", "category", "app_color",
                "app_preference", "measurement_unit", "report_filter"
            )

            tables.forEach { tableName ->
                val csvFile = File(tempDir, "$tableName.csv")
                exportTableToCsv(tableName, csvFile)
            }

            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zipOut ->
                    tempDir.listFiles()?.forEach { file ->
                        val zipEntry = ZipEntry(file.name)
                        zipOut.putNextEntry(zipEntry)
                        FileInputStream(file).use { input ->
                            input.copyTo(zipOut)
                        }
                        zipOut.closeEntry()
                    }
                }
            } ?: return Result.failure(Exception("Failed to open output stream"))

            tempDir.deleteRecursively()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun exportTableToCsv(tableName: String, outputFile: File) {
        val db = database.openHelper.readableDatabase
        db.query("SELECT * FROM $tableName", arrayOf()).use { cursor ->
            outputFile.outputStream().bufferedWriter().use { writer ->
                val columnNames = cursor.columnNames
                writer.write(columnNames.joinToString(";") + "\n")

                while (cursor.moveToNext()) {
                    val row = (0 until cursor.columnCount).map { i ->
                        when (cursor.getType(i)) {
                            Cursor.FIELD_TYPE_NULL -> ""
                            Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i).toString()
                            Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(i).toString()
                            Cursor.FIELD_TYPE_STRING -> {
                                val value = cursor.getString(i)
                                if (value.contains(";") || value.contains("\n") || value.contains("\"")) {
                                    "\"${value.replace("\"", "\"\"")}\""
                                } else {
                                    value
                                }
                            }
                            Cursor.FIELD_TYPE_BLOB -> "[BLOB]"
                            else -> ""
                        }
                    }
                    writer.write(row.joinToString(";") + "\n")
                }
            }
        }
    }

    fun getSuggestedTotalExportFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "MoxEquipLog_TotalExport_$timestamp.zip"
    }
}
