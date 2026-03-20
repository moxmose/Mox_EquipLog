package com.moxmose.moxequiplog.ui.operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.ImageRepository
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.Image
import com.moxmose.moxequiplog.data.local.ImageIdentifier
import com.moxmose.moxequiplog.data.local.OperationType
import com.moxmose.moxequiplog.data.local.OperationTypeDao
import com.moxmose.moxequiplog.utils.AppConstants
import com.moxmose.moxequiplog.utils.UiConstants
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class OperationsTypeViewModel(
    private val operationTypeDao: OperationTypeDao,
    private val imageRepository: ImageRepository,
    private val appSettingsManager: AppSettingsManager
) : ViewModel() {

    sealed class UiEvent {
        data object DescriptionInvalid : UiEvent()
        data object AddOperationTypeFailed : UiEvent()
        data object UpdateOperationTypeFailed : UiEvent()
        data object UpdateOperationTypesFailed : UiEvent()
        data object DismissOperationTypeFailed : UiEvent()
        data object RestoreOperationTypeFailed : UiEvent()
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

    val activeOperationTypes: StateFlow<List<OperationType>> = operationTypeDao.getActiveOperationTypes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT),
            initialValue = emptyList()
        )

    val allOperationTypes: StateFlow<List<OperationType>> = operationTypeDao.getAllOperationTypes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT),
            initialValue = emptyList()
        )

    val operationImages: StateFlow<List<Image>> = imageRepository.getImagesByCategory(Category.OPERATION)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT),
            initialValue = emptyList()
        )

    val allCategories: StateFlow<List<Category>> = imageRepository.allCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT),
            initialValue = emptyList()
        )

    val categoryColor: StateFlow<String> = imageRepository.getCategoryColor(Category.OPERATION)
        .map { it ?: UiConstants.DEFAULT_FALLBACK_COLOR }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), UiConstants.DEFAULT_FALLBACK_COLOR)

    val categoryDefaultIcon: StateFlow<String?> = imageRepository.getCategoryDefaultIcon(Category.OPERATION)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), null)

    val categoryDefaultPhoto: StateFlow<String?> = imageRepository.getCategoryDefaultPhoto(Category.OPERATION)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), null)

    val defaultOperationTypeId: StateFlow<Int?> = appSettingsManager.defaultOperationTypeId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), null)

    fun setDefaultOperationType(id: Int?) {
        viewModelScope.launch {
            try {
                appSettingsManager.setDefaultOperationTypeId(id)
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.SetDefaultFailed)
            }
        }
    }

    fun toggleDefaultOperationType(id: Int) {
        viewModelScope.launch {
            try {
                val currentDefault = defaultOperationTypeId.value
                if (currentDefault == id) {
                    appSettingsManager.setDefaultOperationTypeId(null)
                } else {
                    appSettingsManager.setDefaultOperationTypeId(id)
                }
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.SetDefaultFailed)
            }
        }
    }

    fun addOperationType(description: String, imageIdentifier: ImageIdentifier?) {
        if (description.isBlank()) {
            viewModelScope.launch { _uiEvents.send(UiEvent.DescriptionInvalid) }
            return
        }
        viewModelScope.launch {
            try {
                val currentList = allOperationTypes.value
                val nextOrder = if (currentList.isEmpty()) 0 else currentList.maxOf { it.displayOrder } + 1
                
                var operationPhotoUri: String? = null
                var operationIconIdentifier: String? = null

                when (imageIdentifier) {
                    is ImageIdentifier.Icon -> operationIconIdentifier = imageIdentifier.name
                    is ImageIdentifier.Photo -> operationPhotoUri = imageIdentifier.uri
                    null -> {
                        operationPhotoUri = categoryDefaultPhoto.value
                        operationIconIdentifier = categoryDefaultIcon.value
                    }
                }

                operationTypeDao.insertOperationType(
                    OperationType(
                        description = description,
                        photoUri = operationPhotoUri,
                        iconIdentifier = operationIconIdentifier,
                        displayOrder = nextOrder
                    )
                )
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.AddOperationTypeFailed)
            }
        }
    }

    fun updateOperationType(operationType: OperationType) {
        viewModelScope.launch {
            try {
                operationTypeDao.updateOperationType(operationType)
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.UpdateOperationTypeFailed)
            }
        }
    }

    fun updateOperationTypes(operationTypes: List<OperationType>) {
        if (operationTypes.isEmpty()) return
        viewModelScope.launch {
            try {
                operationTypeDao.updateOperationTypes(operationTypes)
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.UpdateOperationTypesFailed)
            }
        }
    }

    fun dismissOperationType(operationType: OperationType) {
        viewModelScope.launch {
            try {
                operationTypeDao.updateOperationType(operationType.copy(dismissed = true))
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.DismissOperationTypeFailed)
            }
        }
    }

    fun restoreOperationType(operationType: OperationType) {
        viewModelScope.launch {
            try {
                operationTypeDao.updateOperationType(operationType.copy(dismissed = false))
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.RestoreOperationTypeFailed)
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
            operationTypeDao.countOperationTypesUsingPhoto(uri) > 0
        } catch (e: Exception) {
            _uiEvents.trySend(UiEvent.DatabaseCheckFailed)
            true
        }
    }
}
