package com.moxmose.moxequiplog.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: MaintenanceLog)

    @Update
    suspend fun updateLog(log: MaintenanceLog)

    @RawQuery(observedEntities = [MaintenanceLog::class, Equipment::class, OperationType::class])
    fun getLogsWithDetails(query: SupportSQLiteQuery): Flow<List<MaintenanceLogDetails>>
}
