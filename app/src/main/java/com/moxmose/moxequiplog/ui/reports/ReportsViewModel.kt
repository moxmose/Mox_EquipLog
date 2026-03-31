package com.moxmose.moxequiplog.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.ImageRepository
import com.moxmose.moxequiplog.data.local.*
import com.moxmose.moxequiplog.utils.UiConstants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

@Serializable
enum class TimeGranularity {
    HOURS, DAYS, WEEKS, MONTHS, YEARS
}

data class ChartPoint(
    val date: Long,
    val kilometers: Float,
    val label: String? = null
)

data class PieChartPoint(
    val label: String,
    val value: Float,
    val color: String? = null
)

@Serializable
data class ReportFilterState(
    val selectedEquipmentIds: Set<Int> = emptySet(),
    val selectedOperationTypeIds: Set<Int> = emptySet(),
    val startDate: Long? = null,
    val endDate: Long? = null,
    val timeGranularity: TimeGranularity = TimeGranularity.HOURS,
    val showDismissed: Boolean = false
)

data class ReportsUiState(
    val equipments: List<Equipment> = emptyList(),
    val selectedEquipmentIds: Set<Int> = emptySet(),
    val equipmentChartData: Map<Int, List<ChartPoint>> = emptyMap(),
    val equipmentDistribution: List<PieChartPoint> = emptyList(),
    val equipmentCategoryColor: String = UiConstants.DEFAULT_FALLBACK_COLOR,
    val equipmentUnitLabel: String = "",
    val hasMixedUnits: Boolean = false,
    
    val operationTypes: List<OperationType> = emptyList(),
    val selectedOperationTypeIds: Set<Int> = emptySet(),
    val operationChartData: Map<Int, List<ChartPoint>> = emptyMap(),
    val operationDistribution: List<PieChartPoint> = emptyList(),
    val operationCategoryColor: String = UiConstants.DEFAULT_FALLBACK_COLOR,
    val operationUnitLabel: String = "",
    val opHasMixedUnits: Boolean = false,

    val startDate: Long? = null,
    val endDate: Long? = null,
    val timeGranularity: TimeGranularity = TimeGranularity.HOURS,
    val showDismissed: Boolean = false,

    val colorMode: String = UiConstants.DEFAULT_REPORTS_COLOR_MODE,
    val customColors: List<String> = emptyList(),
    
    val savedFilters: List<ReportFilter> = emptyList(),
    val activeFilterName: String? = null,
    val isFilterDirty: Boolean = false
)

