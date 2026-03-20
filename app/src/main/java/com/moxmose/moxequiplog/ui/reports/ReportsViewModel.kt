package com.moxmose.moxequiplog.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moxmose.moxequiplog.data.local.Equipment
import com.moxmose.moxequiplog.data.local.EquipmentDao
import com.moxmose.moxequiplog.data.local.MaintenanceLogDao
import com.moxmose.moxequiplog.utils.UiConstants
import kotlinx.coroutines.flow.*

data class ChartPoint(
    val date: Long,
    val kilometers: Float
)

data class ReportsUiState(
    val equipments: List<Equipment> = emptyList(),
    val selectedEquipmentId: Int? = null,
    val chartData: List<ChartPoint> = emptyList()
)

class ReportsViewModel(
    private val equipmentDao: EquipmentDao,
    private val maintenanceLogDao: MaintenanceLogDao
) : ViewModel() {

    private val _selectedEquipmentId = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<ReportsUiState> = combine(
        equipmentDao.getActiveEquipments(),
        _selectedEquipmentId,
        // Qui usiamo un trucco: osserviamo tutti i log e filtriamo in memoria per ora
        // In futuro potremmo aggiungere una query specifica nel DAO
        maintenanceLogDao.getLogsWithDetails(androidx.sqlite.db.SimpleSQLiteQuery("SELECT l.*, e.description as equipmentDescription, ot.description as operationTypeDescription, e.photoUri as equipmentPhotoUri, e.iconIdentifier as equipmentIconIdentifier, ot.photoUri as operationTypePhotoUri, ot.iconIdentifier as operationTypeIconIdentifier, e.dismissed as equipmentDismissed, ot.dismissed as operationTypeDismissed FROM maintenance_logs as l JOIN equipments as e ON l.equipmentId = e.id JOIN operation_types as ot ON l.operationTypeId = ot.id ORDER BY l.date ASC"))
    ) { equipments, selectedId, logs ->
        val currentId = selectedId ?: equipments.firstOrNull()?.id
        val chartPoints = logs
            .filter { it.log.equipmentId == currentId && it.log.kilometers != null }
            .map { ChartPoint(it.log.date, it.log.kilometers!!.toFloat()) }

        ReportsUiState(
            equipments = equipments,
            selectedEquipmentId = currentId,
            chartData = chartPoints
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsUiState()
    )

    fun selectEquipment(id: Int) {
        _selectedEquipmentId.value = id
    }
}
