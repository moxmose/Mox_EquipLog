package com.moxmose.moxequiplog.ui.operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moxmose.moxequiplog.data.MediaRepository
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.Media
import com.moxmose.moxequiplog.data.local.MediaIdentifier
import com.moxmose.moxequiplog.data.local.OperationType
import com.moxmose.moxequiplog.data.local.OperationTypeDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OperationTypeViewModel(
    private val operationTypeDao: OperationTypeDao,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    val activeOperationTypes: StateFlow<List<OperationType>> = operationTypeDao.getActiveOperationTypes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val allOperationTypes: StateFlow<List<OperationType>> = operationTypeDao.getAllOperationTypes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val operationTypeMedia: StateFlow<List<Media>> = mediaRepository.getMediaByCategory("OPERATION")
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

    fun addOperationType(description: String, mediaIdentifier: MediaIdentifier?) {
        viewModelScope.launch {
            val maxDisplayOrder = allOperationTypes.first().maxOfOrNull { it.displayOrder } ?: -1
            val operationCategory = allCategories.first().find { it.id == "OPERATION" }

            var operationPhotoUri: String? = null
            var operationIconIdentifier: String? = null

            when (mediaIdentifier) {
                is MediaIdentifier.Icon -> operationIconIdentifier = mediaIdentifier.name
                is MediaIdentifier.Photo -> operationPhotoUri = mediaIdentifier.uri
                null -> { // Usa i default di categoria
                    operationPhotoUri = operationCategory?.defaultPhotoUri
                    operationIconIdentifier = operationCategory?.defaultIconIdentifier
                }
            }

            operationTypeDao.insertOperationType(
                OperationType(
                    description = description,
                    iconIdentifier = operationIconIdentifier,
                    photoUri = operationPhotoUri,
                    displayOrder = maxDisplayOrder + 1
                )
            )
        }
    }

    fun updateOperationType(operationType: OperationType) {
        viewModelScope.launch {
            operationTypeDao.updateOperationType(operationType)
        }
    }

    fun updateOperationTypes(operationTypes: List<OperationType>) {
        viewModelScope.launch {
            operationTypeDao.updateOperationTypes(operationTypes)
        }
    }

    fun dismissOperationType(operationType: OperationType) {
        viewModelScope.launch {
            updateOperationType(operationType.copy(dismissed = true))
        }
    }

    fun restoreOperationType(operationType: OperationType) {
        viewModelScope.launch {
            updateOperationType(operationType.copy(dismissed = false))
        }
    }

    fun addMedia(mediaIdentifier: MediaIdentifier, category: String) {
        viewModelScope.launch {
            mediaRepository.addMedia(mediaIdentifier, category)
        }
    }

    fun toggleMediaVisibility(media: Media) {
        viewModelScope.launch {
            mediaRepository.toggleMediaVisibility(media)
        }
    }
}
