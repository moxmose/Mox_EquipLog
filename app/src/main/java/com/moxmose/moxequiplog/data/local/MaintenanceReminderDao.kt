package com.moxmose.moxequiplog.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceReminderDao {
    @Query("SELECT * FROM maintenance_reminders ORDER BY dueDate ASC")
    fun getAllReminders(): Flow<List<MaintenanceReminder>>

    @Query("""
        SELECT 
            r.*, 
            e.description as equipmentDescription, 
            ot.description as operationTypeDescription,
            e.photoUri as equipmentPhotoUri,
            e.iconIdentifier as equipmentIconIdentifier,
            ot.photoUri as operationTypePhotoUri,
            ot.iconIdentifier as operationTypeIconIdentifier,
            e.dismissed as equipmentDismissed,
            ot.dismissed as operationTypeDismissed,
            e.unitId as unitId
        FROM maintenance_reminders r
        JOIN equipments e ON r.equipmentId = e.id
        JOIN operation_types ot ON r.operationTypeId = ot.id
        WHERE r.isCompleted = 0
        ORDER BY COALESCE(r.dueDate, r.presumedDate) ASC
    """)
    fun getActiveRemindersWithDetails(): Flow<List<MaintenanceReminderDetails>>

    @Query("SELECT * FROM maintenance_reminders WHERE id = :id")
    suspend fun getReminderById(id: Int): MaintenanceReminder?

    @Query("SELECT * FROM maintenance_reminders WHERE equipmentId = :equipmentId")
    fun getRemindersForEquipment(equipmentId: Int): Flow<List<MaintenanceReminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: MaintenanceReminder): Long

    @Update
    suspend fun updateReminder(reminder: MaintenanceReminder)

    @Delete
    suspend fun deleteReminder(reminder: MaintenanceReminder)

    @Query("UPDATE maintenance_reminders SET isCompleted = :completed WHERE id = :id")
    suspend fun updateCompletionStatus(id: Int, completed: Boolean)

    @Query("SELECT * FROM maintenance_reminders WHERE equipmentId = :equipmentId AND operationTypeId = :operationTypeId AND isCompleted = 0 LIMIT 1")
    suspend fun getReminderByEquipmentAndOperation(equipmentId: Int, operationTypeId: Int): MaintenanceReminder?
}
