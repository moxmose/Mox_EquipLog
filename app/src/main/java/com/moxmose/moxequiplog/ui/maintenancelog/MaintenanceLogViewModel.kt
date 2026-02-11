package com.moxmose.moxequiplog.ui.maintenancelog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.sqlite.db.SimpleSQLiteQuery
import com.moxmose.moxequiplog.data.local.CategoryDao
import com.moxmose.moxequiplog.data.local.EquipmentDao
import com.moxmose.moxequiplog.data.local.MaintenanceLog
import com.moxmose.moxequiplog.data.local.MaintenanceLogDao
import com.moxmose.moxequiplog.data.local.MaintenanceLogDetails
import com.moxmose.moxequiplog.data.local.OperationTypeDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MaintenanceLogViewModel(
    private val maintenanceLogDao: MaintenanceLogDao,
    equipmentDao: EquipmentDao,
    operationTypeDao: OperationTypeDao,
    categoryDao: CategoryDao
) : ViewModel() {

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
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    val allCategories = categoryDao.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

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
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val allOperationTypes = operationTypeDao.getAllOperationTypes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun addLog(equipmentId: Int, operationTypeId: Int, notes: String?, kilometers: Int?, date: Long, color: String?) {
        viewModelScope.launch {
            val newLog = MaintenanceLog(
                equipmentId = equipmentId,
                operationTypeId = operationTypeId,
                notes = notes,
                kilometers = kilometers,
                date = date,
                color = color
            )
            maintenanceLogDao.insertLog(newLog)
        }
    }

    fun updateLog(log: MaintenanceLog) {
        viewModelScope.launch {
            maintenanceLogDao.updateLog(log)
        }
    }

    fun dismissLog(log: MaintenanceLog) {
        viewModelScope.launch {
            updateLog(log.copy(dismissed = true))
        }
    }

    fun restoreLog(log: MaintenanceLog) {
        viewModelScope.launch {
            updateLog(log.copy(dismissed = false))
        }
    }
}
