package com.moxmose.moxequiplog.ui.equipment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.ImageRepository
import com.moxmose.moxequiplog.data.MaintenanceManager
import com.moxmose.moxequiplog.data.SectionRepository
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.Equipment
import com.moxmose.moxequiplog.data.local.EquipmentDao
import com.moxmose.moxequiplog.data.local.EquipmentDraft
import com.moxmose.moxequiplog.data.local.Image
import com.moxmose.moxequiplog.data.local.ImageIdentifier
import com.moxmose.moxequiplog.data.local.MaintenanceLogDao
import com.moxmose.moxequiplog.data.local.MaintenanceReminder
import com.moxmose.moxequiplog.data.local.MaintenanceReminderDao
import com.moxmose.moxequiplog.data.local.MeasurementUnit
import com.moxmose.moxequiplog.data.local.MeasurementUnitDao
import com.moxmose.moxequiplog.data.local.OperationType
import com.moxmose.moxequiplog.data.local.OperationTypeDao
import com.moxmose.moxequiplog.data.local.Section
import com.moxmose.moxequiplog.data.local.TimeGranularity
import com.moxmose.moxequiplog.utils.AppConstants
import com.moxmose.moxequiplog.utils.UiConstants
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OperationStatus(
    val operation: OperationType,
    val lastLogDate: Long?,
    val lastLogValue: Double?,
    val nextPresumedDate: Long?,
    val isOverdue: Boolean,
    val isPlanned: Boolean = false,
    val reminderId: Int? = null,
    val plannedValue: Double? = null,
    val predictedDate: Long? = null,
    val equipmentSectionId: Int? = null,
    val equipmentSectionName: String? = null,
    val equipmentSectionColor: String? = null,
    val operationSectionId: Int? = null,
    val operationSectionName: String? = null,
    val operationSectionColor: String? = null
)

data class EquipmentHealth(
    val lastRecordedValue: Double?,
    val lastRecordedDate: Long?,
    val estimatedCurrentValue: Double?,
    val currentSessionValue: Double? = null,
    val currentSessionEstimated: Double? = null
)

data class EquipmentStatus(
    val equipmentId: Int,
    val health: EquipmentHealth,
    val operationStatuses: List<OperationStatus>
)

