package com.moxmose.moxequiplog.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moxmose.moxequiplog.data.local.Equipment
import com.moxmose.moxequiplog.data.local.EquipmentDao
import com.moxmose.moxequiplog.data.local.MaintenanceLogDao
import com.moxmose.moxequiplog.data.local.OperationType
import com.moxmose.moxequiplog.data.local.OperationTypeDao
import kotlinx.coroutines.flow.*

data class ChartPoint(
    val date: Long,
    val kilometers: Float,
    val label: String? = null
)

data class ReportsUiState(
    val equipments: List<Equipment> = emptyList(),
    val selectedEquipmentId: Int? = null,
    val equipmentChartData: List<ChartPoint> = emptyList(),
    
    val operationTypes: List<OperationType> = emptyList(),
    val selectedOperationTypeId: Int? = null,
    val operationChartData: List<ChartPoint> = emptyList()
)

class ReportsViewModel(
    private val equipmentDao: EquipmentDao,
    private val maintenanceLogDao: MaintenanceLogDao,
    private val operationTypeDao: OperationTypeDao
) : ViewModel() {

    private val _selectedEquipmentId = MutableStateFlow<Int?>(null)
    private val _selectedOperationTypeId = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<ReportsUiState> = combine(
        equipmentDao.getActiveEquipments(),
        operationTypeDao.getActiveOperationTypes(),
        _selectedEquipmentId,
        _selectedOperationTypeId,
        maintenanceLogDao.getLogsWithDetails(androidx.sqlite.db.SimpleSQLiteQuery("SELECT l.*, e.description as equipmentDescription, ot.description as operationTypeDescription, e.photoUri as equipmentPhotoUri, e.iconIdentifier as equipmentIconIdentifier, ot.photoUri as operationTypePhotoUri, ot.iconIdentifier as operationTypeIconIdentifier, e.dismissed as equipmentDismissed, ot.dismissed as operationTypeDismissed FROM maintenance_logs as l JOIN equipments as e ON l.equipmentId = e.id JOIN operation_types as ot ON l.operationTypeId = ot.id ORDER BY l.date ASC"))
    ) { equipments, operationTypes, selectedEquipId, selectedOpId, logs ->
        
        // Dati per il report Equipment (KM nel tempo)
        val currentEquipId = selectedEquipId ?: equipments.firstOrNull()?.id
        val equipChartPoints = logs
            .filter { it.log.equipmentId == currentEquipId && it.log.kilometers != null }
            .map { ChartPoint(it.log.date, it.log.kilometers!!.toFloat()) }

        // Dati per il report Operations (KM a cui è stata fatta l'operazione)
        val currentOpId = selectedOpId ?: operationTypes.firstOrNull()?.id
        val opChartPoints = logs
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
            operationTypes = operationTypes,
            selectedOperationTypeId = currentOpId,
            operationChartData = opChartPoints
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
}
