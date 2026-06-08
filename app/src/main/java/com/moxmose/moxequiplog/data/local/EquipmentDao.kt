package com.moxmose.moxequiplog.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipment(equipment: Equipment): Long

    @Update
    suspend fun updateEquipment(equipment: Equipment)

    @Update
    suspend fun updateEquipmentList(equipmentList: List<Equipment>)

    @Query("SELECT * FROM equipments WHERE dismissed = 0 AND sectionId = :sectionId ORDER BY displayOrder ASC")
    fun getActiveEquipmentListBySection(sectionId: Int): Flow<List<Equipment>>

    @Query("SELECT * FROM equipments WHERE sectionId = :sectionId ORDER BY displayOrder ASC")
    fun getAllEquipmentListBySection(sectionId: Int): Flow<List<Equipment>>

    @Query("SELECT * FROM equipments WHERE dismissed = 0 ORDER BY displayOrder ASC")
    fun getActiveEquipmentList(): Flow<List<Equipment>>

    @Query("SELECT * FROM equipments ORDER BY displayOrder ASC")
    fun getAllEquipmentList(): Flow<List<Equipment>>

    @Query("SELECT * FROM equipments WHERE id = :equipmentId")
    fun getEquipmentById(equipmentId: Int): Flow<Equipment?>

    @Query("SELECT * FROM equipments WHERE id = :id")
    suspend fun getEquipmentByIdOneShot(id: Int): Equipment?

    @Query("SELECT COUNT(*) FROM equipments WHERE photoUri = :uri")
    suspend fun countEquipmentUsingPhoto(uri: String): Int

    @Query("SELECT DISTINCT photoUri FROM equipments WHERE photoUri IS NOT NULL")
    fun getAllUsedPhotos(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM equipments WHERE isResettable = 1 AND dismissed = 0")
    fun countActiveResettableEquipment(): Flow<Int>

    @Query("SELECT COUNT(*) FROM equipments WHERE isResettable = 1 AND dismissed = 0 AND sectionId = :sectionId")
    fun countActiveResettableEquipmentBySection(sectionId: Int): Flow<Int>

    @Query("SELECT * FROM equipments WHERE description LIKE '%(Demo)%'")
    suspend fun getDemoEquipmentList(): List<Equipment>

    @Query("SELECT COUNT(*) FROM equipments WHERE unitId = :unitId")
    fun countEquipmentsByUnit(unitId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM equipments WHERE sectionId = :sectionId")
    fun countEquipmentsBySection(sectionId: Int): Flow<Int>

    @androidx.room.Delete
    suspend fun deleteEquipmentList(equipmentList: List<Equipment>)
}