class EquipmentViewModel(
    private val equipmentDao: EquipmentDao,
    private val imageRepository: ImageRepository,
    private val appSettingsManager: AppSettingsManager,
    private val sectionRepository: SectionRepository,
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
        data object UpdateEquipmentOrderFailed : UiEvent()
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

    private val _cloningEquipment = MutableStateFlow<Equipment?>(null)
    val cloningEquipment = _cloningEquipment.asStateFlow()

    private val _selectedPredictionForAdd = MutableStateFlow<Pair<Int, OperationStatus>?>(null)
    val selectedPredictionForAdd = _selectedPredictionForAdd.asStateFlow()

    private val _selectedPlannedForEdit = MutableStateFlow<Pair<Int, OperationStatus>?>(null)
    val selectedPlannedForEdit = _selectedPlannedForEdit.asStateFlow()

    fun onToggleShowDismissed() { _showDismissed.value = !_showDismissed.value }
    fun onShowAddDialogChange(show: Boolean) { 
        _showAddDialog.value = show 
        if (!show) {
            _cloningEquipment.value = null
            cancelAddDraft()
        }
    }
    fun onCloneEquipment(equipment: Equipment?) {
        _cloningEquipment.value = equipment
        if (equipment != null) {
            _showAddDialog.value = true
            updateAddDraft(EquipmentDraft(equipment = equipment.copy(id = 0), isDefault = false))
        }
    }
    fun onPredictionAction(eqId: Int, status: OperationStatus?) { _selectedPredictionForAdd.value = if (status != null) eqId to status else null }
    fun onPlannedAction(eqId: Int, status: OperationStatus?) { _selectedPlannedForEdit.value = if (status != null) eqId to status else null }

    val selectedSectionId: StateFlow<Int> = appSettingsManager.selectedSectionId
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT),
            initialValue = AppConstants.DEFAULT_SECTION_ID
        )

    val allSections: StateFlow<List<Section>> = sectionRepository.allSections
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT),
            initialValue = emptyList()
        )

    fun onSectionSelected(sectionId: Int) {
        viewModelScope.launch {
            appSettingsManager.setSelectedSectionId(sectionId)
        }
    }

    fun onToggleShowDismissedSections() {
        viewModelScope.launch {
            val current = showDismissedSections.value
            appSettingsManager.setShowDismissedSections(!current)
        }
    }

    val activeEquipments: StateFlow<List<Equipment>> = appSettingsManager.selectedSectionId
        .flatMapLatest { sectionId ->
            if (sectionId == AppConstants.ALL_SECTIONS_ID) {
                equipmentDao.getActiveEquipmentList()
            } else {
                equipmentDao.getActiveEquipmentListBySection(sectionId)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT),
            initialValue = emptyList()
        )

    val allEquipments: StateFlow<List<Equipment>> = appSettingsManager.selectedSectionId
        .flatMapLatest { sectionId ->
            if (sectionId == AppConstants.ALL_SECTIONS_ID) {
                equipmentDao.getAllEquipmentList()
            } else {
                equipmentDao.getAllEquipmentListBySection(sectionId)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT),
            initialValue = emptyList()
        )

    private val _allActiveOperationTypes: StateFlow<List<OperationType>> = operationTypeDao.getActiveOperationTypes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT),
            initialValue = emptyList()
        )

    val equipmentStatuses: StateFlow<Map<Int, EquipmentStatus>> = combine(
        activeEquipments,
        _allActiveOperationTypes,
        maintenanceReminderDao.getAllReminders(),
        maintenanceLogDao.getLogsCountFlow()
    ) { equipments, opTypes, reminders, _ ->
        val statuses = mutableMapOf<Int, EquipmentStatus>()
        equipments.forEach { equipment ->
            statuses[equipment.id] = calculateEquipmentStatus(equipment, opTypes, reminders)
        }
        statuses
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), emptyMap())

    private suspend fun calculateEquipmentStatus(
        equipment: Equipment, 
        opTypes: List<OperationType>, 
        reminders: List<MaintenanceReminder>
    ): EquipmentStatus {
        val lastValueLog = maintenanceLogDao.getLastValueLogForEquipment(equipment.id)
        val trend = maintenanceManager.calculateTrend(equipment)
        val now = System.currentTimeMillis()

        val health = if (lastValueLog != null) {
            val daysSince = (now - lastValueLog.date).toDouble() / AppConstants.MS_PER_DAY
            val estimatedCurrent = if (trend != null) (lastValueLog.value ?: 0.0) + (daysSince * trend) else null
            
            // Gestione sessione (reset UdM)
            val sessionValue = if (lastValueLog.resetAfter) 0.0 else lastValueLog.value
            val sessionEstimated = if (sessionValue != null && trend != null) sessionValue + (daysSince * trend) else sessionValue

            EquipmentHealth(
                lastRecordedValue = lastValueLog.value,
                lastRecordedDate = lastValueLog.date,
                estimatedCurrentValue = estimatedCurrent,
                currentSessionValue = sessionValue,
                currentSessionEstimated = sessionEstimated
            )
        } else {
            EquipmentHealth(null, null, null)
        }

        val horizonValue = if (equipment.useCustomVisibilityHorizon) equipment.visibilityHorizon else globalVisibilityHorizonValue.value
        val horizonUnit = if (equipment.useCustomVisibilityHorizon) equipment.visibilityHorizonUnit else globalVisibilityHorizonUnit.value
        val horizonMs = getHorizonMs(horizonValue.toLong(), horizonUnit)
        val horizonLimit = now + horizonMs

        val opStatuses = opTypes
            .filter { it.isPredictable && !it.dismissed }
            .mapNotNull { opType ->
                val lastLogForOp = maintenanceLogDao.getLastLogForEquipmentAndOperation(equipment.id, opType.id)
                val nextPresumedDate = if (lastLogForOp != null) maintenanceManager.getOperationPrediction(equipment.id, opType, lastLogForOp, trend) else null
                
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
                
                // Effective horizon check for operation (might have its own custom horizon)
                val opHorizonValue = if (opType.useCustomVisibilityHorizon) opType.visibilityHorizon else globalVisibilityHorizonValue.value
                val opHorizonUnit = if (opType.useCustomVisibilityHorizon) opType.visibilityHorizonUnit else globalVisibilityHorizonUnit.value
                val opHorizonMs = getHorizonMs(opHorizonValue.toLong(), opHorizonUnit)
                val opHorizonLimit = now + opHorizonMs

                // Check if there's a manual reminder (Planned)
                val manualReminder = reminders.find { !it.isCompleted && it.equipmentId == equipment.id && it.operationTypeId == opType.id }
                
                if (manualReminder != null) {
                    val effectiveDate = manualReminder.dueDate ?: manualReminder.presumedDate
                    val isWithinHorizon = effectiveDate == null || effectiveDate <= opHorizonLimit || effectiveDate < now
                    
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
                    } else if (prediction != null) {
                        // Se la manutenzione pianificata è lontana ma la predizione dice che serve ora,
                        // mostriamo la predizione (che sarà rossa perché scaduta)
                        return@mapNotNull prediction
                    }
                } else if (prediction != null) {
                    // Senza reminder manuale, mostriamo sempre la predizione (il filtro temporale lo fa l'app chiamante o la logica di business)
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
            TimeGranularity.DAYS -> value * AppConstants.MS_PER_DAY
            TimeGranularity.WEEKS -> value * 7 * AppConstants.MS_PER_DAY
            TimeGranularity.MONTHS -> value * 30 * AppConstants.MS_PER_DAY
            TimeGranularity.YEARS -> value * 365 * AppConstants.MS_PER_DAY
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

    val defaultEquipmentId: StateFlow<Int?> = combine(
        selectedSectionId,
        allSections
    ) { sectionId, sections ->
        if (sectionId == AppConstants.ALL_SECTIONS_ID) null
        else sections.find { it.id == sectionId }?.defaultEquipmentId
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), null)

    val showDismissedSections: StateFlow<Boolean> = appSettingsManager.showDismissedSections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), false)

    val sectionSelectorType: StateFlow<String> = appSettingsManager.sectionSelectorType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), UiConstants.DEFAULT_SECTION_SELECTOR_TYPE)

    val defaultUnitId: StateFlow<Int?> = combine(
        selectedSectionId,
        allSections,
        appSettingsManager.defaultUnitId
    ) { sectionId, sections, globalDefault ->
        if (sectionId == AppConstants.ALL_SECTIONS_ID) globalDefault
        else sections.find { it.id == sectionId }?.defaultUnitId ?: globalDefault
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), null)

    val globalVisibilityHorizonValue: StateFlow<Int> = appSettingsManager.defaultVisibilityHorizonValue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), UiConstants.DEFAULT_VISIBILITY_HORIZON_VALUE)

    val globalVisibilityHorizonUnit: StateFlow<TimeGranularity> = appSettingsManager.defaultVisibilityHorizonUnit
        .map { TimeGranularity.valueOf(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), TimeGranularity.valueOf(UiConstants.DEFAULT_VISIBILITY_HORIZON_UNIT))

    val allDrafts: StateFlow<Map<Int, EquipmentDraft>> = appSettingsManager.getAllDraftsFlow("equipment")
        .map { draftsMap ->
            draftsMap.mapValues { (_, json) ->
                try {
                    Json.decodeFromString<EquipmentDraft>(json)
                } catch (e: Exception) {
                    null
                }
            }.filterValues { it != null }.mapValues { it.value!! }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), emptyMap())

    val addDraft: StateFlow<EquipmentDraft?> = appSettingsManager.getDraftFlow("equipment", 0)
        .map { json ->
            try {
                json?.let { Json.decodeFromString<EquipmentDraft>(it) }
            } catch (e: Exception) {
                null
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), null)

    fun setDefaultEquipment(id: Int?) {
        val sectionId = selectedSectionId.value
        if (sectionId == AppConstants.ALL_SECTIONS_ID) return
        
        viewModelScope.launch {
            try {
                sectionRepository.updateSectionDefaultEquipment(sectionId, id)
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.SetDefaultFailed)
            }
        }
    }

    fun toggleDefaultEquipment(id: Int) {
        val sectionId = selectedSectionId.value
        if (sectionId == AppConstants.ALL_SECTIONS_ID) return

        viewModelScope.launch {
            try {
                val currentDefault = defaultEquipmentId.value
                if (currentDefault == id) {
                    sectionRepository.updateSectionDefaultEquipment(sectionId, null)
                } else {
                    sectionRepository.updateSectionDefaultEquipment(sectionId, id)
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
        sectionId: Int? = null,
        usageWindow: Int = 30, 
        usageWindowUnit: TimeGranularity = TimeGranularity.DAYS,
        manualAverageValue: Double? = null,
        manualAverageUnit: TimeGranularity = TimeGranularity.DAYS,
        visibilityHorizon: Int = 30,
        visibilityHorizonUnit: TimeGranularity = TimeGranularity.DAYS,
        useCustomUsageWindow: Boolean = false,
        useCustomVisibilityHorizon: Boolean = false
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

                val targetSectionId = sectionId ?: run {
                    val currentSection = selectedSectionId.value
                    if (currentSection == AppConstants.ALL_SECTIONS_ID) AppConstants.DEFAULT_SECTION_ID else currentSection
                }

                equipmentDao.insertEquipment(
                    Equipment(
                        description = description,
                        photoUri = equipmentPhotoUri,
                        iconIdentifier = equipmentIconIdentifier,
                        displayOrder = nextOrder,
                        unitId = unitId,
                        sectionId = targetSectionId,
                        usageWindow = usageWindow,
                        usageWindowUnit = usageWindowUnit,
                        manualAverageValue = manualAverageValue,
                        manualAverageUnit = manualAverageUnit,
                        visibilityHorizon = visibilityHorizon,
                        visibilityHorizonUnit = visibilityHorizonUnit,
                        useCustomUsageWindow = useCustomUsageWindow,
                        useCustomVisibilityHorizon = useCustomVisibilityHorizon
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
                equipmentDao.updateEquipmentList(equipments)
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.UpdateEquipmentOrderFailed)
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

    fun deleteEquipment(equipment: Equipment) {
        viewModelScope.launch {
            try {
                equipmentDao.deleteEquipment(equipment)
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.UpdateEquipmentFailed)
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
            equipmentDao.countEquipmentUsingPhoto(uri) > 0
        } catch (e: Exception) {
            _uiEvents.trySend(UiEvent.DatabaseCheckFailed)
            true
        }
    }

    // --- Draft Management ---
    fun getEquipmentDraft(id: Int): Flow<EquipmentDraft?> {
        return appSettingsManager.getDraftFlow("equipment", id).map { json ->
            try {
                json?.let { Json.decodeFromString<EquipmentDraft>(it) }
            } catch (e: Exception) {
                null
            }
        }
    }

    fun startEditing(equipment: Equipment) {
        viewModelScope.launch {
            val section = allSections.value.find { it.id == equipment.sectionId }
            val isCurrentlyDefault = section?.defaultEquipmentId == equipment.id
            val draft = EquipmentDraft(equipment = equipment, isDefault = isCurrentlyDefault)
            val json = Json.encodeToString(draft)
            appSettingsManager.saveDraft("equipment", equipment.id, json)
        }
    }

    fun toggleDefaultInDraft(id: Int) {
        viewModelScope.launch {
            val drafts = allDrafts.value
            drafts[id]?.let { draft ->
                val updated = draft.copy(isDefault = !draft.isDefault)
                updateDraft(updated)
            }
        }
    }

    fun updateDraft(draft: EquipmentDraft) {
        viewModelScope.launch {
            val json = Json.encodeToString(draft)
            appSettingsManager.saveDraft("equipment", draft.equipment.id, json)
        }
    }

    fun cancelEditing(id: Int) {
        viewModelScope.launch {
            appSettingsManager.deleteDraft("equipment", id)
        }
    }

    fun saveEditing(draft: EquipmentDraft) {
        viewModelScope.launch {
            updateEquipment(draft.equipment)
            
            // Sync default status if changed in draft
            val sectionId = draft.equipment.sectionId
            val section = allSections.value.find { it.id == sectionId }
            val currentDefaultId = section?.defaultEquipmentId
            
            if (draft.isDefault && currentDefaultId != draft.equipment.id) {
                sectionRepository.updateSectionDefaultEquipment(sectionId, draft.equipment.id)
            } else if (!draft.isDefault && currentDefaultId == draft.equipment.id) {
                sectionRepository.updateSectionDefaultEquipment(sectionId, null)
            }
            
            appSettingsManager.deleteDraft("equipment", draft.equipment.id)
        }
    }

    // --- Add Draft Management ---
    fun updateAddDraft(draft: EquipmentDraft) {
        viewModelScope.launch {
            val json = Json.encodeToString(draft)
            appSettingsManager.saveDraft("equipment", 0, json)
        }
    }

    fun cancelAddDraft() {
        viewModelScope.launch {
            appSettingsManager.deleteDraft("equipment", 0)
        }
    }
}
