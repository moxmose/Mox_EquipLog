package com.moxmose.moxequiplog.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moxmose.moxequiplog.data.ImageRepository
import com.moxmose.moxequiplog.data.local.*
import com.moxmose.moxequiplog.utils.UiConstants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.*

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

data class ReportsUiState(
    val equipments: List<Equipment> = emptyList(),
    val selectedEquipmentIds: Set<Int> = emptySet(),
    val equipmentChartData: Map<Int, List<ChartPoint>> = emptyMap(), // Mappa per multi-serie
    val equipmentDistribution: List<PieChartPoint> = emptyList(),
    val equipmentCategoryColor: String = UiConstants.DEFAULT_FALLBACK_COLOR,
    val equipmentUnitLabel: String = "",
    val hasMixedUnits: Boolean = false,
    
    val operationTypes: List<OperationType> = emptyList(),
    val selectedOperationTypeIds: Set<Int> = emptySet(),
    val operationChartData: Map<Int, List<ChartPoint>> = emptyMap(), // Mappa per multi-serie
    val operationDistribution: List<PieChartPoint> = emptyList(),
    val operationCategoryColor: String = UiConstants.DEFAULT_FALLBACK_COLOR,

    // Time Filter State
    val startDate: Long? = null,
    val endDate: Long? = null,
    val timeGranularity: TimeGranularity = TimeGranularity.HOURS,
    val showDismissed: Boolean = false
)

