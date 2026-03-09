package com.moxmose.moxequiplog.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppPreferenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreference(preference: AppPreference)

    @Query("SELECT * FROM app_preferences WHERE `key` = :key")
    suspend fun getPreference(key: String): AppPreference?

    @Query("SELECT value FROM app_preferences WHERE `key` = :key")
    fun getPreferenceFlow(key: String): Flow<String?>

    @Query("SELECT * FROM app_preferences")
    fun getAllPreferences(): Flow<List<AppPreference>>
    
    @Query("DELETE FROM app_preferences WHERE `key` = :key")
    suspend fun deletePreference(key: String)
}
