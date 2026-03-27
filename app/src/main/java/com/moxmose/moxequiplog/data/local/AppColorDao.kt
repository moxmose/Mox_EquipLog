package com.moxmose.moxequiplog.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppColorDao {
    @Query("SELECT * FROM app_colors ORDER BY displayOrder ASC")
    fun getAllColors(): Flow<List<AppColor>>

    @Query("SELECT * FROM app_colors ORDER BY reportOrder ASC")
    fun getAllColorsForReports(): Flow<List<AppColor>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertColor(color: AppColor)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllColors(colors: List<AppColor>)

    @Update
    suspend fun updateColor(color: AppColor)

    @Update
    suspend fun updateAllColors(colors: List<AppColor>)

    @Delete
    suspend fun deleteColor(color: AppColor)

    @Query("SELECT MAX(displayOrder) FROM app_colors")
    suspend fun getMaxOrder(): Int?

    @Query("SELECT MAX(reportOrder) FROM app_colors")
    suspend fun getMaxReportOrder(): Int?

    @Query("UPDATE app_colors SET hidden = NOT hidden WHERE id = :id")
    suspend fun toggleHidden(id: Long)

    @Query("UPDATE app_colors SET reportHidden = NOT reportHidden WHERE id = :id")
    suspend fun toggleReportHidden(id: Long)
}
