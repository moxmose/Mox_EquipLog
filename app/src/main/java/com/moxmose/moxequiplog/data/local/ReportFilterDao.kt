package com.moxmose.moxequiplog.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportFilterDao {
    @Query("SELECT * FROM report_filters WHERE reportType = :type AND isLastSession = 1 LIMIT 1")
    fun getLastSession(type: String): Flow<ReportFilter?>

    @Query("SELECT * FROM report_filters WHERE reportType = :type AND isLastSession = 0 ORDER BY timestamp DESC")
    fun getSavedFilters(type: String): Flow<List<ReportFilter>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFilter(filter: ReportFilter)

    @Query("DELETE FROM report_filters WHERE id = :id")
    suspend fun deleteFilter(id: Int)

    @Query("DELETE FROM report_filters WHERE reportType = :type AND isLastSession = 1")
    suspend fun deleteLastSession(type: String)

    @Transaction
    suspend fun updateLastSession(filter: ReportFilter) {
        deleteLastSession(filter.reportType)
        insertFilter(filter.copy(isLastSession = true))
    }
}
