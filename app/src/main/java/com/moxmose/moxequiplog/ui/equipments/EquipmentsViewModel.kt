package com.moxmose.moxequiplog.ui.equipments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.ImageRepository
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.Equipment
import com.moxmose.moxequiplog.data.local.EquipmentDao
import com.moxmose.moxequiplog.data.local.Image
import com.moxmose.moxequiplog.data.local.ImageIdentifier
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EquipmentsViewModel(
    private val equipmentDao: EquipmentDao,
    private val imageRepository: ImageRepository,
    private val appSettingsManager: AppSettingsManager
) : ViewModel() {

    sealed class UiEvent {
        data object DescriptionInvalid : UiEvent()
        data object AddEquipmentFailed : UiEvent()
        data object UpdateEquipmentFailed : UiEvent()
        data object UpdateEquipmentsFailed : UiEvent()
        data object DismissEquipmentFailed : UiEvent()
        data object RestoreEquipmentFailed : UiEvent()
        data object AddImageFailed : UiEvent()
        data object RemoveImageFailed : UiEvent()
        data object UpdateImageOrderFailed : UiEvent()
        data object ToggleImageVisibilityFailed : UiEvent()
        data object DatabaseCheckFailed : UiEvent()
        data object PhotoUriInvalid : UiEvent()
        data object SetDefaultFailed : UiEvent()
    }

    private val _uiEvents = Channel<UiEvent>()
    val uiEvents: Flow<UiEvent> = _uiEvents.receiveAsFlow()

    val activeEquipments: StateFlow<List<Equipment>> = equipmentDao.getActiveEquipments()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val allEquipments: StateFlow<List<Equipment>> = equipmentDao.getAllEquipments()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val equipmentImages: StateFlow<List<Image>> = imageRepository.getImagesByCategory("EQUIPMENT")
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

    val defaultEquipmentId: StateFlow<Int?> = appSettingsManager.defaultEquipmentId

    fun setDefaultEquipment(id: Int?) {
        viewModelScope.launch {
            try {
                appSettingsManager.setDefaultEquipmentId(id)
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.SetDefaultFailed)
            }
        }
    }

    fun toggleDefaultEquipment(id: Int) {
        viewModelScope.launch {
            try {
                val currentDefault = defaultEquipmentId.value
                if (currentDefault == id) {
                    appSettingsManager.setDefaultEquipmentId(null)
                } else {
                    appSettingsManager.setDefaultEquipmentId(id)
                }
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.SetDefaultFailed)
            }
        }
    }

    fun addEquipment(description: String, imageIdentifier: ImageIdentifier?) {
        if (description.isBlank()) {
            viewModelScope.launch { _uiEvents.send(UiEvent.DescriptionInvalid) }
            return
        }
        viewModelScope.launch {
            try {
                val currentList = allEquipments.value
                val nextOrder = if (currentList.isEmpty()) 0 else currentList.maxOf { it.displayOrder } + 1
                val equipmentCategory = allCategories.first().find { it.id == "EQUIPMENT" }

                var equipmentPhotoUri: String? = null
                var equipmentIconIdentifier: String? = null

                when (imageIdentifier) {
                    is ImageIdentifier.Icon -> equipmentIconIdentifier = imageIdentifier.name
                    is ImageIdentifier.Photo -> equipmentPhotoUri = imageIdentifier.uri
                    null -> { // Usa i default di categoria
                        equipmentPhotoUri = equipmentCategory?.defaultPhotoUri
                        equipmentIconIdentifier = equipmentCategory?.defaultIconIdentifier
                    }
                }

                equipmentDao.insertEquipment(
                    Equipment(
                        description = description,
                        photoUri = equipmentPhotoUri,
                        iconIdentifier = equipmentIconIdentifier,
                        displayOrder = nextOrder
                    )
                )
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.AddEquipmentFailed)
            }
        }
    }

    fun updateEquipment(equipment: Equipment) {
        viewModelScope.launch {
            try {
                equipmentDao.updateEquipment(equipment)
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.UpdateEquipmentFailed)
            }
        }
    }

    fun updateEquipments(equipments: List<Equipment>) {
        if (equipments.isEmpty()) return
        viewModelScope.launch {
            try {
                equipmentDao.updateEquipments(equipments)
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.UpdateEquipmentsFailed)
            }
        }
    }

    fun dismissEquipment(equipment: Equipment) {
        viewModelScope.launch {
            try {
                equipmentDao.updateEquipment(equipment.copy(dismissed = true))
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.DismissEquipmentFailed)
            }
        }
    }

    fun restoreEquipment(equipment: Equipment) {
        viewModelScope.launch {
            try {
                equipmentDao.updateEquipment(equipment.copy(dismissed = false))
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.RestoreEquipmentFailed)
            }
        }
    }

    fun addImage(imageIdentifier: ImageIdentifier, category: String) {
        viewModelScope.launch {
            try {
                imageRepository.addImage(imageIdentifier, category)
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.AddImageFailed)
            }
        }
    }

    fun removeImage(image: Image) {
        viewModelScope.launch {
            try {
                imageRepository.removeImage(image)
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.RemoveImageFailed)
            }
        }
    }

    fun updateImageOrder(imageList: List<Image>) {
        if (imageList.isEmpty()) return
        viewModelScope.launch {
            try {
                imageRepository.updateImageOrder(imageList)
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.UpdateImageOrderFailed)
            }
        }
    }

    fun toggleImageVisibility(image: Image) {
        viewModelScope.launch {
            try {
                imageRepository.toggleImageVisibility(image)
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.ToggleImageVisibilityFailed)
            }
        }
    }

    suspend fun isPhotoUsed(uri: String): Boolean {
        if (uri.isBlank()) {
            _uiEvents.send(UiEvent.PhotoUriInvalid)
            return true
        }
        return try {
            equipmentDao.countEquipmentsUsingPhoto(uri) > 0
        } catch (e: Exception) {
            _uiEvents.send(UiEvent.DatabaseCheckFailed)
            true
        }
    }
}
