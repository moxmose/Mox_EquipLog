package com.moxmose.moxequiplog.ui.options

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.MediaRepository
import com.moxmose.moxequiplog.data.local.AppColor
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.EquipmentDao
import com.moxmose.moxequiplog.data.local.Media
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OptionsViewModel(
    private val appSettingsManager: AppSettingsManager,
    private val equipmentDao: EquipmentDao,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    sealed class OptionsUiEvent {
        data object RemoveMediaFailed : OptionsUiEvent()
        data object UpdateColorFailed : OptionsUiEvent()
        data object ColorNameInvalid : OptionsUiEvent()
        data object PhotoUriInvalid : OptionsUiEvent()
        data object DatabaseCheckFailed : OptionsUiEvent()
        data object ColorListInvalid : OptionsUiEvent()
        data object ColorInvalid : OptionsUiEvent()
        data object DeleteColorFailed : OptionsUiEvent()
    }

    private val _uiEvents = Channel<OptionsUiEvent>()
    val uiEvents: Flow<OptionsUiEvent> = _uiEvents.receiveAsFlow()

    init {
        viewModelScope.launch {
            mediaRepository.initializeAppData()
        }
    }

    val username: StateFlow<String> = appSettingsManager.username
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = ""
        )

    val allMedia: StateFlow<List<Media>> = mediaRepository.allMedia
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val allCategories: StateFlow<List<Category>> = mediaRepository.allCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val allColors: StateFlow<List<AppColor>> = mediaRepository.allColors
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun setUsername(newUsername: String) {
        viewModelScope.launch {
            appSettingsManager.setUsername(newUsername)
        }
    }

    fun setCategoryDefault(categoryId: String, iconId: String?, photoUri: String?) {
        viewModelScope.launch {
            mediaRepository.setCategoryDefault(categoryId, iconId, photoUri)
        }
    }

    fun toggleMediaVisibility(uri: String, category: String) {
        viewModelScope.launch {
            mediaRepository.toggleMediaVisibility(uri, category)
        }
    }

    fun addMedia(uri: String, category: String) {
        viewModelScope.launch {
            mediaRepository.addMedia(uri, category)
        }
    }

    fun removeMedia(uri: String, category: String) {
        viewModelScope.launch {
            try {
                mediaRepository.removeMedia(uri, category)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.RemoveMediaFailed)
            }
        }
    }

    fun updateMediaOrder(mediaList: List<Media>) {
        viewModelScope.launch {
            mediaRepository.updateMediaOrder(mediaList)
        }
    }

    fun updateCategoryColor(categoryId: String, colorHex: String) {
        viewModelScope.launch {
            mediaRepository.updateCategoryColor(categoryId, colorHex)
        }
    }

    fun addColor(hex: String, name: String) {
        viewModelScope.launch {
            mediaRepository.addColor(hex, name)
        }
    }

    fun updateColor(color: AppColor) {
        if (color.name.isBlank()) {
            viewModelScope.launch { _uiEvents.send(OptionsUiEvent.ColorNameInvalid) }
            return
        }
        viewModelScope.launch {
            try {
                mediaRepository.updateColor(color)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateColorFailed)
            }
        }
    }

    fun updateColorsOrder(colors: List<AppColor>) {
        if (colors.isEmpty()) {
            viewModelScope.launch { _uiEvents.send(OptionsUiEvent.ColorListInvalid) }
            return
        }
        viewModelScope.launch {
            mediaRepository.updateColorsOrder(colors)
        }
    }

    fun toggleColorVisibility(id: Long) {
        viewModelScope.launch {
            mediaRepository.toggleColorVisibility(id)
        }
    }

    fun deleteColor(color: AppColor) {
        if (color.id == 0L) {
            viewModelScope.launch { _uiEvents.send(OptionsUiEvent.ColorInvalid) }
            return
        } // Non puoi eliminare)
        viewModelScope.launch {
            try {
                mediaRepository.deleteColor(color)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.DeleteColorFailed)
            }
        }
    }

    suspend fun isPhotoUsed(uri: String): Boolean {
        if (uri.isBlank()) {
            _uiEvents.send(OptionsUiEvent.PhotoUriInvalid)
            return true // Ritorna 'true' per sicurezza, per prevenire eliminazioni
        }
        return try {
            equipmentDao.countEquipmentsUsingPhoto(uri) > 0
        } catch (e: Exception) {
            _uiEvents.send(OptionsUiEvent.DatabaseCheckFailed)
            true // In caso di dubbio/errore, assumi che la foto sia in uso per sicurezza.
        }
    }
}
