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
        .stateIn(coroutineScope, SharingStarted.Eagerly, defaultUsername)

    val defaultEquipmentId: StateFlow<Int?> = appPreferenceDao.getPreferenceFlow("default_equipment_id")
        .map { it?.toIntOrNull() }
        .stateIn(coroutineScope, SharingStarted.Eagerly, null)

    val defaultOperationTypeId: StateFlow<Int?> = appPreferenceDao.getPreferenceFlow("default_operation_type_id")
        .map { it?.toIntOrNull() }
        .stateIn(coroutineScope, SharingStarted.Eagerly, null)

    val backgroundUri: StateFlow<String?> = appPreferenceDao.getPreferenceFlow("background_uri")
        .stateIn(coroutineScope, SharingStarted.Eagerly, null)

    val backgroundBlur: StateFlow<Float> = appPreferenceDao.getPreferenceFlow("background_blur")
        .map { it?.toFloatOrNull() ?: 0f }
        .stateIn(coroutineScope, SharingStarted.Eagerly, 0f)

    val backgroundSaturation: StateFlow<Float> = appPreferenceDao.getPreferenceFlow("background_saturation")
        .map { it?.toFloatOrNull() ?: 1f }
        .stateIn(coroutineScope, SharingStarted.Eagerly, 1f)

    val backgroundTintEnabled: StateFlow<Boolean> = appPreferenceDao.getPreferenceFlow("background_tint_enabled")
        .map { it?.toBoolean() ?: false }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    val backgroundTintAlpha: StateFlow<Float> = appPreferenceDao.getPreferenceFlow("background_tint_alpha")
        .map { it?.toFloatOrNull() ?: 0.25f }
        .stateIn(coroutineScope, SharingStarted.Eagerly, 0.25f)

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

    suspend fun setBackgroundUri(uri: String?) {
        if (uri == null) {
            appPreferenceDao.deletePreference("background_uri")
        } else {
            appPreferenceDao.insertPreference(AppPreference("background_uri", uri))
        }
    }

    suspend fun setBackgroundBlur(blur: Float) {
        appPreferenceDao.insertPreference(AppPreference("background_blur", blur.toString()))
    }

    suspend fun setBackgroundSaturation(saturation: Float) {
        appPreferenceDao.insertPreference(AppPreference("background_saturation", saturation.toString()))
    }

    suspend fun setBackgroundTintEnabled(enabled: Boolean) {
        appPreferenceDao.insertPreference(AppPreference("background_tint_enabled", enabled.toString()))
    }

    suspend fun setBackgroundTintAlpha(alpha: Float) {
        appPreferenceDao.insertPreference(AppPreference("background_tint_alpha", alpha.toString()))
    }
}
