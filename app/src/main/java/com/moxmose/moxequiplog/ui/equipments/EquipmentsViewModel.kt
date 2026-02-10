package com.moxmose.moxequiplog.ui.equipments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moxmose.moxequiplog.data.MediaRepository
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.Equipment
import com.moxmose.moxequiplog.data.local.EquipmentDao
import com.moxmose.moxequiplog.data.local.Media
import com.moxmose.moxequiplog.data.local.MediaIdentifier
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EquipmentsViewModel(
    private val equipmentDao: EquipmentDao,
    private val mediaRepository: MediaRepository
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

    val equipmentMedia: StateFlow<List<Media>> = mediaRepository.getMediaByCategory("EQUIPMENT")
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

    fun addEquipment(description: String, mediaIdentifier: MediaIdentifier?) {
        viewModelScope.launch {
            val currentList = allEquipments.value
            val nextOrder = if (currentList.isEmpty()) 0 else currentList.maxOf { it.displayOrder } + 1
            val equipmentCategory = allCategories.first().find { it.id == "EQUIPMENT" }

            var equipmentPhotoUri: String? = null
            var equipmentIconIdentifier: String? = null

            when (mediaIdentifier) {
                is MediaIdentifier.Icon -> equipmentIconIdentifier = mediaIdentifier.name
                is MediaIdentifier.Photo -> equipmentPhotoUri = mediaIdentifier.uri
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

    fun addMedia(mediaIdentifier: MediaIdentifier, category: String) {
        viewModelScope.launch {
            mediaRepository.addMedia(mediaIdentifier, category)
        }
    }

    fun removeMedia(uri: String, category: String) {
        viewModelScope.launch {
            mediaRepository.removeMedia(uri, category)
        }
    }

    fun updateMediaOrder(mediaList: List<Media>) {
        viewModelScope.launch {
            mediaRepository.updateMediaOrder(mediaList)
        }
    }

    fun toggleMediaVisibility(uri: String, category: String) {
        viewModelScope.launch {
            mediaRepository.toggleMediaVisibility(uri, category)
        }
    }

    suspend fun isPhotoUsed(uri: String): Boolean {
        return equipmentDao.countEquipmentsUsingPhoto(uri) > 0
    }
}
