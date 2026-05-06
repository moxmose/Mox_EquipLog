package com.moxmose.moxequiplog.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementUnitDao {
    @Query("SELECT * FROM measurement_units ORDER BY displayOrder ASC")
    fun getAllUnits(): Flow<List<MeasurementUnit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnit(unit: MeasurementUnit)

    @Update
    suspend fun updateUnit(unit: MeasurementUnit)

    @Delete
    suspend fun deleteUnit(unit: MeasurementUnit)

    @Query("SELECT * FROM measurement_units WHERE id = :id")
    suspend fun getUnitById(id: Int): MeasurementUnit?

    @Query("SELECT COUNT(*) FROM measurement_units")
    suspend fun countUnits(): Int
}
