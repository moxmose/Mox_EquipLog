package com.moxmose.moxequiplog.ui.maintenancelog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.sqlite.db.SimpleSQLiteQuery
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.ImageRepository
import com.moxmose.moxequiplog.data.MaintenanceManager
import com.moxmose.moxequiplog.data.SectionRepository
import com.moxmose.moxequiplog.data.local.CategoryDao
import com.moxmose.moxequiplog.data.local.EquipmentDao
import com.moxmose.moxequiplog.data.local.MaintenanceLog
import com.moxmose.moxequiplog.data.local.MaintenanceLogDao
import com.moxmose.moxequiplog.data.local.MaintenanceLogDetails
import com.moxmose.moxequiplog.data.local.MaintenanceReminder
import com.moxmose.moxequiplog.data.local.MaintenanceReminderDao
import com.moxmose.moxequiplog.data.local.MaintenanceReminderDetails
import com.moxmose.moxequiplog.data.local.MeasurementUnit
import com.moxmose.moxequiplog.data.local.MeasurementUnitDao
import com.moxmose.moxequiplog.data.local.OperationTypeDao
import com.moxmose.moxequiplog.data.local.OperationType
import com.moxmose.moxequiplog.data.local.Section
import com.moxmose.moxequiplog.data.local.TimeGranularity
import com.moxmose.moxequiplog.utils.AppConstants
import com.moxmose.moxequiplog.utils.CalendarManager
import com.moxmose.moxequiplog.utils.ResourceProvider
import com.moxmose.moxequiplog.utils.UiConstants
import com.moxmose.moxequiplog.data.local.Equipment
import com.moxmose.moxequiplog.ui.equipment.OperationStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortProperty {
    DATE, EQUIPMENT, OPERATION, VALUE, NOTES
}

enum class SortDirection {
    ASCENDING, DESCENDING
}

data class MaintenanceReminderUiModel(
    val details: MaintenanceReminderDetails,
    val presumedDate: Long?,
    val effectiveDate: Long // Used for sorting: fixed date if present, otherwise presumed
)

