package com.moxmose.moxequiplog.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class BackupManager(private val context: Context) {

    companion object {
        private const val DATABASE_NAME = "mox_equiplog.db"
    }

    fun backupDatabase(destinationUri: Uri): Result<Unit> {
        return try {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            if (!dbFile.exists()) {
                return Result.failure(Exception("Database file not found"))
            }

            // Close database before copying is recommended, 
            // but for a simple copy of the file we might need to handle -wal and -shm files too.
            val walFile = File(dbFile.path + "-wal")
            val shmFile = File(dbFile.path + "-shm")

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
