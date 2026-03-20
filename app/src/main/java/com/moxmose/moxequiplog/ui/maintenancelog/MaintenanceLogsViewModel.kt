package com.moxmose.moxequiplog.ui.maintenancelog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.sqlite.db.SimpleSQLiteQuery
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.ImageRepository
import com.moxmose.moxequiplog.data.local.*
import com.moxmose.moxequiplog.utils.AppConstants
import com.moxmose.moxequiplog.utils.UiConstants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortProperty {
    DATE, EQUIPMENT, OPERATION, KILOMETERS, NOTES
}

enum class SortDirection {
    ASCENDING, DESCENDING
}

@OptIn(ExperimentalCoroutinesApi::class)
class MaintenanceLogViewModel(
    private val maintenanceLogDao: MaintenanceLogDao,
    private val equipmentDao: EquipmentDao,
    private val operationTypeDao: OperationTypeDao,
    private val categoryDao: CategoryDao,
    private val appSettingsManager: AppSettingsManager,
    private val imageRepository: ImageRepository,
    private val measurementUnitDao: MeasurementUnitDao
) : ViewModel() {

    sealed class UiEvent {
        data object AddLogFailed : UiEvent()
        data object UpdateLogFailed : UiEvent()
        data object DismissLogFailed : UiEvent()
        data object RestoreLogFailed : UiEvent()
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
                ot.dismissed as operationTypeDismissed
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
            SortProperty.KILOMETERS -> "l.kilometers"
            SortProperty.NOTES -> "l.notes"
        }

        val sortOrder = if (sortDirection == SortDirection.ASCENDING) "ASC" else "DESC"
        val nullsOrder = when (sortProperty) {
            SortProperty.KILOMETERS, SortProperty.NOTES -> if (sortDirection == SortDirection.ASCENDING) "NULLS FIRST" else "NULLS LAST"
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

    fun addLog(equipmentId: Int, operationTypeId: Int, notes: String?, kilometers: Int?, date: Long, color: String?) {
        viewModelScope.launch {
            try {
                val newLog = MaintenanceLog(
                    equipmentId = equipmentId,
                    operationTypeId = operationTypeId,
                    notes = notes,
                    kilometers = kilometers,
                    date = date,
                    color = color
                )
                maintenanceLogDao.insertLog(newLog)
            } catch (e: Exception) {
                _uiEvents.send(UiEvent.AddLogFailed)
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
}
