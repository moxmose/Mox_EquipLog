package com.moxmose.moxequiplog.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSection(section: Section): Long

    @Update
    suspend fun updateSection(section: Section)

    @Update
    suspend fun updateSectionList(sectionList: List<Section>)

    @Delete
    suspend fun deleteSection(section: Section)

    @Query("SELECT * FROM sections ORDER BY displayOrder ASC")
    fun getAllSections(): Flow<List<Section>>

    @Query("SELECT * FROM sections WHERE id = :id")
    suspend fun getSectionById(id: Int): Section?

    @Query("SELECT COUNT(*) FROM sections")
    suspend fun getSectionsCount(): Int

    @Query("SELECT * FROM sections WHERE name LIKE '%(Demo)%'")
    suspend fun getDemoSections(): List<Section>

    @Delete
    suspend fun deleteSections(sections: List<Section>)
}
