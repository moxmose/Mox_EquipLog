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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<MaintenanceLog>)

    @Update
    suspend fun updateLog(log: MaintenanceLog)

    @RawQuery(observedEntities = [MaintenanceLog::class, Equipment::class, OperationType::class])
    fun getLogsWithDetails(query: SupportSQLiteQuery): Flow<List<MaintenanceLogDetails>>

    @Query("SELECT * FROM maintenance_logs WHERE equipmentId = :equipmentId AND date < :date ORDER BY date DESC LIMIT 1")
    suspend fun getLastLogBefore(equipmentId: Int, date: Long): MaintenanceLog?

    @Query("""
        SELECT l.* FROM maintenance_logs l
        JOIN operation_types ot ON l.operationTypeId = ot.id
        JOIN equipments e ON l.equipmentId = e.id
        WHERE l.equipmentId = :equipmentId 
        AND l.date >= :sinceDate 
        AND l.value IS NOT NULL
        AND ot.unitId = e.unitId
        ORDER BY l.date ASC
    """)
    suspend fun getValueLogsSince(equipmentId: Int, sinceDate: Long): List<MaintenanceLog>

    @Query("SELECT * FROM maintenance_logs WHERE equipmentId = :equipmentId AND operationTypeId = :operationTypeId ORDER BY date DESC LIMIT 1")
    suspend fun getLastLogForEquipmentAndOperation(equipmentId: Int, operationTypeId: Int): MaintenanceLog?

    @Query("""
        SELECT l.* FROM maintenance_logs l
        JOIN operation_types ot ON l.operationTypeId = ot.id
        JOIN equipments e ON l.equipmentId = e.id
        WHERE l.equipmentId = :equipmentId 
        AND l.value IS NOT NULL 
        AND ot.unitId = e.unitId
        ORDER BY l.date DESC LIMIT 1
    """)
    suspend fun getLastValueLogForEquipment(equipmentId: Int): MaintenanceLog?

    @Query("SELECT * FROM maintenance_logs WHERE equipmentId = :equipmentId ORDER BY date ASC, timestamp ASC")
    suspend fun getAllLogsForEquipment(equipmentId: Int): List<MaintenanceLog>

    @Query("""
        SELECT l.*, ot.unitId as operationTypeUnitId 
        FROM maintenance_logs l 
        JOIN operation_types ot ON l.operationTypeId = ot.id 
        WHERE l.equipmentId = :equipmentId 
        ORDER BY l.date ASC, l.timestamp ASC
    """)
    suspend fun getAllLogsForEquipmentWithUnit(equipmentId: Int): List<MaintenanceLogWithUnit>

    @Query("SELECT SUM(cost) FROM maintenance_logs WHERE equipmentId = :equipmentId AND date >= :sinceDate")
    suspend fun getTotalCostForEquipmentSince(equipmentId: Int, sinceDate: Long): Double?

    @Query("SELECT * FROM maintenance_logs WHERE operationTypeId = :operationTypeId AND cost IS NOT NULL ORDER BY date DESC LIMIT 1")
    suspend fun getLastLogWithCostForOperation(operationTypeId: Int): MaintenanceLog?

    @Query("SELECT AVG(cost) FROM maintenance_logs WHERE operationTypeId = :operationTypeId AND date >= :sinceDate")
    suspend fun getAverageCostForOperationSince(operationTypeId: Int, sinceDate: Long): Double?

    @Query("SELECT COUNT(*) FROM maintenance_logs")
    fun getLogsCountFlow(): Flow<Int>

    @Update
    suspend fun updateLogs(logs: List<MaintenanceLog>)

    @androidx.room.Delete
    suspend fun deleteLog(log: MaintenanceLog)
}
