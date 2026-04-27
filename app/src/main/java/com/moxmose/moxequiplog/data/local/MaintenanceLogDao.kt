package com.moxmose.moxequiplog.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Query("SELECT * FROM maintenance_logs WHERE equipmentId = :equipmentId AND date < :date ORDER BY date DESC LIMIT 1")
    suspend fun getLastLogBefore(equipmentId: Int, date: Long): MaintenanceLog?

    @Query("SELECT * FROM maintenance_logs WHERE equipmentId = :equipmentId AND date >= :sinceDate ORDER BY date ASC")
    suspend fun getLogsSince(equipmentId: Int, sinceDate: Long): List<MaintenanceLog>

    @Query("SELECT * FROM maintenance_logs WHERE equipmentId = :equipmentId AND operationTypeId = :operationTypeId ORDER BY date DESC LIMIT 1")
    suspend fun getLastLogForEquipmentAndOperation(equipmentId: Int, operationTypeId: Int): MaintenanceLog?

    @Query("SELECT * FROM maintenance_logs WHERE equipmentId = :equipmentId AND value IS NOT NULL ORDER BY date DESC LIMIT 1")
    suspend fun getLastValueLogForEquipment(equipmentId: Int): MaintenanceLog?
}
