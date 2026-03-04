package com.moxmose.moxequiplog.data

import com.moxmose.moxequiplog.data.local.AppPreference
import com.moxmose.moxequiplog.data.local.AppPreferenceDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class AppSettingsManager(
    private val appPreferenceDao: AppPreferenceDao,
    private val defaultUsername: String
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    val username: StateFlow<String> = appPreferenceDao.getPreferenceFlow("default_username")
        .map { it ?: defaultUsername }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(5000), defaultUsername)

    val defaultEquipmentId: StateFlow<Int?> = appPreferenceDao.getPreferenceFlow("default_equipment_id")
        .map { it?.toIntOrNull() }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(5000), null)

    val defaultOperationTypeId: StateFlow<Int?> = appPreferenceDao.getPreferenceFlow("default_operation_type_id")
        .map { it?.toIntOrNull() }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(5000), null)

    suspend fun setUsername(username: String) {
        appPreferenceDao.insertPreference(AppPreference("default_username", username))
    }

    suspend fun setDefaultEquipmentId(id: Int?) {
        if (id == null) {
            appPreferenceDao.deletePreference("default_equipment_id")
        } else {
            appPreferenceDao.insertPreference(AppPreference("default_equipment_id", id.toString()))
        }
    }

    suspend fun setDefaultOperationTypeId(id: Int?) {
        if (id == null) {
            appPreferenceDao.deletePreference("default_operation_type_id")
        } else {
            appPreferenceDao.insertPreference(AppPreference("default_operation_type_id", id.toString()))
        }
    }
}
