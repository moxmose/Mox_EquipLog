package com.moxmose.moxequiplog.ui.equipments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.ImageRepository
import com.moxmose.moxequiplog.data.MaintenanceManager
import com.moxmose.moxequiplog.data.local.*
import com.moxmose.moxequiplog.utils.AppConstants
import com.moxmose.moxequiplog.utils.UiConstants
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class OperationStatus(
    val operation: OperationType,
    val lastLogDate: Long?,
    val lastLogValue: Double?,
    val nextPresumedDate: Long?,
    val isOverdue: Boolean,
    val isPlanned: Boolean = false,
    val reminderId: Int? = null,
    val plannedValue: Double? = null,
    val predictedDate: Long? = null
)

data class EquipmentHealth(
    val lastRecordedValue: Double?,
    val lastRecordedDate: Long?,
    val estimatedCurrentValue: Double?
)

data class EquipmentStatus(
    val equipmentId: Int,
    val health: EquipmentHealth,
    val operationStatuses: List<OperationStatus>
)

class EquipmentsViewModel(
    private val equipmentDao: EquipmentDao,
    private val imageRepository: ImageRepository,
    private val appSettingsManager: AppSettingsManager,
    private val measurementUnitDao: MeasurementUnitDao,
    private val operationTypeDao: OperationTypeDao,
    private val maintenanceLogDao: MaintenanceLogDao,
    private val maintenanceReminderDao: MaintenanceReminderDao,
    private val maintenanceManager: MaintenanceManager
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

    // Hoisted UI State from Screen
    private val _showDismissed = MutableStateFlow(false)
    val showDismissed = _showDismissed.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog = _showAddDialog.asStateFlow()

    private val _selectedPredictionForAdd = MutableStateFlow<Pair<Int, OperationStatus>?>(null)
    val selectedPredictionForAdd = _selectedPredictionForAdd.asStateFlow()

    private val _selectedPlannedForEdit = MutableStateFlow<Pair<Int, OperationStatus>?>(null)
    val selectedPlannedForEdit = _selectedPlannedForEdit.asStateFlow()

    fun onToggleShowDismissed() { _showDismissed.value = !_showDismissed.value }
    fun onShowAddDialogChange(show: Boolean) { _showAddDialog.value = show }
    fun onPredictionAction(eqId: Int, status: OperationStatus?) { _selectedPredictionForAdd.value = if (status != null) eqId to status else null }
    fun onPlannedAction(eqId: Int, status: OperationStatus?) { _selectedPlannedForEdit.value = if (status != null) eqId to status else null }

    val activeEquipments: StateFlow<List<Equipment>> = equipmentDao.getActiveEquipments()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT),
            initialValue = emptyList()
        )

    val allEquipments: StateFlow<List<Equipment>> = equipmentDao.getAllEquipments()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT),
            initialValue = emptyList()
        )

    val equipmentStatuses: StateFlow<Map<Int, EquipmentStatus>> = combine(
        activeEquipments,
        operationTypeDao.getAllOperationTypes(),
        maintenanceReminderDao.getAllReminders(),
        maintenanceLogDao.getLogsCountFlow()
    ) { equipments, opTypes, reminders, _ ->
        val statuses = mutableMapOf<Int, EquipmentStatus>()
        equipments.forEach { equipment ->
            statuses[equipment.id] = calculateEquipmentStatus(equipment, opTypes, reminders)
        }
        statuses
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private suspend fun calculateEquipmentStatus(
        equipment: Equipment, 
        opTypes: List<OperationType>, 
        reminders: List<MaintenanceReminder>
    ): EquipmentStatus {
        val lastValueLog = maintenanceLogDao.getLastValueLogForEquipment(equipment.id)
        val trend = maintenanceManager.calculateTrend(equipment)
        val now = System.currentTimeMillis()

        val health = if (lastValueLog != null) {
            val daysSince = (now - lastValueLog.date).toDouble() / (24 * 60 * 60 * 1000.0)
            val estimatedCurrent = if (trend != null) (lastValueLog.value ?: 0.0) + (daysSince * trend) else null
            EquipmentHealth(lastValueLog.value, lastValueLog.date, estimatedCurrent)
        } else {
            EquipmentHealth(null, null, null)
        }

        val horizonMs = getHorizonMs(equipment.visibilityHorizon.toLong(), equipment.visibilityHorizonUnit)
        val horizonLimit = now + horizonMs

        val opStatuses = opTypes
            .filter { it.isPredictable && !it.dismissed }
            .mapNotNull { opType ->
                val lastLogForOp = maintenanceLogDao.getLastLogForEquipmentAndOperation(equipment.id, opType.id)
                val nextPresumedDate = if (lastLogForOp != null) maintenanceManager.getOperationPrediction(opType, lastLogForOp, trend) else null
                
                val prediction = nextPresumedDate?.let {
                    OperationStatus(
                        operation = opType,
                        lastLogDate = lastLogForOp?.date,
                        lastLogValue = lastLogForOp?.value,
                        nextPresumedDate = it,
                        isOverdue = it < now,
                        isPlanned = false
                    )
                }
                
                // Check if there's a manual reminder (Planned)
                val manualReminder = reminders.find { !it.isCompleted && it.equipmentId == equipment.id && it.operationTypeId == opType.id }
                
                if (manualReminder != null) {
                    val effectiveDate = manualReminder.dueDate ?: manualReminder.presumedDate
                    val isWithinHorizon = effectiveDate == null || effectiveDate <= horizonLimit || effectiveDate < now
                    
                    if (isWithinHorizon) {
                        return@mapNotNull OperationStatus(
                            operation = opType,
                            lastLogDate = lastLogForOp?.date,
                            lastLogValue = lastLogForOp?.value,
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

        return EquipmentStatus(equipment.id, health, opStatuses)
    }

    private fun getHorizonMs(value: Long, unit: TimeGranularity): Long {
        return when (unit) {
            TimeGranularity.MINUTES_5 -> value * 5 * 60 * 1000L
            TimeGranularity.MINUTES_15 -> value * 15 * 60 * 1000L
            TimeGranularity.HOURS -> value * 60 * 60 * 1000L
            TimeGranularity.DAYS -> value * 24 * 60 * 60 * 1000L
            TimeGranularity.WEEKS -> value * 7 * 24 * 60 * 60 * 1000L
            TimeGranularity.MONTHS -> value * 30 * 24 * 60 * 60 * 1000L
            TimeGranularity.YEARS -> value * 365 * 24 * 60 * 60 * 1000L
        }
    }

    val measurementUnits: StateFlow<List<MeasurementUnit>> = measurementUnitDao.getAllUnits()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT),
            initialValue = emptyList()
        )

    val equipmentImages: StateFlow<List<Image>> = imageRepository.getImagesByCategory(Category.EQUIPMENT)
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

    val categoryColor: StateFlow<String> = imageRepository.getCategoryColor(Category.EQUIPMENT)
        .map { it ?: UiConstants.DEFAULT_FALLBACK_COLOR }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), UiConstants.DEFAULT_FALLBACK_COLOR)

    val categoryDefaultIcon: StateFlow<String?> = imageRepository.getCategoryDefaultIcon(Category.EQUIPMENT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), null)

    val categoryDefaultPhoto: StateFlow<String?> = imageRepository.getCategoryDefaultPhoto(Category.EQUIPMENT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), null)

    val defaultEquipmentId: StateFlow<Int?> = appSettingsManager.defaultEquipmentId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), null)
        
    val defaultUnitId: StateFlow<Int?> = appSettingsManager.defaultUnitId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), null)

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

    fun addEquipment(
        description: String, 
        imageIdentifier: ImageIdentifier?, 
        unitId: Int, 
        isResettable: Boolean = false, 
        usageWindow: Int = 30, 
        usageWindowUnit: TimeGranularity = TimeGranularity.DAYS,
        manualAverageValue: Double? = null,
        manualAverageUnit: TimeGranularity = TimeGranularity.DAYS,
        visibilityHorizon: Int = 30,
        visibilityHorizonUnit: TimeGranularity = TimeGranularity.DAYS
    ) {
        if (description.isBlank()) {
            viewModelScope.launch { _uiEvents.send(UiEvent.DescriptionInvalid) }
            return
        }
        viewModelScope.launch {
            try {
                val currentList = allEquipments.value
                val nextOrder = if (currentList.isEmpty()) 0 else currentList.maxOf { it.displayOrder } + 1
                
                var equipmentPhotoUri: String? = null
                var equipmentIconIdentifier: String? = null

                when (imageIdentifier) {
                    is ImageIdentifier.Icon -> equipmentIconIdentifier = imageIdentifier.name
                    is ImageIdentifier.Photo -> equipmentPhotoUri = imageIdentifier.uri
                    null -> {
                        equipmentPhotoUri = categoryDefaultPhoto.value
                        equipmentIconIdentifier = categoryDefaultIcon.value
                    }
                }

                equipmentDao.insertEquipment(
                    Equipment(
                        description = description,
                        photoUri = equipmentPhotoUri,
                        iconIdentifier = equipmentIconIdentifier,
                        displayOrder = nextOrder,
                        unitId = unitId,
                        isResettable = isResettable,
                        usageWindow = usageWindow,
                        usageWindowUnit = usageWindowUnit,
                        manualAverageValue = manualAverageValue,
                        manualAverageUnit = manualAverageUnit,
                        visibilityHorizon = visibilityHorizon,
                        visibilityHorizonUnit = visibilityHorizonUnit
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
            _uiEvents.trySend(UiEvent.DatabaseCheckFailed)
            true
        }
    }
}
