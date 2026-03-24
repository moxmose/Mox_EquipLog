package com.moxmose.moxequiplog.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moxmose.moxequiplog.data.ImageRepository
import com.moxmose.moxequiplog.data.local.*
import com.moxmose.moxequiplog.utils.UiConstants
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

data class ReportsUiState(
    val equipments: List<Equipment> = emptyList(),
    val selectedEquipmentId: Int? = null,
    val equipmentChartData: List<ChartPoint> = emptyList(),
    val equipmentUnitLabel: String = "",
    val equipmentCategoryColor: String = UiConstants.DEFAULT_FALLBACK_COLOR,
    
    val operationTypes: List<OperationType> = emptyList(),
    val selectedOperationTypeId: Int? = null,
    val operationChartData: List<ChartPoint> = emptyList(),
    val operationUnitLabel: String = "", // Generic label or first equipment's unit
    val operationCategoryColor: String = UiConstants.DEFAULT_FALLBACK_COLOR,

    // Time Filter State
    val startDate: Long? = null,
    val endDate: Long? = null,
    val timeGranularity: TimeGranularity = TimeGranularity.HOURS,
    val showDismissed: Boolean = false
)

private data class SelectionState(
    val selectedEquipmentId: Int?,
    val selectedOperationTypeId: Int?,
    val startDate: Long?,
    val endDate: Long?,
    val timeGranularity: TimeGranularity,
    val refreshKey: Int,
    val showDismissed: Boolean
)

class ReportsViewModel(
    private val equipmentDao: EquipmentDao,
    private val maintenanceLogDao: MaintenanceLogDao,
    private val operationTypeDao: OperationTypeDao,
    private val measurementUnitDao: MeasurementUnitDao,
    private val imageRepository: ImageRepository
) : ViewModel() {

    private val _selectedEquipmentId = MutableStateFlow<Int?>(null)
    private val _selectedOperationTypeId = MutableStateFlow<Int?>(null)
    
    private val _startDate = MutableStateFlow<Long?>(null)
    private val _endDate = MutableStateFlow<Long?>(null)
    private val _timeGranularity = MutableStateFlow(TimeGranularity.HOURS)
    private val _refreshTrigger = MutableStateFlow(0)
    private val _showDismissed = MutableStateFlow(false)

    private val selectionState = combine(
        _selectedEquipmentId,
        _selectedOperationTypeId,
        _startDate,
        _endDate,
        combine(_timeGranularity, _refreshTrigger, _showDismissed) { gran, refresh, dismissed -> Triple(gran, refresh, dismissed) }
    ) { selEquip, selOp, start, end, triple ->
        SelectionState(selEquip, selOp, start, end, triple.first, triple.second, triple.third)
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

        // Equipment Report Logic
        val currentEquipId = selections.selectedEquipmentId ?: equipments.firstOrNull()?.id
        val selectedEquipment = equipments.find { it.id == currentEquipId }
        val equipUnit = units.find { it.id == selectedEquipment?.unitId }?.label ?: ""
        
        val rawEquipPoints = filteredLogs
            .filter { it.log.equipmentId == currentEquipId && it.log.kilometers != null }
            .map { ChartPoint(it.log.date, it.log.kilometers!!.toFloat()) }
        
        val equipChartPoints = aggregateData(rawEquipPoints, selections.timeGranularity)

        // Operation Report Logic
        val currentOpId = selections.selectedOperationTypeId ?: operationTypes.firstOrNull()?.id
        val rawOpPoints = filteredLogs
            .filter { it.log.operationTypeId == currentOpId && it.log.kilometers != null }
            .map { 
                ChartPoint(
                    date = it.log.date, 
                    kilometers = it.log.kilometers!!.toFloat(),
                    label = it.equipmentDescription
                ) 
            }
        
        val opChartPoints = aggregateData(rawOpPoints, selections.timeGranularity)

        ReportsUiState(
            equipments = equipments,
            selectedEquipmentId = currentEquipId,
            equipmentChartData = equipChartPoints,
            equipmentUnitLabel = equipUnit,
            equipmentCategoryColor = eColor ?: UiConstants.DEFAULT_FALLBACK_COLOR,
            operationTypes = operationTypes,
            selectedOperationTypeId = currentOpId,
            operationChartData = opChartPoints,
            operationUnitLabel = "", 
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
                TimeGranularity.HOURS -> {
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                }
                TimeGranularity.DAYS -> {
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                }
                TimeGranularity.WEEKS -> {
                    calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                }
                TimeGranularity.MONTHS -> {
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                }
                TimeGranularity.YEARS -> {
                    calendar.set(Calendar.DAY_OF_YEAR, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                }
            }
            calendar.timeInMillis
        }.map { (timestamp, groupedPoints) ->
            // For kilometers, we usually want the maximum value in that period
            ChartPoint(timestamp, groupedPoints.maxOf { it.kilometers })
        }.sortedBy { it.date }
    }

    fun selectEquipment(id: Int) {
        _selectedEquipmentId.value = id
    }

    fun selectOperationType(id: Int) {
        _selectedOperationTypeId.value = id
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
    }
    
    fun toggleShowDismissed() {
        _showDismissed.value = !_showDismissed.value
    }
    
    fun refresh() {
        _refreshTrigger.value += 1
    }
}
