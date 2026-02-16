package com.moxmose.moxequiplog.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {
    @Query("SELECT * FROM images ORDER BY displayOrder ASC")
    fun getAllImages(): Flow<List<Image>>

    @Query("SELECT * FROM images WHERE category = :category ORDER BY displayOrder ASC")
    fun getImagesByCategory(category: String): Flow<List<Image>>

    @Query("SELECT * FROM images WHERE uri = :uri AND category = :category")
    suspend fun getImageByUriAndCategory(uri: String, category: String): Image?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: Image)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllImages(images: List<Image>)

    @Update
    suspend fun updateAllImages(images: List<Image>)

    @Delete
    suspend fun deleteImage(image: Image)

    @Query("SELECT MAX(displayOrder) FROM images WHERE category = :category")
    suspend fun getMaxOrder(category: String): Int?

    @Query("UPDATE images SET hidden = NOT hidden WHERE uri = :uri AND category = :category")
    suspend fun toggleHidden(uri: String, category: String)
}
