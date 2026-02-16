package com.moxmose.moxequiplog.ui.options

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.ImageRepository
import com.moxmose.moxequiplog.data.local.AppColor
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.EquipmentDao
import com.moxmose.moxequiplog.data.local.Image
import com.moxmose.moxequiplog.data.local.ImageIdentifier
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
    private val imageRepository: ImageRepository
) : ViewModel() {

    sealed class OptionsUiEvent {
        // Errori Generici di Repository
        data object DatabaseCheckFailed : OptionsUiEvent()

        // Errori Funzione `setUsername`
        data object UsernameInvalid : OptionsUiEvent()
        data object UpdateUsernameFailed : OptionsUiEvent()

        // Errori Funzione `removeImage`
        data object RemoveImageFailed : OptionsUiEvent()

        // Errori Funzione `updateColor`
        data object UpdateColorFailed : OptionsUiEvent()
        data object ColorNameInvalid : OptionsUiEvent()

        // Errori Funzione `isPhotoUsed`
        data object PhotoUriInvalid : OptionsUiEvent()

        // Errori Funzione `setCategoryDefault`
        data object SetCategoryDefaultFailed : OptionsUiEvent()
        data object CategoryIdInvalid : OptionsUiEvent()
        data object NoImageSelectedForDefault : OptionsUiEvent()

        // Errori Funzione `toggleImageVisibility`
        data object ToggleImageVisibilityFailed : OptionsUiEvent()

        // Errori Funzione `addImage`
        data object AddImageFailed : OptionsUiEvent()

        // Errori Funzione `updateImageOrder`
        data object UpdateImageOrderFailed : OptionsUiEvent()

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
            imageRepository.initializeAppData()
        }
    }

    val username: StateFlow<String> = appSettingsManager.username
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = ""
        )

    val allImages: StateFlow<List<Image>> = imageRepository.allImages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val allCategories: StateFlow<List<Category>> = imageRepository.allCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val allColors: StateFlow<List<AppColor>> = imageRepository.allColors
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun setUsername(newUsername: String) {
        if (newUsername.isBlank()) {
            viewModelScope.launch { _uiEvents.send(OptionsUiEvent.UsernameInvalid) }
            return
        }
        viewModelScope.launch {
            try {
                appSettingsManager.setUsername(newUsername)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateUsernameFailed)
            }
        }
    }

    fun setCategoryDefault(categoryId: String, imageIdentifier: ImageIdentifier?) {
        if (categoryId.isBlank()) {
            viewModelScope.launch { _uiEvents.send(OptionsUiEvent.CategoryIdInvalid) }
            return
        }
        viewModelScope.launch {
            try {
                imageRepository.setCategoryDefault(categoryId, imageIdentifier)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.SetCategoryDefaultFailed)
            }
        }
    }

    fun toggleImageVisibility(image: Image) {
        viewModelScope.launch {
            try {
                imageRepository.toggleImageVisibility(image)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.ToggleImageVisibilityFailed)
            }
        }
    }

    fun addImage(imageIdentifier: ImageIdentifier, category: String) {
        if (category.isBlank()) {
            viewModelScope.launch { _uiEvents.send(OptionsUiEvent.CategoryIdInvalid) }
            return
        }
        viewModelScope.launch {
            try {
                imageRepository.addImage(imageIdentifier, category)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.AddImageFailed)
            }
        }
    }

    fun removeImage(image: Image) {
        viewModelScope.launch {
            try {
                imageRepository.removeImage(image)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.RemoveImageFailed)
            }
        }
    }

    fun updateImageOrder(imageList: List<Image>) {
        if (imageList.isEmpty()) {
            return // Esegui una no-op efficiente, non è un errore
        }
        viewModelScope.launch {
            try {
                imageRepository.updateImageOrder(imageList)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateImageOrderFailed)
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
                imageRepository.updateCategoryColor(categoryId, colorHex)
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
                imageRepository.addColor(hex, name)
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
                imageRepository.updateColor(color)
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
                imageRepository.updateColorsOrder(colors)
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
                imageRepository.toggleColorVisibility(id)
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
                imageRepository.deleteColor(color)
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
