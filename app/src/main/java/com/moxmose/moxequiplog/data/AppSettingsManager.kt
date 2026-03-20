package com.moxmose.moxequiplog.data

import com.moxmose.moxequiplog.data.local.AppPreference
import com.moxmose.moxequiplog.data.local.AppPreferenceDao
import com.moxmose.moxequiplog.utils.UiConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppSettingsManager(
    private val appPreferenceDao: AppPreferenceDao,
    private val defaultUsername: String
) {
    val username: Flow<String> = appPreferenceDao.getPreferenceFlow("default_username")
        .map { it ?: defaultUsername }

    val defaultEquipmentId: Flow<Int?> = appPreferenceDao.getPreferenceFlow("default_equipment_id")
        .map { it?.toIntOrNull() }

    val defaultOperationTypeId: Flow<Int?> = appPreferenceDao.getPreferenceFlow("default_operation_type_id")
        .map { it?.toIntOrNull() }
        
    val defaultUnitId: Flow<Int?> = appPreferenceDao.getPreferenceFlow("default_unit_id")
        .map { it?.toIntOrNull() }

    val backgroundUri: Flow<String?> = appPreferenceDao.getPreferenceFlow("background_uri")

    val backgroundBlur: Flow<Float> = appPreferenceDao.getPreferenceFlow("background_blur")
        .map { it?.toFloatOrNull() ?: UiConstants.DEFAULT_BACKGROUND_BLUR }

    val backgroundSaturation: Flow<Float> = appPreferenceDao.getPreferenceFlow("background_saturation")
        .map { it?.toFloatOrNull() ?: UiConstants.DEFAULT_BACKGROUND_SATURATION }

    val backgroundTintEnabled: Flow<Boolean> = appPreferenceDao.getPreferenceFlow("background_tint_enabled")
        .map { it?.toBoolean() ?: UiConstants.DEFAULT_BACKGROUND_TINT_ENABLED }

    val backgroundTintAlpha: Flow<Float> = appPreferenceDao.getPreferenceFlow("background_tint_alpha")
        .map { it?.toFloatOrNull() ?: UiConstants.DEFAULT_BACKGROUND_TINT_ALPHA }

    val backgroundImageAlpha: Flow<Float> = appPreferenceDao.getPreferenceFlow("background_image_alpha")
        .map { it?.toFloatOrNull() ?: UiConstants.DEFAULT_BACKGROUND_IMAGE_ALPHA }

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
    
    suspend fun setDefaultUnitId(id: Int?) {
        if (id == null) {
            appPreferenceDao.deletePreference("default_unit_id")
        } else {
            appPreferenceDao.insertPreference(AppPreference("default_unit_id", id.toString()))
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

    suspend fun setBackgroundImageAlpha(alpha: Float) {
        appPreferenceDao.insertPreference(AppPreference("background_image_alpha", alpha.toString()))
    }
}