@OptIn(ExperimentalCoroutinesApi::class)
class MaintenanceLogViewModel(
    private val maintenanceLogDao: MaintenanceLogDao,
    private val maintenanceReminderDao: MaintenanceReminderDao,
    private val equipmentDao: EquipmentDao,
    private val operationTypeDao: OperationTypeDao,
    private val categoryDao: CategoryDao,
    private val appSettingsManager: AppSettingsManager,
    private val sectionRepository: SectionRepository,
    private val imageRepository: ImageRepository,
    private val measurementUnitDao: MeasurementUnitDao,
    private val calendarManager: CalendarManager,
    private val maintenanceManager: MaintenanceManager,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    sealed class UiEvent {
        data object AddLogFailed : UiEvent()
        data object UpdateLogFailed : UiEvent()
        data object DismissLogFailed : UiEvent()
        data object RestoreLogFailed : UiEvent()
        data object DeleteLogFailed : UiEvent()
        data object DeleteReminderFailed : UiEvent()
        data object UpdateReminderFailed : UiEvent()
        data object RecalculateRemindersFailed : UiEvent()
    }

    private val _uiEvents = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvents: Flow<UiEvent> = _uiEvents.receiveAsFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _sortProperty = MutableStateFlow(SortProperty.DATE)
    private val _sortDirection = MutableStateFlow(SortDirection.DESCENDING)
    private val _showDismissed = MutableStateFlow(false)

    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val sortProperty: StateFlow<SortProperty> = _sortProperty.asStateFlow()
    val sortDirection: StateFlow<SortDirection> = _sortDirection.asStateFlow()
    val showDismissed: StateFlow<Boolean> = _showDismissed.asStateFlow()

    // Hoisted UI State from Screen
    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog = _showAddDialog.asStateFlow()

    private val _expandedCardId = MutableStateFlow<Int?>(null)
    val expandedCardId = _expandedCardId.asStateFlow()

    private val _editingCardId = MutableStateFlow<Int?>(null)
    val editingCardId = _editingCardId.asStateFlow()

    private val _selectedReminderForComplete = MutableStateFlow<MaintenanceReminderDetails?>(null)
    val selectedReminderForComplete = _selectedReminderForComplete.asStateFlow()

    private val _selectedReminderForEdit = MutableStateFlow<MaintenanceReminderDetails?>(null)
    val selectedReminderForEdit = _selectedReminderForEdit.asStateFlow()

    private val _selectedPredictionForAdd = MutableStateFlow<Pair<Int, OperationStatus>?>(null)
    val selectedPredictionForAdd = _selectedPredictionForAdd.asStateFlow()

    fun onCardExpanded(id: Int) { _expandedCardId.value = if (_expandedCardId.value == id) null else id }
    fun onEditLog(log: MaintenanceLog) { _editingCardId.value = log.id }
    fun onEditReminder(reminder: MaintenanceReminderDetails?) { _selectedReminderForEdit.value = reminder }

    fun onPredictionAction(eqId: Int, status: OperationStatus?) { 
        _selectedPredictionForAdd.value = if (status != null) eqId to status else null 
        if (status == null) {
            cancelLogAddDraft()
            cancelReminderAddDraft()
        }
    }

    fun onShowAddDialogChange(show: Boolean) { 
        _showAddDialog.value = show 
        if (!show) {
            cancelLogAddDraft()
            cancelReminderAddDraft()
        }
    }

    fun onCompleteReminder(reminder: MaintenanceReminderDetails?) { 
        _selectedReminderForComplete.value = reminder 
        if (reminder == null) {
            cancelLogAddDraft()
            cancelReminderAddDraft()
        }
    }

    val selectedSectionId: StateFlow<Int> = appSettingsManager.selectedSectionId
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT),
            initialValue = AppConstants.DEFAULT_SECTION_ID
        )

    val showDismissedSections: StateFlow<Boolean> = appSettingsManager.showDismissedSections
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT),
            initialValue = false
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val logs: StateFlow<List<MaintenanceLogDetails>> = combine(
        appSettingsManager.selectedSectionId,
        _searchQuery,
        _sortProperty,
        _sortDirection,
        _showDismissed
    ) { sectionId, query, sortProp, sortDir, showDismissedValue ->
        buildQuery(sectionId, query, sortProp, sortDir, showDismissedValue)
    }.flatMapLatest { query ->
        maintenanceLogDao.getLogsWithDetails(query)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT),
        initialValue = emptyList()
    )

    val allEquipments: StateFlow<List<Equipment>> = appSettingsManager.selectedSectionId.flatMapLatest { sectionId ->
        if (sectionId == AppConstants.ALL_SECTIONS_ID) {
            equipmentDao.getAllEquipmentList()
        } else {
            equipmentDao.getAllEquipmentListBySection(sectionId)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT),
        initialValue = emptyList()
    )

    val allOperationTypes: StateFlow<List<OperationType>> = appSettingsManager.selectedSectionId.flatMapLatest { sectionId ->
        if (sectionId == AppConstants.ALL_SECTIONS_ID) {
            operationTypeDao.getAllOperationTypes()
        } else {
            operationTypeDao.getAllOperationTypesBySection(sectionId)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT),
        initialValue = emptyList()
    )

    val activeReminders: StateFlow<List<MaintenanceReminderDetails>> = combine(
        appSettingsManager.selectedSectionId,
        appSettingsManager.costAnalysisWindowValue,
        appSettingsManager.costAnalysisWindowUnit
    ) { sectionId, value, unit ->
        val windowMs = maintenanceManager.getWindowMs(value.toLong(), TimeGranularity.valueOf(unit))
        Triple(sectionId, value, System.currentTimeMillis() - windowMs)
    }.flatMapLatest { (sectionId, _, sinceDate) ->
        if (sectionId == AppConstants.ALL_SECTIONS_ID) {
            maintenanceReminderDao.getActiveRemindersWithDetails(sinceDate)
        } else {
            maintenanceReminderDao.getActiveRemindersWithDetailsBySection(sinceDate, sectionId)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT),
        initialValue = emptyList()
    )

    // Automatic Predictions Flow
    val automaticPredictions: StateFlow<List<Pair<Equipment, OperationStatus>>> = combine(
        allEquipments,
        allOperationTypes,
        maintenanceReminderDao.getAllReminders(),
        appSettingsManager.defaultVisibilityHorizonValue,
        appSettingsManager.defaultVisibilityHorizonUnit
    ) { equipments, opTypes, reminders, globalHorizonVal, globalHorizonUnitStr ->
        val now = System.currentTimeMillis()
        val globalHorizonUnit = TimeGranularity.valueOf(globalHorizonUnitStr)
        
        val allPredictions = mutableListOf<Pair<Equipment, OperationStatus>>()
        
        equipments.filter { !it.dismissed }.forEach { equipment ->
            val trend = maintenanceManager.calculateTrend(equipment)
            
            opTypes.filter { it.isPredictable && !it.dismissed }.forEach { opType ->
                // Check if there's already a manual reminder
                val hasManualReminder = reminders.any { !it.isCompleted && it.equipmentId == equipment.id && it.operationTypeId == opType.id }
                
                if (!hasManualReminder) {
                    val lastLogForOp = maintenanceLogDao.getLastLogForEquipmentAndOperation(equipment.id, opType.id)
                    if (lastLogForOp != null) {
                        val nextPresumedDate = maintenanceManager.getOperationPrediction(equipment.id, opType, lastLogForOp, trend)
                        
                        if (nextPresumedDate != null) {
                            val horizonValue = if (equipment.useCustomUsageWindow) equipment.visibilityHorizon else globalHorizonVal
                            val horizonUnit = if (equipment.useCustomUsageWindow) equipment.visibilityHorizonUnit else globalHorizonUnit
                            val horizonMs = maintenanceManager.getWindowMs(horizonValue.toLong(), horizonUnit)
                            
                            // Include if overdue or within horizon
                            if (nextPresumedDate < now || nextPresumedDate <= now + horizonMs) {
                                allPredictions.add(
                                    Pair(equipment, OperationStatus(
                                        operation = opType,
                                        lastLogDate = lastLogForOp.date,
                                        lastLogValue = lastLogForOp.value,
                                        nextPresumedDate = nextPresumedDate,
                                        isOverdue = nextPresumedDate < now,
                                        isPlanned = false
                                    ))
                                )
                            }
                        }
                    }
                }
            }
        }
        allPredictions.sortedBy { it.second.nextPresumedDate ?: Long.MAX_VALUE }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT),
        initialValue = emptyList()
    )

    val measurementUnits: StateFlow<List<MeasurementUnit>> = measurementUnitDao.getAllUnits()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT),
            initialValue = emptyList()
        )

    val allCategories = categoryDao.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT),
            initialValue = emptyList()
        )

    val defaultEquipmentId: StateFlow<Int?> = appSettingsManager.defaultEquipmentId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), null)
        
    val defaultOperationTypeId: StateFlow<Int?> = appSettingsManager.defaultOperationTypeId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), null)

    val syncCalendarByDefault: StateFlow<Boolean> = appSettingsManager.syncCalendarByDefault
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), false)

    val googleAccountName: StateFlow<String?> = appSettingsManager.googleAccountName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), null)

    val costTrendThreshold: StateFlow<Float> = appSettingsManager.costTrendThreshold
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), UiConstants.DEFAULT_COST_TREND_THRESHOLD)

    val allDrafts: StateFlow<Map<Int, MaintenanceLog>> = appSettingsManager.getAllDraftsFlow("log")
        .map { draftsMap ->
            draftsMap.mapValues { (_, json) ->
                try {
                    Json.decodeFromString<MaintenanceLog>(json)
                } catch (e: Exception) {
                    null
                }
            }.filterValues { it != null }.mapValues { it.value!! }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), emptyMap())

    val logAddDraft: StateFlow<MaintenanceLog?> = appSettingsManager.getDraftFlow("log", 0)
        .map { json ->
            try {
                json?.let { Json.decodeFromString<MaintenanceLog>(it) }
            } catch (e: Exception) {
                null
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), null)

    val reminderAddDraft: StateFlow<MaintenanceReminder?> = appSettingsManager.getDraftFlow("reminder", 0)
        .map { json ->
            try {
                json?.let { Json.decodeFromString<MaintenanceReminder>(it) }
            } catch (e: Exception) {
                null
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), null)

    fun getCategoryColor(categoryId: String): Flow<String?> = imageRepository.getCategoryColor(categoryId)

    private fun buildQuery(
        sectionId: Int,
        searchQuery: String,
        sortProperty: SortProperty,
        sortDirection: SortDirection,
        showDismissed: Boolean
    ): SimpleSQLiteQuery {
        val computedEquipmentDesc = "IFNULL(NULLIF(e.description, ''), 'id:' || e.id || ' - no description')"
        val computedOpDesc = "IFNULL(NULLIF(ot.description, ''), 'id:' || ot.id || ' - no description')"

        val selectClause = """
            SELECT
                l.*,
                e.description as equipmentDescription,
                ot.description as operationTypeDescription,
                e.photoUri as equipmentPhotoUri,
                e.iconIdentifier as equipmentIconIdentifier,
                ot.photoUri as operationTypePhotoUri,
                ot.iconIdentifier as operationTypeIconIdentifier,
                e.dismissed as equipmentDismissed,
                ot.dismissed as operationTypeDismissed,
                e.isResettable as equipmentIsResettable,
                ot.isSystem as operationTypeIsSystem,
                (SELECT l2.value FROM maintenance_logs l2 
                 JOIN operation_types ot2 ON l2.operationTypeId = ot2.id
                 WHERE l2.equipmentId = l.equipmentId 
                 AND (l2.date < l.date OR (l2.date = l.date AND l2.id < l.id)) 
                 AND ot2.isSystem = 0
                 ORDER BY l2.date DESC, l2.id DESC LIMIT 1) as previousLogValue,
                (SELECT ot2.isSystem FROM maintenance_logs l2 
                 JOIN operation_types ot2 ON l2.operationTypeId = ot2.id
                 WHERE l2.equipmentId = l.equipmentId 
                 AND (l2.date < l.date OR (l2.date = l.date AND l2.id < l.id))
                 ORDER BY l2.date DESC, l2.id DESC LIMIT 1) as previousLogIsSystem
            FROM maintenance_logs as l
            JOIN equipments as e ON l.equipmentId = e.id
            JOIN operation_types as ot ON l.operationTypeId = ot.id
        """

        val whereClauses = mutableListOf<String>()
        val args = mutableListOf<Any>()

        if (sectionId != AppConstants.ALL_SECTIONS_ID) {
            whereClauses.add("e.sectionId = ?")
            args.add(sectionId)
        }

        if (!showDismissed) {
            whereClauses.add("l.dismissed = 0")
        }

        if (searchQuery.isNotBlank()) {
            val searchTerm = "%$searchQuery%"
            val searchClause = "(($computedEquipmentDesc) LIKE ? OR ($computedOpDesc) LIKE ? OR l.notes LIKE ?)"
            whereClauses.add(searchClause)
            args.add(searchTerm)
            args.add(searchTerm)
            args.add(searchTerm)
        }

        val whereClause = if (whereClauses.isNotEmpty()) "WHERE " + whereClauses.joinToString(" AND ") else ""

        val orderByColumn = when (sortProperty) {
            SortProperty.DATE -> "l.date"
            SortProperty.EQUIPMENT -> computedEquipmentDesc
            SortProperty.OPERATION -> computedOpDesc
            SortProperty.VALUE -> "l.value"
            SortProperty.NOTES -> "l.notes"
        }

        val sortOrder = if (sortDirection == SortDirection.ASCENDING) "ASC" else "DESC"
        val nullsOrder = when (sortProperty) {
            SortProperty.VALUE, SortProperty.NOTES -> if (sortDirection == SortDirection.ASCENDING) "NULLS FIRST" else "NULLS LAST"
            else -> ""
        }

        val orderByClause = "ORDER BY $orderByColumn $sortOrder $nullsOrder, l.id DESC"

        val finalQuery = "$selectClause $whereClause $orderByClause"

        return SimpleSQLiteQuery(finalQuery, args.toTypedArray())
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onSortPropertyChanged(property: SortProperty) {
        _sortProperty.value = property
    }

    fun onSortDirectionChanged() {
        val newDirection = if (_sortDirection.value == SortDirection.ASCENDING) SortDirection.DESCENDING else SortDirection.ASCENDING
        _sortDirection.value = newDirection
    }

    fun onShowDismissedToggled() {
        _showDismissed.value = !_showDismissed.value
    }

    val activeResettableEquipmentsCount = appSettingsManager.selectedSectionId.flatMapLatest { sectionId ->
        if (sectionId == AppConstants.ALL_SECTIONS_ID) {
            equipmentDao.countActiveResettableEquipment()
        } else {
            equipmentDao.countActiveResettableEquipmentBySection(sectionId)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT),
        initialValue = 0
    )

    fun addLog(
        equipmentId: Int,
        operationTypeId: Int,
        notes: String?,
        value: Double?,
        date: Long,
        color: String?,
        resetAfter: Boolean = false,
        cost: Double? = null,
        isUnplanned: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                val newLog = MaintenanceLog(
                    equipmentId = equipmentId,
                    operationTypeId = operationTypeId,
                    notes = notes,
                    value = value,
                    date = date,
                    color = color,
                    resetAfter = resetAfter,
                    cost = cost,
                    isUnplanned = isUnplanned
                )
                maintenanceLogDao.insertLog(newLog)
                maintenanceManager.recalculateAccumulatedValues(equipmentId)
                
                // Mark reminder as completed if it exists for this equipment and operation type
                maintenanceReminderDao.getReminderByEquipmentAndOperation(equipmentId, operationTypeId)?.let { reminder ->
                    maintenanceReminderDao.updateReminder(reminder.copy(isCompleted = true))
                    
                    // Delete Google Calendar event if it exists
                    reminder.calendarEventId?.let { eventId ->
                        val accountName = appSettingsManager.googleAccountName.first()
                        if (accountName != null) {
                            val credential = calendarManager.getCredential(accountName)
                            calendarManager.deleteEvent(credential, eventId)
                        }
                    }
                }
                cancelLogAddDraft()
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.AddLogFailed)
            }
        }
    }

    fun addReminder(equipmentId: Int, operationTypeId: Int, dueDate: Long?, dueValue: Double?, syncToCalendar: Boolean) {
        viewModelScope.launch {
            try {
                // Calculate presumed date if value is present
                val presumedDate = if (dueValue != null) maintenanceManager.estimateDueDate(equipmentId, dueValue) else null
                
                // Use fixed dueDate if present, otherwise use presumedDate for calendar
                val calendarDate = dueDate ?: presumedDate
                
                var calendarEventId: String? = null
                if (syncToCalendar && calendarDate != null) {
                    val accountName = appSettingsManager.googleAccountName.first()
                    if (accountName != null) {
                        val equipment = equipmentDao.getEquipmentByIdOneShot(equipmentId)
                        val operation = operationTypeDao.getOperationTypeById(operationTypeId)
                        val title = resourceProvider.getString(R.string.calendar_event_title, equipment?.description ?: "", operation?.description ?: "")
                        val description = resourceProvider.getString(R.string.calendar_event_description)
                        
                        val credential = calendarManager.getCredential(accountName)
                        calendarEventId = calendarManager.addEvent(
                            credential = credential,
                            title = title,
                            description = description,
                            startTimeMillis = calendarDate,
                            endTimeMillis = calendarDate + 3600000 // +1 hour
                        )
                    }
                }

                val reminder = MaintenanceReminder(
                    equipmentId = equipmentId,
                    operationTypeId = operationTypeId,
                    dueDate = dueDate,
                    dueValue = dueValue,
                    presumedDate = presumedDate,
                    calendarEventId = calendarEventId
                )
                maintenanceReminderDao.insertReminder(reminder)
                cancelReminderAddDraft()
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.AddLogFailed)
            }
        }
    }

    fun updateReminder(equipmentId: Int, operationTypeId: Int, dueDate: Long?, dueValue: Double?, syncToCalendar: Boolean, reminderId: Int) {
        viewModelScope.launch {
            try {
                val existingReminder = maintenanceReminderDao.getReminderById(reminderId) ?: return@launch
                
                // Calculate presumed date if value is present
                val presumedDate = if (dueValue != null) maintenanceManager.estimateDueDate(equipmentId, dueValue) else null
                
                // Effective date for calendar
                val calendarDate = dueDate ?: presumedDate
                
                var calendarEventId = existingReminder.calendarEventId
                val accountName = appSettingsManager.googleAccountName.first()

                if (syncToCalendar && calendarDate != null) {
                    val equipment = equipmentDao.getEquipmentByIdOneShot(equipmentId)
                    val operation = operationTypeDao.getOperationTypeById(operationTypeId)
                    val title = resourceProvider.getString(R.string.calendar_event_title, equipment?.description ?: "", operation?.description ?: "")
                    val description = resourceProvider.getString(R.string.calendar_event_description)

                    if (accountName != null) {
                        val credential = calendarManager.getCredential(accountName)
                        if (calendarEventId != null) {
                            calendarManager.updateEvent(
                                credential = credential,
                                eventId = calendarEventId,
                                title = title,
                                description = description,
                                startTimeMillis = calendarDate,
                                endTimeMillis = calendarDate + 3600000
                            )
                        } else {
                            calendarEventId = calendarManager.addEvent(
                                credential = credential,
                                title = title,
                                description = description,
                                startTimeMillis = calendarDate,
                                endTimeMillis = calendarDate + 3600000
                            )
                        }
                    }
                } else if (calendarEventId != null && accountName != null) {
                    val credential = calendarManager.getCredential(accountName)
                    calendarManager.deleteEvent(credential, calendarEventId)
                    calendarEventId = null
                }

                val updatedReminder = existingReminder.copy(
                    equipmentId = equipmentId,
                    operationTypeId = operationTypeId,
                    dueDate = dueDate,
                    dueValue = dueValue,
                    presumedDate = presumedDate,
                    calendarEventId = calendarEventId
                )
                maintenanceReminderDao.updateReminder(updatedReminder)
                // If it was an edit from a prediction/reminder, we might have a draft to clear
                cancelReminderAddDraft()
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.UpdateReminderFailed)
            }
        }
    }

    fun updateLog(log: MaintenanceLog) {
        viewModelScope.launch {
            try {
                maintenanceLogDao.updateLog(log)
                maintenanceManager.recalculateAccumulatedValues(log.equipmentId)
                _editingCardId.value = null
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.UpdateLogFailed)
            }
        }
    }

    // --- Draft Management ---
    fun startEditing(log: MaintenanceLog) {
        viewModelScope.launch {
            val json = Json.encodeToString(log)
            appSettingsManager.saveDraft("log", log.id, json)
            onEditLog(log)
        }
    }

    fun updateDraft(log: MaintenanceLog) {
        viewModelScope.launch {
            val json = Json.encodeToString(log)
            appSettingsManager.saveDraft("log", log.id, json)
        }
    }

    fun cancelEditing(id: Int) {
        viewModelScope.launch {
            appSettingsManager.deleteDraft("log", id)
            _editingCardId.value = null
        }
    }

    fun updateLogAddDraft(log: MaintenanceLog) {
        viewModelScope.launch {
            val json = Json.encodeToString(log)
            appSettingsManager.saveDraft("log", 0, json)
        }
    }

    fun cancelLogAddDraft() {
        viewModelScope.launch {
            appSettingsManager.deleteDraft("log", 0)
        }
    }

    fun updateReminderAddDraft(reminder: MaintenanceReminder) {
        viewModelScope.launch {
            val json = Json.encodeToString(reminder)
            appSettingsManager.saveDraft("reminder", 0, json)
        }
    }

    fun cancelReminderAddDraft() {
        viewModelScope.launch {
            appSettingsManager.deleteDraft("reminder", 0)
        }
    }

    fun saveEditing(log: MaintenanceLog) {
        viewModelScope.launch {
            updateLog(log)
            appSettingsManager.deleteDraft("log", log.id)
        }
    }

    fun dismissLog(log: MaintenanceLog) {
        viewModelScope.launch {
            try {
                maintenanceLogDao.updateLog(log.copy(dismissed = true))
                _editingCardId.value = null
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.DismissLogFailed)
            }
        }
    }

    fun restoreLog(log: MaintenanceLog) {
        viewModelScope.launch {
            try {
                maintenanceLogDao.updateLog(log.copy(dismissed = false))
                _editingCardId.value = null
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.RestoreLogFailed)
            }
        }
    }

    fun deleteLog(log: MaintenanceLog) {
        viewModelScope.launch {
            try {
                maintenanceLogDao.deleteLog(log)
                maintenanceManager.recalculateAccumulatedValues(log.equipmentId)
                _editingCardId.value = null
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.DeleteLogFailed)
            }
        }
    }

    fun deleteReminder(reminderDetails: MaintenanceReminderDetails) {
        viewModelScope.launch {
            try {
                reminderDetails.reminder.calendarEventId?.let { eventId ->
                    val accountName = appSettingsManager.googleAccountName.first()
                    if (accountName != null) {
                        val credential = calendarManager.getCredential(accountName)
                        calendarManager.deleteEvent(credential, eventId)
                    }
                }
                maintenanceReminderDao.deleteReminder(reminderDetails.reminder)
                if (_selectedReminderForEdit.value?.reminder?.id == reminderDetails.reminder.id) {
                    _selectedReminderForEdit.value = null
                }
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.DeleteReminderFailed)
            }
        }
    }

    fun recalculateAllReminders() {
        viewModelScope.launch {
            try {
                val windowValue = appSettingsManager.costAnalysisWindowValue.first()
                val windowUnit = appSettingsManager.costAnalysisWindowUnit.first()
                val windowMs = maintenanceManager.getWindowMs(windowValue.toLong(), TimeGranularity.valueOf(windowUnit))
                val sinceDate = System.currentTimeMillis() - windowMs
                
                val reminders = maintenanceReminderDao.getActiveRemindersWithDetails(sinceDate).first()
                val accountName = appSettingsManager.googleAccountName.first()
                val credential = accountName?.let { calendarManager.getCredential(it) }

                reminders.forEach { details ->
                    val reminder = details.reminder
                    var updatedReminder = reminder
                    var changed = false

                    // Logic: Presumed date is always calculated if dueValue is present
                    if (reminder.dueValue != null) {
                        val newPresumedDate = maintenanceManager.estimateDueDate(reminder.equipmentId, reminder.dueValue)
                        if (newPresumedDate != reminder.presumedDate) {
                            updatedReminder = updatedReminder.copy(presumedDate = newPresumedDate)
                            changed = true
                        }
                    } else {
                        // If no target value, reset presumed date (shouldn't happen with proper logic but for safety)
                        if (reminder.presumedDate != null) {
                            updatedReminder = updatedReminder.copy(presumedDate = null)
                            changed = true
                        }
                    }

                    if (changed) {
                        maintenanceReminderDao.updateReminder(updatedReminder)
                        
                        // Update Google Calendar if event exists
                        val effectiveDate = updatedReminder.dueDate ?: updatedReminder.presumedDate
                        if (updatedReminder.calendarEventId != null && credential != null && effectiveDate != null) {
                            val equipment = equipmentDao.getEquipmentByIdOneShot(updatedReminder.equipmentId)
                            val operation = operationTypeDao.getOperationTypeById(updatedReminder.operationTypeId)
                            val title = resourceProvider.getString(R.string.calendar_event_title, equipment?.description ?: "", operation?.description ?: "")
                            val description = resourceProvider.getString(R.string.calendar_event_description_updated)
                            
                            calendarManager.updateEvent(
                                credential = credential,
                                eventId = updatedReminder.calendarEventId,
                                title = title,
                                description = description,
                                startTimeMillis = effectiveDate,
                                endTimeMillis = effectiveDate + 3600000
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.RecalculateRemindersFailed)
            }
        }
    }

    suspend fun estimateDueDate(equipmentId: Int, targetValue: Double): Long? = maintenanceManager.estimateDueDate(equipmentId, targetValue)
    suspend fun estimateTargetValue(equipmentId: Int, dueDate: Long): Double? = maintenanceManager.estimateTargetValue(equipmentId, dueDate)

    suspend fun getOperationCostStats(operationTypeId: Int): Pair<Double?, Double?> {
        val windowValue = appSettingsManager.costAnalysisWindowValue.first()
        val windowUnit = TimeGranularity.valueOf(appSettingsManager.costAnalysisWindowUnit.first())
        return maintenanceManager.getOperationCostStats(operationTypeId, windowValue.toLong(), windowUnit)
    }
}
