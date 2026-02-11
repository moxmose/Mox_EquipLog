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
    suspend fun insertEquipment(equipment: Equipment)

    @Update
    suspend fun updateEquipment(equipment: Equipment)

    @Update
    suspend fun updateEquipments(equipments: List<Equipment>)

    @Query("SELECT * FROM equipments WHERE dismissed = 0 ORDER BY displayOrder ASC")
    fun getActiveEquipments(): Flow<List<Equipment>>

    @Query("SELECT * FROM equipments ORDER BY displayOrder ASC")
    fun getAllEquipments(): Flow<List<Equipment>>

    @Query("SELECT * FROM equipments WHERE id = :equipmentId")
    fun getEquipmentById(equipmentId: Int): Flow<Equipment?>

    @Query("SELECT COUNT(*) FROM equipments WHERE photoUri = :uri")
    suspend fun countEquipmentsUsingPhoto(uri: String): Int

    @Query("SELECT DISTINCT photoUri FROM equipments WHERE photoUri IS NOT NULL")
    fun getAllUsedPhotos(): Flow<List<String>>
}
