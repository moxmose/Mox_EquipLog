package com.moxmose.moxequiplog.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media ORDER BY displayOrder ASC")
    fun getAllMedia(): Flow<List<Media>>

    @Query("SELECT * FROM media WHERE category = :category ORDER BY displayOrder ASC")
    fun getMediaByCategory(category: String): Flow<List<Media>>

    @Query("SELECT * FROM media WHERE uri = :uri AND category = :category")
    suspend fun getMediaByUriAndCategory(uri: String, category: String): Media?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: Media)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMedia(media: List<Media>)

    @Update
    suspend fun updateAllMedia(media: List<Media>)

    @Delete
    suspend fun deleteMedia(media: Media)

    @Query("SELECT MAX(displayOrder) FROM media WHERE category = :category")
    suspend fun getMaxOrder(category: String): Int?

    @Query("UPDATE media SET hidden = NOT hidden WHERE uri = :uri AND category = :category")
    suspend fun toggleHidden(uri: String, category: String)
}