private data class SelectionState(
    val selectedEquipmentIds: Set<Int>,
    val selectedOperationTypeIds: Set<Int>,
    val startDate: Long?,
    val endDate: Long?,
    val timeGranularity: TimeGranularity,
    val refreshKey: Int,
    val showDismissed: Boolean,
    val colorMode: String,
    val customColors: List<String>
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class ReportsViewModel(
    private val equipmentDao: EquipmentDao,
    private val maintenanceLogDao: MaintenanceLogDao,
    private val operationTypeDao: OperationTypeDao,
    private val measurementUnitDao: MeasurementUnitDao,
    private val imageRepository: ImageRepository,
    private val appSettingsManager: AppSettingsManager,
    private val reportFilterDao: ReportFilterDao
) : ViewModel() {

    private val _selectedEquipmentIds = MutableStateFlow<Set<Int>>(emptySet())
    private val _selectedOperationTypeIds = MutableStateFlow<Set<Int>>(emptySet())
    private val _startDate = MutableStateFlow<Long?>(null)
    private val _endDate = MutableStateFlow<Long?>(null)
    private val _timeGranularity = MutableStateFlow(TimeGranularity.HOURS)
    private val _refreshTrigger = MutableStateFlow(0)
    private val _showDismissed = MutableStateFlow(false)
    private val _activeFilter = MutableStateFlow<ReportFilter?>(null)

    private var initializedEquipments = false
    private var initializedOperations = false

    // Grouping classes to solve combine parameter limit
    private data class FilterBase(val ids: Set<Int>, val ops: Set<Int>, val start: Long?, val end: Long?, val gran: TimeGranularity)
    private data class CoreData(val equips: List<Equipment>, val ops: List<OperationType>, val logs: List<MaintenanceLogDetails>, val units: List<MeasurementUnit>)
    private data class StyleData(val ec: String?, val oc: String?, val selections: SelectionState)
    private data class PersistData(val saved: List<ReportFilter>, val active: ReportFilter?, val current: ReportFilterState)

    private val filterBaseFlow = combine(_selectedEquipmentIds, _selectedOperationTypeIds, _startDate, _endDate, _timeGranularity) { ids, ops, s, e, g ->
        FilterBase(ids, ops, s, e, g)
    }

    private val currentState: Flow<ReportFilterState> = combine(filterBaseFlow, _showDismissed) { base, dismissed ->
        ReportFilterState(base.ids, base.ops, base.start, base.end, base.gran, dismissed)
    }

    private val selectionState: Flow<SelectionState> = combine(
        currentState,
        _refreshTrigger,
        appSettingsManager.reportsColorMode,
        combine(appSettingsManager.reportsCustomColors, imageRepository.allColorsForReports) { custom, dbColors ->
            custom ?: dbColors.filter { !it.reportHidden }.map { it.hexValue }
        }
    ) { state, refresh, mode, colors ->
        SelectionState(state.selectedEquipmentIds, state.selectedOperationTypeIds, state.startDate, state.endDate, state.timeGranularity, refresh, state.showDismissed, mode, colors)
    }

    init {
        viewModelScope.launch {
            val lastSession = reportFilterDao.getLastSession("ALL_REPORTS").firstOrNull()
            lastSession?.let { session ->
                try {
                    val state = Json.decodeFromString<ReportFilterState>(session.filterJson)
                    _selectedEquipmentIds.value = state.selectedEquipmentIds
                    _selectedOperationTypeIds.value = state.selectedOperationTypeIds
                    _startDate.value = state.startDate
                    _endDate.value = state.endDate
                    _timeGranularity.value = state.timeGranularity
                    _showDismissed.value = state.showDismissed
                    initializedEquipments = true
                    initializedOperations = true
                } catch (e: Exception) { }
            }
        }

        viewModelScope.launch {
            _showDismissed.flatMapLatest { if (it) equipmentDao.getAllEquipments() else equipmentDao.getActiveEquipments() }
                .collect { list ->
                    if (!initializedEquipments && list.isNotEmpty()) {
                        _selectedEquipmentIds.value = list.map { it.id }.toSet()
                        initializedEquipments = true
                    }
                }
        }
        viewModelScope.launch {
            _showDismissed.flatMapLatest { if (it) operationTypeDao.getAllOperationTypes() else operationTypeDao.getActiveOperationTypes() }
                .collect { list ->
                    if (!initializedOperations && list.isNotEmpty()) {
                        _selectedOperationTypeIds.value = list.map { it.id }.toSet()
                        initializedOperations = true
                    }
                }
        }

        currentState.debounce(1500).onEach { state ->
            val json = Json.encodeToString(state)
            reportFilterDao.updateLastSession(ReportFilter(reportType = "ALL_REPORTS", filterJson = json, name = null))
        }.launchIn(viewModelScope)
    }

    val uiState: StateFlow<ReportsUiState> = combine(
        combine(
            _showDismissed.flatMapLatest { if (it) equipmentDao.getAllEquipments() else equipmentDao.getActiveEquipments() },
            _showDismissed.flatMapLatest { if (it) operationTypeDao.getAllOperationTypes() else operationTypeDao.getActiveOperationTypes() },
            maintenanceLogDao.getLogsWithDetails(androidx.sqlite.db.SimpleSQLiteQuery("SELECT l.*, e.description as equipmentDescription, ot.description as operationTypeDescription, e.photoUri as equipmentPhotoUri, e.iconIdentifier as equipmentIconIdentifier, ot.photoUri as operationTypePhotoUri, ot.iconIdentifier as operationTypeIconIdentifier, e.dismissed as equipmentDismissed, ot.dismissed as operationTypeDismissed FROM maintenance_logs as l JOIN equipments as e ON l.equipmentId = e.id JOIN operation_types as ot ON l.operationTypeId = ot.id ORDER BY l.date ASC")),
            measurementUnitDao.getAllUnits()
        ) { e, o, l, u -> CoreData(e, o, l, u) },
        combine(
            imageRepository.getCategoryColor(Category.EQUIPMENT),
            imageRepository.getCategoryColor(Category.OPERATION),
            selectionState
        ) { ec, oc, s -> StyleData(ec, oc, s) },
        combine(
            reportFilterDao.getSavedFilters("ALL_REPORTS"),
            _activeFilter,
            currentState
        ) { saved, active, current -> PersistData(saved, active, current) }
    ) { core, style, persist ->
        val filteredLogs = core.logs.filter { logDetail ->
            val date = logDetail.log.date
            val matchesDate = (style.selections.startDate == null || date >= style.selections.startDate) && 
                              (selectionsMatchesDate(date, style.selections))
            val matchesVisibility = style.selections.showDismissed || !logDetail.log.dismissed
            matchesDate && matchesVisibility
        }

        val sortedSelectedEquipIds = core.equips.map { it.id }.filter { it in style.selections.selectedEquipmentIds }
        val sortedSelectedOpIds = core.ops.map { it.id }.filter { it in style.selections.selectedOperationTypeIds }

        val equipDist = filteredLogs
            .filter { it.log.equipmentId in style.selections.selectedEquipmentIds }
            .groupBy { it.log.equipmentId }
            .map { (id, logs) ->
                val label = core.equips.find { it.id == id }?.description?.takeIf { it.isNotBlank() } ?: "ID: $id"
                PieChartPoint(label, logs.size.toFloat())
            }.sortedByDescending { it.value }

        val opDist = filteredLogs
            .filter { it.log.operationTypeId in style.selections.selectedOperationTypeIds }
            .groupBy { it.log.operationTypeId }
            .map { (id, logs) ->
                val label = core.ops.find { it.id == id }?.description?.takeIf { it.isNotBlank() } ?: "ID: $id"
                PieChartPoint(label, logs.size.toFloat())
            }.sortedByDescending { it.value }

        val equipChartData = sortedSelectedEquipIds.associateWith { id ->
            val points = filteredLogs
                .filter { it.log.equipmentId == id && it.log.kilometers != null }
                .map { ChartPoint(it.log.date, it.log.kilometers!!.toFloat()) }
            aggregateData(points, style.selections.timeGranularity)
        }

        val opChartData = sortedSelectedOpIds.associateWith { id ->
            val points = filteredLogs
                .filter { it.log.operationTypeId == id && it.log.kilometers != null }
                .map { ChartPoint(it.log.date, it.log.kilometers!!.toFloat(), it.equipmentDescription) }
            aggregateData(points, style.selections.timeGranularity)
        }
        
        val selectedUnits = sortedSelectedEquipIds.mapNotNull { id ->
            core.equips.find { it.id == id }?.unitId?.let { unitId ->
                core.units.find { it.id == unitId }?.label
            }
        }.distinct()
        val equipUnit = selectedUnits.joinToString(", ")
        val hasMixedUnits = selectedUnits.size > 1

        val opSelectedUnits = sortedSelectedOpIds.flatMap { opId ->
            filteredLogs.filter { it.log.operationTypeId == opId }
                .mapNotNull { logDetail ->
                    core.equips.find { it.id == logDetail.log.equipmentId }?.unitId
                        ?.let { unitId -> core.units.find { it.id == unitId }?.label }
                }
        }.distinct()
        val opUnitLabel = opSelectedUnits.joinToString(", ")
        val opHasMixedUnits = opSelectedUnits.size > 1

        val isDirty = persist.active != null && persist.active.filterJson != Json.encodeToString(persist.current)

        ReportsUiState(
            equipments = core.equips,
            selectedEquipmentIds = style.selections.selectedEquipmentIds,
            equipmentChartData = equipChartData,
            equipmentDistribution = equipDist,
            equipmentCategoryColor = style.ec ?: UiConstants.DEFAULT_FALLBACK_COLOR,
            equipmentUnitLabel = equipUnit,
            hasMixedUnits = hasMixedUnits,
            operationTypes = core.ops,
            selectedOperationTypeIds = style.selections.selectedOperationTypeIds,
            operationChartData = opChartData,
            operationDistribution = opDist,
            operationCategoryColor = style.oc ?: UiConstants.DEFAULT_FALLBACK_COLOR,
            operationUnitLabel = opUnitLabel,
            opHasMixedUnits = opHasMixedUnits,
            startDate = style.selections.startDate,
            endDate = style.selections.endDate,
            timeGranularity = style.selections.timeGranularity,
            showDismissed = style.selections.showDismissed,
            colorMode = style.selections.colorMode,
            customColors = style.selections.customColors,
            savedFilters = persist.saved,
            activeFilterName = persist.active?.name,
            isFilterDirty = isDirty
        )
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = ReportsUiState())

    private fun selectionsMatchesDate(date: Long, selections: SelectionState): Boolean {
        return (selections.startDate == null || date >= selections.startDate) && 
               (selections.endDate == null || date <= selections.endDate)
    }

    private fun aggregateData(points: List<ChartPoint>, granularity: TimeGranularity): List<ChartPoint> {
        if (points.isEmpty()) return emptyList()
        val calendar = Calendar.getInstance()
        return points.groupBy { point ->
            calendar.timeInMillis = point.date
            when (granularity) {
                TimeGranularity.HOURS -> { calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0) }
                TimeGranularity.DAYS -> { calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0) }
                TimeGranularity.WEEKS -> { calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek); calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0) }
                TimeGranularity.MONTHS -> { calendar.set(Calendar.DAY_OF_MONTH, 1); calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0) }
                TimeGranularity.YEARS -> { calendar.set(Calendar.DAY_OF_YEAR, 1); calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0) }
            }
            calendar.timeInMillis
        }.map { (timestamp, groupedPoints) ->
            ChartPoint(timestamp, groupedPoints.maxOf { it.kilometers })
        }.sortedBy { it.date }
    }

    fun toggleEquipmentSelection(id: Int) {
        _selectedEquipmentIds.value = if (_selectedEquipmentIds.value.contains(id)) _selectedEquipmentIds.value - id else _selectedEquipmentIds.value + id
    }

    fun toggleOperationTypeSelection(id: Int) {
        _selectedOperationTypeIds.value = if (_selectedOperationTypeIds.value.contains(id)) _selectedOperationTypeIds.value - id else _selectedOperationTypeIds.value + id
    }

    fun selectAllEquipment() { _selectedEquipmentIds.value = uiState.value.equipments.map { it.id }.toSet() }
    fun invertEquipmentSelection() { _selectedEquipmentIds.value = uiState.value.equipments.map { it.id }.toSet() - _selectedEquipmentIds.value }
    fun clearEquipmentSelection() { _selectedEquipmentIds.value = emptySet() }
    fun selectAllOperationTypes() { _selectedOperationTypeIds.value = uiState.value.operationTypes.map { it.id }.toSet() }
    fun invertOperationTypeSelection() { _selectedOperationTypeIds.value = uiState.value.operationTypes.map { it.id }.toSet() - _selectedOperationTypeIds.value }
    fun clearOperationTypeSelection() { _selectedOperationTypeIds.value = emptySet() }
    fun setDateRange(start: Long?, end: Long?) { _startDate.value = start; _endDate.value = end }
    fun setTimeGranularity(granularity: TimeGranularity) { _timeGranularity.value = granularity }

    fun resetFilters() {
        _startDate.value = null; _endDate.value = null
        _timeGranularity.value = TimeGranularity.HOURS; _showDismissed.value = false
        selectAllEquipment(); selectAllOperationTypes()
        _activeFilter.value = null
    }
    
    fun toggleShowDismissed() { _showDismissed.value = !_showDismissed.value }
    fun refresh() { _refreshTrigger.value += 1 }

    fun saveAsNewFilter(name: String) {
        viewModelScope.launch {
            val state = ReportFilterState(_selectedEquipmentIds.value, _selectedOperationTypeIds.value, _startDate.value, _endDate.value, _timeGranularity.value, _showDismissed.value)
            val json = Json.encodeToString(state)
            val existing = uiState.value.savedFilters.find { it.name?.equals(name, ignoreCase = true) == true }
            val filterToSave = if (existing != null) existing.copy(filterJson = json, timestamp = System.currentTimeMillis())
                               else ReportFilter(name = name, reportType = "ALL_REPORTS", filterJson = json, isLastSession = false)
            reportFilterDao.insertFilter(filterToSave)
            _activeFilter.value = filterToSave
        }
    }

    fun overwriteActiveFilter() {
        val active = _activeFilter.value ?: return
        viewModelScope.launch {
            val state = ReportFilterState(_selectedEquipmentIds.value, _selectedOperationTypeIds.value, _startDate.value, _endDate.value, _timeGranularity.value, _showDismissed.value)
            val updated = active.copy(filterJson = Json.encodeToString(state), timestamp = System.currentTimeMillis())
            reportFilterDao.insertFilter(updated)
            _activeFilter.value = updated
        }
    }

    fun applySavedFilter(filter: ReportFilter) {
        try {
            val state = Json.decodeFromString<ReportFilterState>(filter.filterJson)
            _selectedEquipmentIds.value = state.selectedEquipmentIds
            _selectedOperationTypeIds.value = state.selectedOperationTypeIds
            _startDate.value = state.startDate
            _endDate.value = state.endDate
            _timeGranularity.value = state.timeGranularity
            _showDismissed.value = state.showDismissed
            _activeFilter.value = filter
            refresh()
        } catch (e: Exception) { }
    }

    fun deleteSavedFilter(id: Int) {
        viewModelScope.launch {
            if (_activeFilter.value?.id == id) _activeFilter.value = null
            reportFilterDao.deleteFilter(id)
        }
    }
}
