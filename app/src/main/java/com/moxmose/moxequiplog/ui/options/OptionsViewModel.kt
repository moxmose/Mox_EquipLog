package com.moxmose.moxequiplog.ui.options

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.MediaRepository
import com.moxmose.moxequiplog.data.local.AppColor
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.EquipmentDao
import com.moxmose.moxequiplog.data.local.Media
import com.moxmose.moxequiplog.data.local.MediaIdentifier
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OptionsViewModel(
    private val appSettingsManager: AppSettingsManager,
    private val equipmentDao: EquipmentDao,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    sealed class OptionsUiEvent {
        // Errori Generici di Repository
        data object DatabaseCheckFailed : OptionsUiEvent()

        // Errori Funzione `removeMedia`
        data object RemoveMediaFailed : OptionsUiEvent()

        // Errori Funzione `updateColor`
        data object UpdateColorFailed : OptionsUiEvent()
        data object ColorNameInvalid : OptionsUiEvent()

        // Errori Funzione `isPhotoUsed`
        data object PhotoUriInvalid : OptionsUiEvent()

        // Errori Funzione `setCategoryDefault`
        data object SetCategoryDefaultFailed : OptionsUiEvent()
        data object CategoryIdInvalid : OptionsUiEvent()

        // Errori Funzione `toggleMediaVisibility`
        data object ToggleMediaVisibilityFailed : OptionsUiEvent()
        data object MediaInfoInvalid : OptionsUiEvent()

        // Errori Funzione `addMedia`
        data object AddMediaFailed : OptionsUiEvent()

        // Errori Funzione `updateMediaOrder`
        data object UpdateMediaOrderFailed : OptionsUiEvent()

        // Errori Funzione `updateCategoryColor`
        data object UpdateCategoryColorFailed : OptionsUiEvent()
        data object ColorHexInvalid : OptionsUiEvent()

        // Errori Funzione `addColor`
        data object AddColorFailed : OptionsUiEvent()

        // Errori Funzione `updateColorsOrder`
        data object UpdateColorsOrderFailed : OptionsUiEvent()
        data object ColorListInvalid : OptionsUiEvent()

        // Errori Funzione `toggleColorVisibility`
        data object ToggleColorVisibilityFailed : OptionsUiEvent()
        data object ColorIdInvalid : OptionsUiEvent()

        // Errori Funzione `deleteColor`
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

    fun setCategoryDefault(categoryId: String, mediaIdentifier: MediaIdentifier?) {
        if (categoryId.isBlank()) {
            viewModelScope.launch { _uiEvents.send(OptionsUiEvent.CategoryIdInvalid) }
            return
        }
        viewModelScope.launch {
            try {
                mediaRepository.setCategoryDefault(categoryId, mediaIdentifier)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.SetCategoryDefaultFailed)
            }
        }
    }

    fun toggleMediaVisibility(uri: String, category: String) {
        if (uri.isBlank() || category.isBlank()) {
            viewModelScope.launch { _uiEvents.send(OptionsUiEvent.MediaInfoInvalid) }
            return
        }
        viewModelScope.launch {
            try {
                mediaRepository.toggleMediaVisibility(uri, category)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.ToggleMediaVisibilityFailed)
            }
        }
    }

    fun addMedia(mediaIdentifier: MediaIdentifier, category: String) {
        if (category.isBlank()) {
            viewModelScope.launch { _uiEvents.send(OptionsUiEvent.CategoryIdInvalid) }
            return
        }
        viewModelScope.launch {
            try {
                mediaRepository.addMedia(mediaIdentifier, category)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.AddMediaFailed)
            }
        }
    }

    fun removeMedia(uri: String, category: String) {
        if (uri.isBlank() || category.isBlank()) {
            viewModelScope.launch { _uiEvents.send(OptionsUiEvent.MediaInfoInvalid) }
            return
        }
        viewModelScope.launch {
            try {
                mediaRepository.removeMedia(uri, category)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.RemoveMediaFailed)
            }
        }
    }

    fun updateMediaOrder(mediaList: List<Media>) {
        if (mediaList.isEmpty()) {
            return // Esegui una no-op efficiente, non è un errore
        }
        viewModelScope.launch {
            try {
                mediaRepository.updateMediaOrder(mediaList)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateMediaOrderFailed)
            }
        }
    }

    fun updateCategoryColor(categoryId: String, colorHex: String) {
        if (categoryId.isBlank()) {
            viewModelScope.launch { _uiEvents.send(OptionsUiEvent.CategoryIdInvalid) }
            return
        }
        if (colorHex.isBlank()) {
            viewModelScope.launch { _uiEvents.send(OptionsUiEvent.ColorHexInvalid) }
            return
        }
        viewModelScope.launch {
            try {
                mediaRepository.updateCategoryColor(categoryId, colorHex)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateCategoryColorFailed)
            }
        }
    }

    fun addColor(hex: String, name: String) {
        if (name.isBlank()) {
            viewModelScope.launch { _uiEvents.send(OptionsUiEvent.ColorNameInvalid) }
            return
        }
        if (hex.isBlank()) {
            viewModelScope.launch { _uiEvents.send(OptionsUiEvent.ColorHexInvalid) }
            return
        }
        viewModelScope.launch {
            try {
                mediaRepository.addColor(hex, name)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.AddColorFailed)
            }
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
            try {
                mediaRepository.updateColorsOrder(colors)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateColorsOrderFailed)
            }
        }
    }

    fun toggleColorVisibility(id: Long) {
        if (id == 0L) {
            viewModelScope.launch { _uiEvents.send(OptionsUiEvent.ColorIdInvalid) }
            return
        }
        viewModelScope.launch {
            try {
                mediaRepository.toggleColorVisibility(id)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.ToggleColorVisibilityFailed)
            }
        }
    }

    fun deleteColor(color: AppColor) {
        if (color.id == 0L) {
            viewModelScope.launch { _uiEvents.send(OptionsUiEvent.ColorIdInvalid) }
            return
        }
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
