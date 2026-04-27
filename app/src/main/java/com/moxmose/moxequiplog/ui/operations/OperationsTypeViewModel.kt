package com.moxmose.moxequiplog.ui.operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.ImageRepository
import com.moxmose.moxequiplog.data.local.*
import com.moxmose.moxequiplog.utils.AppConstants
import com.moxmose.moxequiplog.utils.UiConstants
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class EquipmentOperationStatus(
    val equipment: Equipment,
    val lastLogDate: Long?,
    val lastLogValue: Double?,
    val nextPresumedDate: Long?,
    val isOverdue: Boolean
)

data class OperationGlobalStatus(
    val operationId: Int,
    val affectedEquipments: List<EquipmentOperationStatus>
)

class OperationsTypeViewModel(
    private val operationTypeDao: OperationTypeDao,
    private val equipmentDao: EquipmentDao,
    private val imageRepository: ImageRepository,
    private val appSettingsManager: AppSettingsManager,
    private val maintenanceLogDao: MaintenanceLogDao
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

    val allOperationTypes: StateFlow<List<OperationType>> = combine(
        operationTypeDao.getAllOperationTypes(),
        equipmentDao.countActiveResettableEquipments()
    ) { types, resettableCount ->
        types.map { type ->
            if (type.isSystem && type.id == AppConstants.SYSTEM_OPERATION_RESET_ID) {
                type.copy(dismissed = resettableCount == 0)
            } else {
                type
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT),
        initialValue = emptyList()
    )

    val activeOperationTypes: StateFlow<List<OperationType>> = allOperationTypes
        .map { types -> types.filter { !it.dismissed } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT),
            initialValue = emptyList()
        )

    val operationStatuses: StateFlow<Map<Int, OperationGlobalStatus>> = combine(
        activeOperationTypes,
        equipmentDao.getActiveEquipments()
    ) { opTypes, equipments ->
        val statuses = mutableMapOf<Int, OperationGlobalStatus>()
        opTypes.filter { it.isPredictable }.forEach { opType ->
            val affected = equipments.mapNotNull { equipment ->
                val lastLog = maintenanceLogDao.getLastLogForEquipmentAndOperation(equipment.id, opType.id)
                if (lastLog == null) return@mapNotNull null
                
                val status = calculateEquipmentStatusForOp(equipment, opType, lastLog)
                
                // Horizon check
                val horizonMs = when (opType.visibilityHorizonUnit) {
                    TimeGranularity.MINUTES_5 -> opType.visibilityHorizon * 5 * 60 * 1000L
                    TimeGranularity.MINUTES_15 -> opType.visibilityHorizon * 15 * 60 * 1000L
                    TimeGranularity.HOURS -> opType.visibilityHorizon * 60 * 60 * 1000L
                    TimeGranularity.DAYS -> opType.visibilityHorizon * 24 * 60 * 60 * 1000L
                    TimeGranularity.WEEKS -> opType.visibilityHorizon * 7 * 24 * 60 * 60 * 1000L
                    TimeGranularity.MONTHS -> opType.visibilityHorizon * 30 * 24 * 60 * 60 * 1000L
                    TimeGranularity.YEARS -> opType.visibilityHorizon * 365 * 24 * 60 * 60 * 1000L
                }
                val now = System.currentTimeMillis()
                if (status.isOverdue || (status.nextPresumedDate != null && status.nextPresumedDate <= now + horizonMs)) {
                    status
                } else null
            }
            statuses[opType.id] = OperationGlobalStatus(opType.id, affected)
        }
        statuses
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private suspend fun calculateEquipmentStatusForOp(equipment: Equipment, opType: OperationType, lastLog: MaintenanceLog): EquipmentOperationStatus {
        val now = System.currentTimeMillis()
        val trend = calculateTrend(equipment)

        val datePrediction = opType.timeoutValue?.let { value ->
            opType.timeoutUnit?.let { unit ->
                val cal = Calendar.getInstance()
                cal.timeInMillis = lastLog.date
                when (unit) {
                    TimeGranularity.MINUTES_5 -> cal.add(Calendar.MINUTE, value * 5)
                    TimeGranularity.MINUTES_15 -> cal.add(Calendar.MINUTE, value * 15)
                    TimeGranularity.HOURS -> cal.add(Calendar.HOUR_OF_DAY, value)
                    TimeGranularity.DAYS -> cal.add(Calendar.DAY_OF_YEAR, value)
                    TimeGranularity.WEEKS -> cal.add(Calendar.WEEK_OF_YEAR, value)
                    TimeGranularity.MONTHS -> cal.add(Calendar.MONTH, value)
                    TimeGranularity.YEARS -> cal.add(Calendar.YEAR, value)
                }
                cal.timeInMillis
            }
        }

        val usagePrediction = if (opType.intervalValue != null && trend != null && trend > 0) {
            val lastValue = lastLog.value ?: 0.0
            val targetValue = lastValue + opType.intervalValue
            val lastEqLog = maintenanceLogDao.getLastValueLogForEquipment(equipment.id)
            val currentUsage = lastEqLog?.value ?: 0.0
            val remaining = targetValue - currentUsage
            if (remaining > 0) {
                val daysRemaining = remaining / trend
                (System.currentTimeMillis() + (daysRemaining * 24 * 60 * 60 * 1000).toLong())
            } else now
        } else null

        val nextDate = when {
            datePrediction != null && usagePrediction != null -> minOf(datePrediction, usagePrediction)
            datePrediction != null -> datePrediction
            usagePrediction != null -> usagePrediction
            else -> null
        }

        return EquipmentOperationStatus(
            equipment = equipment,
            lastLogDate = lastLog.date,
            lastLogValue = lastLog.value,
            nextPresumedDate = nextDate,
            isOverdue = nextDate?.let { it < now } ?: false
        )
    }

    private suspend fun calculateTrend(equipment: Equipment): Double? {
        val windowValue = equipment.usageWindow.toLong()
        val windowMs = when (equipment.usageWindowUnit) {
            TimeGranularity.MINUTES_5 -> windowValue * 5 * 60 * 1000L
            TimeGranularity.MINUTES_15 -> windowValue * 15 * 60 * 1000L
            TimeGranularity.HOURS -> windowValue * 60 * 60 * 1000L
            TimeGranularity.DAYS -> windowValue * 24 * 60 * 60 * 1000L
            TimeGranularity.WEEKS -> windowValue * 7 * 24 * 60 * 60 * 1000L
            TimeGranularity.MONTHS -> windowValue * 30 * 24 * 60 * 60 * 1000L
            TimeGranularity.YEARS -> windowValue * 365 * 24 * 60 * 60 * 1000L
        }
        val sinceDate = System.currentTimeMillis() - windowMs
        val logs = maintenanceLogDao.getLogsSince(equipment.id, sinceDate).filter { it.value != null }.sortedBy { it.date }

        val manualAvg = equipment.manualAverageValue?.let { value ->
            when (equipment.manualAverageUnit) {
                TimeGranularity.MINUTES_5 -> value * 12 * 24
                TimeGranularity.MINUTES_15 -> value * 4 * 24
                TimeGranularity.HOURS -> value * 24
                TimeGranularity.DAYS -> value
                TimeGranularity.WEEKS -> value / 7.0
                TimeGranularity.MONTHS -> value / 30.0
                TimeGranularity.YEARS -> value / 365.0
            }
        }

        if (logs.size < 2) return manualAvg

        var totalValueDiff = 0.0
        var totalTimeDiff = 0L
        for (i in 0 until logs.size - 1) {
            val diff = (logs[i+1].value ?: 0.0) - (logs[i].value ?: 0.0)
            if (diff >= 0) {
                totalValueDiff += diff
                totalTimeDiff += (logs[i+1].date - logs[i].date)
            }
        }
        if (totalTimeDiff <= 0) return manualAvg
        return (totalValueDiff / totalTimeDiff) * (24 * 60 * 60 * 1000.0)
    }

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

    fun addOperationType(
        description: String, 
        imageIdentifier: ImageIdentifier?,
        isPredictable: Boolean = false,
        intervalValue: Double? = null,
        timeoutValue: Int? = null,
        timeoutUnit: TimeGranularity? = null,
        visibilityHorizon: Int = 30,
        visibilityHorizonUnit: TimeGranularity = TimeGranularity.DAYS
    ) {
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
                        displayOrder = nextOrder,
                        isPredictable = isPredictable,
                        intervalValue = intervalValue,
                        timeoutValue = timeoutValue,
                        timeoutUnit = timeoutUnit,
                        visibilityHorizon = visibilityHorizon,
                        visibilityHorizonUnit = visibilityHorizonUnit
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
