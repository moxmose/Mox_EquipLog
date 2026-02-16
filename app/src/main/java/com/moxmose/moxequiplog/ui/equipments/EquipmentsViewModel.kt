package com.moxmose.moxequiplog.ui.equipments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moxmose.moxequiplog.data.ImageRepository
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.Equipment
import com.moxmose.moxequiplog.data.local.EquipmentDao
import com.moxmose.moxequiplog.data.local.Image
import com.moxmose.moxequiplog.data.local.ImageIdentifier
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EquipmentsViewModel(
    private val equipmentDao: EquipmentDao,
    private val imageRepository: ImageRepository
) : ViewModel() {

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

    fun addEquipment(description: String, imageIdentifier: ImageIdentifier?) {
        viewModelScope.launch {
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
        }
    }

    fun updateEquipment(equipment: Equipment) {
        viewModelScope.launch {
            equipmentDao.updateEquipment(equipment)
        }
    }

    fun updateEquipments(equipments: List<Equipment>) {
        viewModelScope.launch {
            equipmentDao.updateEquipments(equipments)
        }
    }

    fun dismissEquipment(equipment: Equipment) {
        viewModelScope.launch {
            equipmentDao.updateEquipment(equipment.copy(dismissed = true))
        }
    }

    fun restoreEquipment(equipment: Equipment) {
        viewModelScope.launch {
            equipmentDao.updateEquipment(equipment.copy(dismissed = false))
        }
    }

    fun addImage(imageIdentifier: ImageIdentifier, category: String) {
        viewModelScope.launch {
            imageRepository.addImage(imageIdentifier, category)
        }
    }

    fun removeImage(image: Image) {
        viewModelScope.launch {
            imageRepository.removeImage(image)
        }
    }

    fun updateImageOrder(imageList: List<Image>) {
        viewModelScope.launch {
            imageRepository.updateImageOrder(imageList)
        }
    }

    fun toggleImageVisibility(image: Image) {
        viewModelScope.launch {
            imageRepository.toggleImageVisibility(image)
        }
    }

    suspend fun isPhotoUsed(uri: String): Boolean {
        return equipmentDao.countEquipmentsUsingPhoto(uri) > 0
    }
}
