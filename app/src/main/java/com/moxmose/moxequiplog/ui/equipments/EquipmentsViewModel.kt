package com.moxmose.moxequiplog.ui.equipments

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

data class OperationStatus(
    val operation: OperationType,
    val lastLogDate: Long?,
    val lastLogValue: Double?,
    val nextPresumedDate: Long?,
    val isOverdue: Boolean,
    val isPlanned: Boolean = false,
    val reminderId: Int? = null
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
    private val maintenanceReminderDao: MaintenanceReminderDao
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
        maintenanceReminderDao.getAllReminders() // To distinguish planned vs predicted
    ) { equipments, opTypes, reminders ->
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
        val trend = calculateTrend(equipment)
        val now = System.currentTimeMillis()

        val health = if (lastValueLog != null) {
            val daysSince = (now - lastValueLog.date).toDouble() / (24 * 60 * 60 * 1000.0)
            val estimatedCurrent = if (trend != null) (lastValueLog.value ?: 0.0) + (daysSince * trend) else null
            EquipmentHealth(lastValueLog.value, lastValueLog.date, estimatedCurrent)
        } else {
            EquipmentHealth(null, null, null)
        }

        val horizonMs = when (equipment.visibilityHorizonUnit) {
            TimeGranularity.MINUTES_5 -> equipment.visibilityHorizon * 5 * 60 * 1000L
            TimeGranularity.MINUTES_15 -> equipment.visibilityHorizon * 15 * 60 * 1000L
            TimeGranularity.HOURS -> equipment.visibilityHorizon * 60 * 60 * 1000L
            TimeGranularity.DAYS -> equipment.visibilityHorizon * 24 * 60 * 60 * 1000L
            TimeGranularity.WEEKS -> equipment.visibilityHorizon * 7 * 24 * 60 * 60 * 1000L
            TimeGranularity.MONTHS -> equipment.visibilityHorizon * 30 * 24 * 60 * 60 * 1000L
            TimeGranularity.YEARS -> equipment.visibilityHorizon * 365 * 24 * 60 * 60 * 1000L
        }
        val horizonLimit = now + horizonMs

        val opStatuses = opTypes
            .filter { it.isPredictable && !it.dismissed }
            .mapNotNull { opType ->
                // Check if there's a manual reminder (Planned)
                val manualReminder = reminders.find { !it.isCompleted && it.equipmentId == equipment.id && it.operationTypeId == opType.id }
                
                if (manualReminder != null) {
                    // It's a PLANNED event
                    val effectiveDate = manualReminder.dueDate ?: manualReminder.presumedDate
                    if (effectiveDate == null || effectiveDate <= horizonLimit || effectiveDate < now) {
                        return@mapNotNull OperationStatus(
                            operation = opType,
                            lastLogDate = null, // Not strictly needed for UI display of event
                            lastLogValue = null,
                            nextPresumedDate = effectiveDate,
                            isOverdue = effectiveDate?.let { it < now } ?: false,
                            isPlanned = true,
                            reminderId = manualReminder.id
                        )
                    }
                    return@mapNotNull null
                }

                // Pertinence check for PREDICTED event
                val lastLogForOp = maintenanceLogDao.getLastLogForEquipmentAndOperation(equipment.id, opType.id)
                if (lastLogForOp == null) return@mapNotNull null

                val status = calculateOperationPrediction(equipment, opType, lastLogForOp, trend)
                
                if (status.isOverdue || (status.nextPresumedDate != null && status.nextPresumedDate <= horizonLimit)) {
                    status
                } else null
            }

        return EquipmentStatus(equipment.id, health, opStatuses)
    }

    private suspend fun calculateOperationPrediction(
        equipment: Equipment, 
        opType: OperationType, 
        lastLog: MaintenanceLog, 
        trend: Double?
    ): OperationStatus {
        val now = System.currentTimeMillis()

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

        return OperationStatus(
            operation = opType,
            lastLogDate = lastLog.date,
            lastLogValue = lastLog.value,
            nextPresumedDate = nextDate,
            isOverdue = nextDate?.let { it < now } ?: false,
            isPlanned = false
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
