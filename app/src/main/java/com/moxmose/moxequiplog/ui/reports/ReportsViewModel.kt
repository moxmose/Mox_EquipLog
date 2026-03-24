package com.moxmose.moxequiplog.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moxmose.moxequiplog.data.local.*
import kotlinx.coroutines.flow.*

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
    
    val operationTypes: List<OperationType> = emptyList(),
    val selectedOperationTypeId: Int? = null,
    val operationChartData: List<ChartPoint> = emptyList(),
    val operationUnitLabel: String = "", // Generic label or first equipment's unit

    // Time Filter State
    val startDate: Long? = null,
    val endDate: Long? = null,
    val timeGranularity: TimeGranularity = TimeGranularity.HOURS
)

private data class SelectionState(
    val selectedEquipmentId: Int?,
    val selectedOperationTypeId: Int?,
    val startDate: Long?,
    val endDate: Long?,
    val timeGranularity: TimeGranularity
)

class ReportsViewModel(
    private val equipmentDao: EquipmentDao,
    private val maintenanceLogDao: MaintenanceLogDao,
    private val operationTypeDao: OperationTypeDao,
    private val measurementUnitDao: MeasurementUnitDao
) : ViewModel() {

    private val _selectedEquipmentId = MutableStateFlow<Int?>(null)
    private val _selectedOperationTypeId = MutableStateFlow<Int?>(null)
    
    private val _startDate = MutableStateFlow<Long?>(null)
    private val _endDate = MutableStateFlow<Long?>(null)
    private val _timeGranularity = MutableStateFlow(TimeGranularity.HOURS)

    private val selectionState = combine(
        _selectedEquipmentId,
        _selectedOperationTypeId,
        _startDate,
        _endDate,
        _timeGranularity
    ) { selEquip, selOp, start, end, gran ->
        SelectionState(selEquip, selOp, start, end, gran)
    }

    val uiState: StateFlow<ReportsUiState> = combine(
        equipmentDao.getActiveEquipments(),
        operationTypeDao.getActiveOperationTypes(),
        maintenanceLogDao.getLogsWithDetails(androidx.sqlite.db.SimpleSQLiteQuery("SELECT l.*, e.description as equipmentDescription, ot.description as operationTypeDescription, e.photoUri as equipmentPhotoUri, e.iconIdentifier as equipmentIconIdentifier, ot.photoUri as operationTypePhotoUri, ot.iconIdentifier as operationTypeIconIdentifier, e.dismissed as equipmentDismissed, ot.dismissed as operationTypeDismissed FROM maintenance_logs as l JOIN equipments as e ON l.equipmentId = e.id JOIN operation_types as ot ON l.operationTypeId = ot.id ORDER BY l.date ASC")),
        measurementUnitDao.getAllUnits(),
        selectionState
    ) { equipments, operationTypes, logs, units, selections ->
        
        val filteredLogs = logs.filter { logDetail ->
            val date = logDetail.log.date
            (selections.startDate == null || date >= selections.startDate) && 
            (selections.endDate == null || date <= selections.endDate)
        }

        // Equipment Report Logic
        val currentEquipId = selections.selectedEquipmentId ?: equipments.firstOrNull()?.id
        val selectedEquipment = equipments.find { it.id == currentEquipId }
        val equipUnit = units.find { it.id == selectedEquipment?.unitId }?.label ?: ""
        
        val equipChartPoints = filteredLogs
            .filter { it.log.equipmentId == currentEquipId && it.log.kilometers != null }
            .map { ChartPoint(it.log.date, it.log.kilometers!!.toFloat()) }

        // Operation Report Logic
        val currentOpId = selections.selectedOperationTypeId ?: operationTypes.firstOrNull()?.id
        val opChartPoints = filteredLogs
            .filter { it.log.operationTypeId == currentOpId && it.log.kilometers != null }
            .map { 
                ChartPoint(
                    date = it.log.date, 
                    kilometers = it.log.kilometers!!.toFloat(),
                    label = it.equipmentDescription
                ) 
            }

        ReportsUiState(
            equipments = equipments,
            selectedEquipmentId = currentEquipId,
            equipmentChartData = equipChartPoints,
            equipmentUnitLabel = equipUnit,
            operationTypes = operationTypes,
            selectedOperationTypeId = currentOpId,
            operationChartData = opChartPoints,
            operationUnitLabel = "", // Could be enhanced to show multiple units if needed
            startDate = selections.startDate,
            endDate = selections.endDate,
            timeGranularity = selections.timeGranularity
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsUiState()
    )

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
    }
}
