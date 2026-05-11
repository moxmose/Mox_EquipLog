package com.moxmose.moxequiplog.ui.operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.ImageRepository
import com.moxmose.moxequiplog.data.MaintenanceManager
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.Equipment
import com.moxmose.moxequiplog.data.local.EquipmentDao
import com.moxmose.moxequiplog.data.local.Image
import com.moxmose.moxequiplog.data.local.ImageIdentifier
import com.moxmose.moxequiplog.data.local.MaintenanceLog
import com.moxmose.moxequiplog.data.local.MaintenanceLogDao
import com.moxmose.moxequiplog.data.local.MaintenanceReminderDao
import com.moxmose.moxequiplog.data.local.OperationType
import com.moxmose.moxequiplog.data.local.OperationTypeDao
import com.moxmose.moxequiplog.data.local.TimeGranularity
import com.moxmose.moxequiplog.utils.AppConstants
import com.moxmose.moxequiplog.utils.UiConstants
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EquipmentOperationStatus(
    val equipment: Equipment,
    val lastLogDate: Long?,
    val lastLogValue: Double?,
    val nextPresumedDate: Long?,
    val isOverdue: Boolean,
    val isPlanned: Boolean = false,
    val reminderId: Int? = null,
    val plannedValue: Double? = null,
    val predictedDate: Long? = null
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
    private val maintenanceLogDao: MaintenanceLogDao,
    private val maintenanceReminderDao: MaintenanceReminderDao,
    private val maintenanceManager: MaintenanceManager
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

    // Hoisted UI State from Screen
    private val _showDismissed = MutableStateFlow(false)
    val showDismissed = _showDismissed.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog = _showAddDialog.asStateFlow()

    private val _selectedAffectedEquipmentForAdd = MutableStateFlow<Pair<Int, EquipmentOperationStatus>?>(null)
    val selectedAffectedEquipmentForAdd = _selectedAffectedEquipmentForAdd.asStateFlow()

    fun onToggleShowDismissed() { _showDismissed.value = !_showDismissed.value }
    fun onShowAddDialogChange(show: Boolean) { _showAddDialog.value = show }
    fun onAffectedAction(opId: Int, status: EquipmentOperationStatus?) { _selectedAffectedEquipmentForAdd.value = if (status != null) opId to status else null }

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
        equipmentDao.getActiveEquipments(),
        maintenanceReminderDao.getAllReminders(),
        maintenanceLogDao.getLogsCountFlow()
    ) { opTypes, equipments, reminders, _ ->
        val statuses = mutableMapOf<Int, OperationGlobalStatus>()
        val now = System.currentTimeMillis()
        
        opTypes.filter { it.isPredictable }.forEach { opType ->
            val affected = equipments.mapNotNull { equipment ->
                val lastLog = maintenanceLogDao.getLastLogForEquipmentAndOperation(equipment.id, opType.id)
                val trend = maintenanceManager.calculateTrend(equipment)
                val nextPresumedDate = if (lastLog != null) maintenanceManager.getOperationPrediction(opType, lastLog, trend) else null
                
                val prediction = nextPresumedDate?.let {
                    if (lastLog != null) {
                        calculateEquipmentStatusForOp(equipment, opType, lastLog).copy(
                            nextPresumedDate = it,
                            isOverdue = it < now
                        )
                    } else null
                }

                // Check for manual reminder first (Planned)
                val manualReminder = reminders.find { !it.isCompleted && it.equipmentId == equipment.id && it.operationTypeId == opType.id }
                
                // Effective horizon check
                val horizonValue = if (opType.useCustomVisibilityHorizon) opType.visibilityHorizon else globalVisibilityHorizonValue.value
                val horizonUnit = if (opType.useCustomVisibilityHorizon) opType.visibilityHorizonUnit else globalVisibilityHorizonUnit.value
                val horizonLimit = now + getHorizonMs(horizonValue.toLong(), horizonUnit)

                if (manualReminder != null) {
                    val effectiveDate = manualReminder.dueDate ?: manualReminder.presumedDate
                    val isWithinHorizon = effectiveDate == null || effectiveDate <= horizonLimit || effectiveDate < now
                    
                    if (isWithinHorizon) {
                        return@mapNotNull EquipmentOperationStatus(
                            equipment = equipment,
                            lastLogDate = lastLog?.date,
                            lastLogValue = lastLog?.value,
                            nextPresumedDate = effectiveDate,
                            isOverdue = effectiveDate?.let { it < now } ?: false,
                            isPlanned = true,
                            reminderId = manualReminder.id,
                            plannedValue = manualReminder.dueValue,
                            predictedDate = prediction?.nextPresumedDate
                        )
                    } else if (prediction != null && (prediction.isOverdue || (prediction.nextPresumedDate != null && prediction.nextPresumedDate <= horizonLimit))) {
                        // Planned is far, but prediction is near/overdue -> show prediction as a warning
                        return@mapNotNull prediction
                    }
                } else if (prediction != null && (prediction.isOverdue || (prediction.nextPresumedDate != null && prediction.nextPresumedDate <= horizonLimit))) {
                    // No manual reminder, show prediction if near/overdue
                    return@mapNotNull prediction
                }
                null
            }
            statuses[opType.id] = OperationGlobalStatus(opType.id, affected)
        }
        statuses
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), emptyMap())

    private fun getHorizonMs(value: Long, unit: TimeGranularity): Long {
        return when (unit) {
            TimeGranularity.MINUTES_5 -> value * 5 * 60 * 1000L
            TimeGranularity.MINUTES_15 -> value * 15 * 60 * 1000L
            TimeGranularity.HOURS -> value * 60 * 60 * 1000L
            TimeGranularity.DAYS -> value * AppConstants.MS_PER_DAY
            TimeGranularity.WEEKS -> value * 7 * AppConstants.MS_PER_DAY
            TimeGranularity.MONTHS -> value * 30 * AppConstants.MS_PER_DAY
            TimeGranularity.YEARS -> value * 365 * AppConstants.MS_PER_DAY
        }
    }

    private suspend fun calculateEquipmentStatusForOp(equipment: Equipment, opType: OperationType, lastLog: MaintenanceLog): EquipmentOperationStatus {
        val now = System.currentTimeMillis()
        val trend = maintenanceManager.calculateTrend(equipment)
        val nextDate = maintenanceManager.getOperationPrediction(opType, lastLog, trend)

        return EquipmentOperationStatus(
            equipment = equipment,
            lastLogDate = lastLog.date,
            lastLogValue = lastLog.value,
            nextPresumedDate = nextDate,
            isOverdue = nextDate?.let { it < now } ?: false
        )
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

    val globalVisibilityHorizonValue: StateFlow<Int> = appSettingsManager.defaultVisibilityHorizonValue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), UiConstants.DEFAULT_VISIBILITY_HORIZON_VALUE)

    val globalVisibilityHorizonUnit: StateFlow<TimeGranularity> = appSettingsManager.defaultVisibilityHorizonUnit
        .map { TimeGranularity.valueOf(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), TimeGranularity.valueOf(UiConstants.DEFAULT_VISIBILITY_HORIZON_UNIT))

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
        visibilityHorizonUnit: TimeGranularity = TimeGranularity.DAYS,
        useCustomVisibilityHorizon: Boolean = false,
        estimatedCost: Double? = null
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
                        visibilityHorizonUnit = visibilityHorizonUnit,
                        useCustomVisibilityHorizon = useCustomVisibilityHorizon,
                        estimatedCost = estimatedCost
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
