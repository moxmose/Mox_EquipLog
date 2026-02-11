package com.moxmose.moxequiplog.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AppSettingsManager(
    private val dataStore: DataStore<Preferences>,
    private val defaultUsername: String
) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val usernameKey = stringPreferencesKey("username")
    private val favoriteIconKey = stringPreferencesKey("favorite_icon")
    private val favoritePhotoUriKey = stringPreferencesKey("favorite_photo_uri")

    val username: StateFlow<String> = dataStore.data
        .map { preferences ->
            val savedUsername = preferences[usernameKey]
            if (savedUsername.isNullOrBlank()) defaultUsername else savedUsername
        }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(5000), defaultUsername)

    val favoriteIcon: StateFlow<String?> = dataStore.data
        .map { it[favoriteIconKey] }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(5000), null)

    val favoritePhotoUri: StateFlow<String?> = dataStore.data
        .map { it[favoritePhotoUriKey] }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(5000), null)

    suspend fun setUsername(username: String) {
        dataStore.edit { it[usernameKey] = username }
    }

    suspend fun setFavoriteResource(iconId: String?, photoUri: String?) {
        dataStore.edit {
            if (iconId != null) {
                it[favoriteIconKey] = iconId
                it.remove(favoritePhotoUriKey)
            } else if (photoUri != null) {
                it[favoritePhotoUriKey] = photoUri
                it.remove(favoriteIconKey)
            } else {
                it.remove(favoriteIconKey)
                it.remove(favoritePhotoUriKey)
            }
        }
    }
}
