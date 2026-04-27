package com.moxmose.moxequiplog.ui.maintenancelog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.sqlite.db.SimpleSQLiteQuery
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.ImageRepository
import com.moxmose.moxequiplog.data.local.*
import com.moxmose.moxequiplog.utils.AppConstants
import com.moxmose.moxequiplog.utils.CalendarManager
import com.moxmose.moxequiplog.utils.UiConstants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
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
    private val imageRepository: ImageRepository,
    private val measurementUnitDao: MeasurementUnitDao,
    private val calendarManager: CalendarManager
) : ViewModel() {

    sealed class UiEvent {
        data object AddLogFailed : UiEvent()
        data object UpdateLogFailed : UiEvent()
        data object DismissLogFailed : UiEvent()
        data object RestoreLogFailed : UiEvent()
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

    val logs: StateFlow<List<MaintenanceLogDetails>> = combine(
        _searchQuery,
        _sortProperty,
        _sortDirection,
        _showDismissed
    ) { query, sortProp, sortDir, showDismissedValue ->
        buildQuery(query, sortProp, sortDir, showDismissedValue)
    }.flatMapLatest { query ->
        maintenanceLogDao.getLogsWithDetails(query)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT),
        initialValue = emptyList()
    )

    val activeReminders: StateFlow<List<MaintenanceReminderDetails>> = maintenanceReminderDao.getActiveRemindersWithDetails()
        .stateIn(
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

    fun getCategoryColor(categoryId: String): Flow<String?> = imageRepository.getCategoryColor(categoryId)

    private fun buildQuery(
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

    val allEquipments = equipmentDao.getAllEquipments()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT),
            initialValue = emptyList()
        )

    val allOperationTypes = operationTypeDao.getAllOperationTypes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT),
            initialValue = emptyList()
        )

    val activeResettableEquipmentsCount = equipmentDao.countActiveResettableEquipments()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT),
            initialValue = 0
        )

    fun addLog(equipmentId: Int, operationTypeId: Int, notes: String?, value: Double?, date: Long, color: String?) {
        viewModelScope.launch {
            try {
                val newLog = MaintenanceLog(
                    equipmentId = equipmentId,
                    operationTypeId = operationTypeId,
                    notes = notes,
                    value = value,
                    date = date,
                    color = color
                )
                maintenanceLogDao.insertLog(newLog)
                
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
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.AddLogFailed)
            }
        }
    }

    fun addReminder(equipmentId: Int, operationTypeId: Int, dueDate: Long?, dueValue: Double?, syncToCalendar: Boolean) {
        viewModelScope.launch {
            try {
                // Calculate presumed date if value is present
                val presumedDate = if (dueValue != null) estimateDueDate(equipmentId, dueValue) else null
                
                // Use fixed dueDate if present, otherwise use presumedDate for calendar
                val calendarDate = dueDate ?: presumedDate
                
                var calendarEventId: String? = null
                if (syncToCalendar && calendarDate != null) {
                    val accountName = appSettingsManager.googleAccountName.first()
                    if (accountName != null) {
                        val equipment = equipmentDao.getEquipmentByIdOneShot(equipmentId)
                        val operation = operationTypeDao.getOperationTypeById(operationTypeId)
                        val title = "Maintenance: ${equipment?.description} - ${operation?.description}"
                        val description = "Maintenance reminder from Mox EquipLog"
                        
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
                val presumedDate = if (dueValue != null) estimateDueDate(equipmentId, dueValue) else null
                
                // Effective date for calendar
                val calendarDate = dueDate ?: presumedDate
                
                var calendarEventId = existingReminder.calendarEventId
                val accountName = appSettingsManager.googleAccountName.first()

                if (syncToCalendar && calendarDate != null) {
                    val equipment = equipmentDao.getEquipmentByIdOneShot(equipmentId)
                    val operation = operationTypeDao.getOperationTypeById(operationTypeId)
                    val title = "Maintenance: ${equipment?.description} - ${operation?.description}"
                    val description = "Maintenance reminder from Mox EquipLog"

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
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.UpdateReminderFailed)
            }
        }
    }

    fun updateLog(log: MaintenanceLog) {
        viewModelScope.launch {
            try {
                maintenanceLogDao.updateLog(log)
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.UpdateLogFailed)
            }
        }
    }

    fun dismissLog(log: MaintenanceLog) {
        viewModelScope.launch {
            try {
                maintenanceLogDao.updateLog(log.copy(dismissed = true))
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.DismissLogFailed)
            }
        }
    }

    fun restoreLog(log: MaintenanceLog) {
        viewModelScope.launch {
            try {
                maintenanceLogDao.updateLog(log.copy(dismissed = false))
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.RestoreLogFailed)
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
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.DeleteReminderFailed)
            }
        }
    }

    fun recalculateAllReminders() {
        viewModelScope.launch {
            try {
                val reminders = maintenanceReminderDao.getActiveRemindersWithDetails().first()
                val accountName = appSettingsManager.googleAccountName.first()
                val credential = accountName?.let { calendarManager.getCredential(it) }

                reminders.forEach { details ->
                    val reminder = details.reminder
                    var updatedReminder = reminder
                    var changed = false

                    // Logic: Presumed date is always calculated if dueValue is present
                    if (reminder.dueValue != null) {
                        val newPresumedDate = estimateDueDate(reminder.equipmentId, reminder.dueValue)
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

                    // If no fixed dueDate, recalculate estimated target value for the presumed date (optional, but consistent)
                    // Wait, user said if UdM present and data not present, presumed date is used for sorting.
                    // If no fixed dueDate but target value is present, presumedDate will be used for sorting in DAO.

                    if (changed) {
                        maintenanceReminderDao.updateReminder(updatedReminder)
                        
                        // Update Google Calendar if event exists
                        // Effective date used for calendar: fixed dueDate if present, otherwise presumedDate
                        val effectiveDate = updatedReminder.dueDate ?: updatedReminder.presumedDate
                        if (updatedReminder.calendarEventId != null && credential != null && effectiveDate != null) {
                            val equipment = equipmentDao.getEquipmentByIdOneShot(updatedReminder.equipmentId)
                            val operation = operationTypeDao.getOperationTypeById(updatedReminder.operationTypeId)
                            val title = "Maintenance: ${equipment?.description} - ${operation?.description}"
                            val description = "Maintenance reminder from Mox EquipLog (updated)"
                            
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

    private fun getDailyManualAverage(equipment: Equipment): Double? {
        val value = equipment.manualAverageValue ?: return null
        return when (equipment.manualAverageUnit) {
            TimeGranularity.MINUTES_5 -> value * 12 * 24
            TimeGranularity.MINUTES_15 -> value * 4 * 24
            TimeGranularity.HOURS -> value * 24
            TimeGranularity.DAYS -> value
            TimeGranularity.WEEKS -> value / 7.0
            TimeGranularity.MONTHS -> value / 30.0
            TimeGranularity.YEARS -> value / 365.0
        }
    }

    suspend fun refreshTrends(equipmentId: Int): Double? {
        val equipment = equipmentDao.getEquipmentByIdOneShot(equipmentId) ?: return null
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
        
        val logs = maintenanceLogDao.getLogsSince(equipmentId, sinceDate)
            .filter { it.value != null } // Only logs with values contribute to trends
            .sortedBy { it.date }

        val manualAvg = getDailyManualAverage(equipment)
        if (logs.size < 2) return manualAvg

        // If there's a reset operation in the window, we should only consider logs after the last reset
        // to have a consistent trend for the current "cycle". 
        // However, for a general daily average, we can look at the whole window but we must handle value drops.
        
        var totalValueDiff = 0.0
        var totalTimeDiff = 0L
        
        for (i in 0 until logs.size - 1) {
            val current = logs[i]
            val next = logs[i+1]
            
            val diff = (next.value ?: 0.0) - (current.value ?: 0.0)
            if (diff >= 0) {
                totalValueDiff += diff
                totalTimeDiff += (next.date - current.date)
            }
            // If diff < 0, it was likely a reset, so we skip this interval for trend calculation
        }

        if (totalTimeDiff <= 0) return manualAvg
        
        val msPerDay = 24 * 60 * 60 * 1000.0
        val calculatedAverage = (totalValueDiff / totalTimeDiff) * msPerDay
        
        return if (calculatedAverage > 0) calculatedAverage else manualAvg
    }

    suspend fun estimateDueDate(equipmentId: Int, targetValue: Double): Long? {
        val trend = refreshTrends(equipmentId) ?: return null
        if (trend <= 0) return null

        val lastLog = maintenanceLogDao.getLastLogBefore(equipmentId, Long.MAX_VALUE)
        val lastValue = lastLog?.value ?: 0.0
        val lastDate = lastLog?.date ?: System.currentTimeMillis()

        if (targetValue <= lastValue) return null

        val remainingValue = targetValue - lastValue
        val daysRemaining = remainingValue / trend
        
        // Ensure we don't return a date in the past if trend was very high but last date was old
        val estimatedDate = lastDate + (daysRemaining * 24 * 60 * 60 * 1000).toLong()
        return if (estimatedDate > System.currentTimeMillis()) estimatedDate else System.currentTimeMillis() + (24 * 60 * 60 * 1000)
    }

    suspend fun estimateTargetValue(equipmentId: Int, dueDate: Long): Double? {
        val trend = refreshTrends(equipmentId) ?: return null

        val lastLog = maintenanceLogDao.getLastLogBefore(equipmentId, Long.MAX_VALUE)
        val lastValue = lastLog?.value ?: 0.0
        val lastDate = lastLog?.date ?: System.currentTimeMillis()

        val referenceDate = if (dueDate > lastDate) lastDate else System.currentTimeMillis()
        if (dueDate <= referenceDate) return lastValue

        val timeDiff = dueDate - referenceDate
        val msPerDay = 24 * 60 * 60 * 1000.0
        val days = timeDiff / msPerDay

        return lastValue + (days * trend)
    }
}