private data class SelectionState(
    val selectedEquipmentIds: Set<Int>,
    val selectedOperationTypeIds: Set<Int>,
    val startDate: Long?,
    val endDate: Long?,
    val timeGranularity: TimeGranularity,
    val refreshKey: Int,
    val showDismissed: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(
    private val equipmentDao: EquipmentDao,
    private val maintenanceLogDao: MaintenanceLogDao,
    private val operationTypeDao: OperationTypeDao,
    private val measurementUnitDao: MeasurementUnitDao,
    private val imageRepository: ImageRepository
) : ViewModel() {

    private val _selectedEquipmentIds = MutableStateFlow<Set<Int>>(emptySet())
    private val _selectedOperationTypeIds = MutableStateFlow<Set<Int>>(emptySet())
    
    private val _startDate = MutableStateFlow<Long?>(null)
    private val _endDate = MutableStateFlow<Long?>(null)
    private val _timeGranularity = MutableStateFlow(TimeGranularity.HOURS)
    private val _refreshTrigger = MutableStateFlow(0)
    private val _showDismissed = MutableStateFlow(false)

    private val selectionState = combine(
        _selectedEquipmentIds,
        _selectedOperationTypeIds,
        _startDate,
        _endDate,
        combine(_timeGranularity, _refreshTrigger, _showDismissed) { gran, refresh, dismissed -> Triple(gran, refresh, dismissed) }
    ) { selEquips, selOps, start, end, triple ->
        SelectionState(selEquips, selOps, start, end, triple.first, triple.second, triple.third)
    }

    val uiState: StateFlow<ReportsUiState> = combine(
        _showDismissed.flatMapLatest { if (it) equipmentDao.getAllEquipments() else equipmentDao.getActiveEquipments() },
        _showDismissed.flatMapLatest { if (it) operationTypeDao.getAllOperationTypes() else operationTypeDao.getActiveOperationTypes() },
        maintenanceLogDao.getLogsWithDetails(androidx.sqlite.db.SimpleSQLiteQuery("SELECT l.*, e.description as equipmentDescription, ot.description as operationTypeDescription, e.photoUri as equipmentPhotoUri, e.iconIdentifier as equipmentIconIdentifier, ot.photoUri as operationTypePhotoUri, ot.iconIdentifier as operationTypeIconIdentifier, e.dismissed as equipmentDismissed, ot.dismissed as operationTypeDismissed FROM maintenance_logs as l JOIN equipments as e ON l.equipmentId = e.id JOIN operation_types as ot ON l.operationTypeId = ot.id ORDER BY l.date ASC")),
        measurementUnitDao.getAllUnits(),
        combine(
            imageRepository.getCategoryColor(Category.EQUIPMENT),
            imageRepository.getCategoryColor(Category.OPERATION),
            selectionState
        ) { eColor, oColor, selections -> Triple(eColor, oColor, selections) }
    ) { equipments, operationTypes, logs, units, triple ->
        val (eColor, oColor, selections) = triple
        
        val filteredLogs = logs.filter { logDetail ->
            val date = logDetail.log.date
            val matchesDate = (selections.startDate == null || date >= selections.startDate) && 
                              (selections.endDate == null || date <= selections.endDate)
            val matchesVisibility = selections.showDismissed || !logDetail.log.dismissed
            matchesDate && matchesVisibility
        }

        // --- DISTRIBUZIONI (TORTE) ---
        // Occorrenze per Equipaggiamento (reali selezioni)
        val equipDist = filteredLogs
            .let { logsList ->
                if (selections.selectedEquipmentIds.isNotEmpty()) {
                    logsList.filter { it.log.equipmentId in selections.selectedEquipmentIds }
                } else logsList
            }
            .groupBy { it.log.equipmentId }
            .map { (id, logs) ->
                val label = equipments.find { it.id == id }?.description?.takeIf { it.isNotBlank() } ?: "ID: $id"
                PieChartPoint(label, logs.size.toFloat())
            }.sortedByDescending { it.value }

        // Occorrenze per Operazione (reali selezioni)
        val opDist = filteredLogs
            .let { logsList ->
                if (selections.selectedOperationTypeIds.isNotEmpty()) {
                    logsList.filter { it.log.operationTypeId in selections.selectedOperationTypeIds }
                } else logsList
            }
            .groupBy { it.log.operationTypeId }
            .map { (id, logs) ->
                val label = operationTypes.find { it.id == id }?.description?.takeIf { it.isNotBlank() } ?: "ID: $id"
                PieChartPoint(label, logs.size.toFloat())
            }.sortedByDescending { it.value }

        // --- ANDAMENTO (LINEE MULTIPLE) ---
        // Se non c'è selezione, mostriamo il primo per default per non avere grafici vuoti
        val activeEquipIds = selections.selectedEquipmentIds.ifEmpty { equipments.firstOrNull()?.id?.let { setOf(it) } ?: emptySet() }
        val activeOpIds = selections.selectedOperationTypeIds.ifEmpty { operationTypes.firstOrNull()?.id?.let { setOf(it) } ?: emptySet() }

        val equipChartData = activeEquipIds.associateWith { id ->
            val points = filteredLogs
                .filter { it.log.equipmentId == id && it.log.kilometers != null }
                .map { ChartPoint(it.log.date, it.log.kilometers!!.toFloat()) }
            aggregateData(points, selections.timeGranularity)
        }

        val opChartData = activeOpIds.associateWith { id ->
            val points = filteredLogs
                .filter { it.log.operationTypeId == id && it.log.kilometers != null }
                .map { ChartPoint(it.log.date, it.log.kilometers!!.toFloat(), it.equipmentDescription) }
            aggregateData(points, selections.timeGranularity)
        }
        
        // Unit label logic
        val selectedUnits = activeEquipIds.mapNotNull { id ->
            val equip = equipments.find { it.id == id }
            units.find { it.id == equip?.unitId }?.label
        }.distinct()
        
        val equipUnit = selectedUnits.joinToString(", ")
        val hasMixedUnits = selectedUnits.size > 1

        ReportsUiState(
            equipments = equipments,
            selectedEquipmentIds = selections.selectedEquipmentIds,
            equipmentChartData = equipChartData,
            equipmentDistribution = equipDist,
            equipmentCategoryColor = eColor ?: UiConstants.DEFAULT_FALLBACK_COLOR,
            equipmentUnitLabel = equipUnit,
            hasMixedUnits = hasMixedUnits,
            operationTypes = operationTypes,
            selectedOperationTypeIds = selections.selectedOperationTypeIds,
            operationChartData = opChartData,
            operationDistribution = opDist,
            operationCategoryColor = oColor ?: UiConstants.DEFAULT_FALLBACK_COLOR,
            startDate = selections.startDate,
            endDate = selections.endDate,
            timeGranularity = selections.timeGranularity,
            showDismissed = selections.showDismissed
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsUiState()
    )

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
        _selectedEquipmentIds.value = if (_selectedEquipmentIds.value.contains(id)) {
            _selectedEquipmentIds.value - id
        } else {
            _selectedEquipmentIds.value + id
        }
    }

    fun toggleOperationTypeSelection(id: Int) {
        _selectedOperationTypeIds.value = if (_selectedOperationTypeIds.value.contains(id)) {
            _selectedOperationTypeIds.value - id
        } else {
            _selectedOperationTypeIds.value + id
        }
    }

    fun invertEquipmentSelection() {
        val currentEquipments = uiState.value.equipments
        val currentSelected = _selectedEquipmentIds.value
        _selectedEquipmentIds.value = currentEquipments.map { it.id }.toSet() - currentSelected
    }

    fun clearEquipmentSelection() {
        _selectedEquipmentIds.value = emptySet()
    }

    fun invertOperationTypeSelection() {
        val currentOps = uiState.value.operationTypes
        val currentSelected = _selectedOperationTypeIds.value
        _selectedOperationTypeIds.value = currentOps.map { it.id }.toSet() - currentSelected
    }

    fun clearOperationTypeSelection() {
        _selectedOperationTypeIds.value = emptySet()
    }

    fun setDateRange(start: Long?, end: Long?) {
        _startDate.value = start
        _endDate.value = end
    }

    fun setTimeGranularity(granularity: TimeGranularity) {
        _timeGranularity.value = granularity
    }

    fun resetFilters() {
        _startDate.value = null
        _endDate.value = null
        _timeGranularity.value = TimeGranularity.HOURS
        _showDismissed.value = false
        _selectedEquipmentIds.value = emptySet()
        _selectedOperationTypeIds.value = emptySet()
    }
    
    fun toggleShowDismissed() {
        _showDismissed.value = !_showDismissed.value
    }
    
    fun refresh() {
        _refreshTrigger.value += 1
    }
}
