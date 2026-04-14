package com.moxmose.moxequiplog.utils

import android.content.Context
import android.net.Uri
import com.moxmose.moxequiplog.data.local.AppDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

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
}
