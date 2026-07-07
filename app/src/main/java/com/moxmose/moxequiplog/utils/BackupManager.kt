package com.moxmose.moxequiplog.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.AppDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
                return Result.failure(Exception(context.getString(R.string.backup_error_db_not_found)))
            }

            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                FileInputStream(dbFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return Result.failure(Exception(context.getString(R.string.backup_error_no_output_stream)))

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
            } ?: return Result.failure(Exception(context.getString(R.string.restore_error_no_input_stream)))

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
                "app_preferences", "app_colors", "categories", "images",
                "measurement_units", "sections", "operation_types", "equipments",
                "maintenance_logs", "maintenance_reminders", "report_filters"
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
            } ?: return Result.failure(Exception(context.getString(R.string.backup_error_no_output_stream)))

            tempDir.deleteRecursively()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun importAllFromZip(sourceUri: Uri): Result<Unit> {
        return try {
            val tempDir = File(context.cacheDir, "total_import_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                java.util.zip.ZipInputStream(inputStream).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        val outFile = File(tempDir, entry.name)
                        FileOutputStream(outFile).use { output ->
                            zipIn.copyTo(output)
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            } ?: return Result.failure(Exception(context.getString(R.string.restore_error_no_input_stream)))

            val tables = listOf(
                "app_preferences", "app_colors", "categories", "images",
                "measurement_units", "sections", "operation_types", "equipments",
                "maintenance_logs", "maintenance_reminders", "report_filters"
            )

            val db = database.openHelper.writableDatabase
            db.beginTransaction()
            try {
                // Clear in reverse order
                tables.reversed().forEach { tableName ->
                    db.execSQL("DELETE FROM `$tableName`")
                }

                tables.forEach { tableName ->
                    val csvFile = File(tempDir, "$tableName.csv")
                    if (csvFile.exists()) {
                        importTableFromCsv(tableName, csvFile)
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }

            tempDir.deleteRecursively()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun importTableFromCsv(tableName: String, csvFile: File) {
        val db = database.openHelper.writableDatabase
        csvFile.bufferedReader().use { reader ->
            val headerLine = reader.readLine() ?: return
            val columns = headerLine.split(";")
            
            var line = reader.readLine()
            while (line != null) {
                val values = parseCsvLine(line)
                if (values.size == columns.size) {
                    val sql = StringBuilder("INSERT INTO `$tableName` (")
                    sql.append(columns.joinToString(", ") { "`$it`" })
                    sql.append(") VALUES (")
                    sql.append(columns.indices.joinToString(", ") { "?" })
                    sql.append(")")
                    
                    val processedValues = values.map { if (it == "__NULL__") null else it }
                    db.execSQL(sql.toString(), processedValues.toTypedArray())
                }
                line = reader.readLine()
            }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    current.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ';' && !inQuotes) {
                result.add(current.toString())
                current = StringBuilder()
            } else {
                current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
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
                            Cursor.FIELD_TYPE_NULL -> "__NULL__"
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
