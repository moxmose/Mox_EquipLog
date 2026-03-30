package com.moxmose.moxequiplog.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.ImageRepository
import com.moxmose.moxequiplog.data.local.*
import com.moxmose.moxequiplog.utils.UiConstants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    val customColors: List<String> = emptyList()
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

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(
    private val equipmentDao: EquipmentDao,
    private val maintenanceLogDao: MaintenanceLogDao,
    private val operationTypeDao: OperationTypeDao,
    private val measurementUnitDao: MeasurementUnitDao,
    private val imageRepository: ImageRepository,
    private val appSettingsManager: AppSettingsManager
) : ViewModel() {

    private val _selectedEquipmentIds = MutableStateFlow<Set<Int>>(emptySet())
    private val _selectedOperationTypeIds = MutableStateFlow<Set<Int>>(emptySet())
    
    private var initializedEquipments = false
    private var initializedOperations = false
    
    private val _startDate = MutableStateFlow<Long?>(null)
    private val _endDate = MutableStateFlow<Long?>(null)
    private val _timeGranularity = MutableStateFlow(TimeGranularity.HOURS)
    private val _refreshTrigger = MutableStateFlow(0)
    private val _showDismissed = MutableStateFlow(false)

    init {
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
    }

    private val selectionState = combine(
        _selectedEquipmentIds,
        _selectedOperationTypeIds,
        _startDate,
        _endDate,
        combine(
            _timeGranularity, 
            _refreshTrigger, 
            _showDismissed,
            appSettingsManager.reportsColorMode,
            imageRepository.allColorsForReports.map { list -> 
                list.filter { !it.reportHidden }.map { it.hexValue } 
            }
        ) { gran, refresh, dismissed, mode, colors -> 
            Quintet(gran, refresh, dismissed, mode, colors) 
        }
    ) { selEquips, selOps, start, end, quintet ->
        SelectionState(
            selEquips, 
            selOps, 
            start, 
            end, 
            quintet.first, 
            quintet.second, 
            quintet.third, 
            quintet.fourth, 
            quintet.fifth
        )
    }

    private data class Quintet<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

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

        // Use stable order based on the master lists
        val sortedSelectedEquipIds = equipments.map { it.id }.filter { it in selections.selectedEquipmentIds }
        val sortedSelectedOpIds = operationTypes.map { it.id }.filter { it in selections.selectedOperationTypeIds }

        val equipDist = filteredLogs
            .filter { it.log.equipmentId in selections.selectedEquipmentIds }
            .groupBy { it.log.equipmentId }
            .map { (id, logs) ->
                val label = equipments.find { it.id == id }?.description?.takeIf { it.isNotBlank() } ?: "ID: $id"
                PieChartPoint(label, logs.size.toFloat())
            }.sortedByDescending { it.value }

        val opDist = filteredLogs
            .filter { it.log.operationTypeId in selections.selectedOperationTypeIds }
            .groupBy { it.log.operationTypeId }
            .map { (id, logs) ->
                val label = operationTypes.find { it.id == id }?.description?.takeIf { it.isNotBlank() } ?: "ID: $id"
                PieChartPoint(label, logs.size.toFloat())
            }.sortedByDescending { it.value }

        val equipChartData = sortedSelectedEquipIds.associateWith { id ->
            val points = filteredLogs
                .filter { it.log.equipmentId == id && it.log.kilometers != null }
                .map { ChartPoint(it.log.date, it.log.kilometers!!.toFloat()) }
            aggregateData(points, selections.timeGranularity)
        }

        val opChartData = sortedSelectedOpIds.associateWith { id ->
            val points = filteredLogs
                .filter { it.log.operationTypeId == id && it.log.kilometers != null }
                .map { ChartPoint(it.log.date, it.log.kilometers!!.toFloat(), it.equipmentDescription) }
            aggregateData(points, selections.timeGranularity)
        }
        
        val selectedUnits = sortedSelectedEquipIds.mapNotNull { id ->
            val equip = equipments.find { it.id == id }
            units.find { it.id == equip?.unitId }?.label
        }.distinct()
        
        val equipUnit = selectedUnits.joinToString(", ")
        val hasMixedUnits = selectedUnits.size > 1

        val opSelectedUnits = sortedSelectedOpIds.flatMap { opId ->
            filteredLogs.filter { it.log.operationTypeId == opId }
                .mapNotNull { logDetail ->
                    equipments.find { it.id == logDetail.log.equipmentId }?.unitId
                        ?.let { unitId -> units.find { it.id == unitId }?.label }
                }
        }.distinct()
        val opUnitLabel = opSelectedUnits.joinToString(", ")
        val opHasMixedUnits = opSelectedUnits.size > 1

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
            operationUnitLabel = opUnitLabel,
            opHasMixedUnits = opHasMixedUnits,
            startDate = selections.startDate,
            endDate = selections.endDate,
            timeGranularity = selections.timeGranularity,
            showDismissed = selections.showDismissed,
            colorMode = selections.colorMode,
            customColors = selections.customColors
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

    fun selectAllEquipment() {
        val currentEquipments = uiState.value.equipments
        _selectedEquipmentIds.value = currentEquipments.map { it.id }.toSet()
    }

    fun invertEquipmentSelection() {
        val currentEquipments = uiState.value.equipments
        val currentSelected = _selectedEquipmentIds.value
        _selectedEquipmentIds.value = currentEquipments.map { it.id }.toSet() - currentSelected
    }

    fun clearEquipmentSelection() {
        _selectedEquipmentIds.value = emptySet()
    }

    fun selectAllOperationTypes() {
        val currentOps = uiState.value.operationTypes
        _selectedOperationTypeIds.value = currentOps.map { it.id }.toSet()
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
        selectAllEquipment()
        selectAllOperationTypes()
    }
    
    fun toggleShowDismissed() {
        _showDismissed.value = !_showDismissed.value
    }
    
    fun refresh() {
        _refreshTrigger.value += 1
    }
}
