package com.moxmose.moxequiplog.ui.reports

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.sqlite.db.SimpleSQLiteQuery
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.ImageRepository
import com.moxmose.moxequiplog.data.MaintenanceManager
import com.moxmose.moxequiplog.data.SectionRepository
import com.moxmose.moxequiplog.data.local.BenchmarkData
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.ChartPoint
import com.moxmose.moxequiplog.data.local.Equipment
import com.moxmose.moxequiplog.data.local.EquipmentDao
import com.moxmose.moxequiplog.data.local.HeatmapPoint
import com.moxmose.moxequiplog.data.local.MaintenanceLogDao
import com.moxmose.moxequiplog.data.local.MaintenanceLogDetails
import com.moxmose.moxequiplog.data.local.MeasurementUnit
import com.moxmose.moxequiplog.data.local.MeasurementUnitDao
import com.moxmose.moxequiplog.data.local.OperationType
import com.moxmose.moxequiplog.data.local.OperationTypeDao
import com.moxmose.moxequiplog.data.local.PieChartPoint
import com.moxmose.moxequiplog.data.local.PredictionDetails
import com.moxmose.moxequiplog.data.local.ReportFilter
import com.moxmose.moxequiplog.data.local.ReportFilterDao
import com.moxmose.moxequiplog.data.local.Section
import com.moxmose.moxequiplog.data.local.TimeGranularity
import com.moxmose.moxequiplog.utils.AppConstants
import com.moxmose.moxequiplog.utils.ResourceProvider
import com.moxmose.moxequiplog.utils.UiConstants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Serializable
data class ReportFilterState(
    val selectedEquipmentIds: Set<Int> = emptySet(),
    val selectedOperationTypeIds: Set<Int> = emptySet(),
    val selectedSectionId: Int = AppConstants.ALL_SECTIONS_ID,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val timeGranularity: TimeGranularity? = null,
    val showDismissed: Boolean = false
)

data class ReportsUiState(
    val equipments: List<Equipment> = emptyList(),
    val selectedEquipmentIds: Set<Int> = emptySet(),
    val equipmentChartData: Map<Int, List<ChartPoint>> = emptyMap(),
    val equipmentDistribution: List<PieChartPoint> = emptyList(),
    val equipmentDistributionByPeriod: Map<String, List<PieChartPoint>> = emptyMap(),
    val equipmentCategoryColor: String = UiConstants.DEFAULT_FALLBACK_COLOR,
    val equipmentUnitLabel: String = "",
    val hasMixedUnits: Boolean = false,
    val equipmentMaxDecimalPlaces: Int = 0,
    
    val operationTypes: List<OperationType> = emptyList(),
    val selectedOperationTypeIds: Set<Int> = emptySet(),
    val operationChartData: Map<Int, List<ChartPoint>> = emptyMap(),
    val operationDistribution: List<PieChartPoint> = emptyList(),
    val operationDistributionByPeriod: Map<String, List<PieChartPoint>> = emptyMap(),
    val operationCategoryColor: String = UiConstants.DEFAULT_FALLBACK_COLOR,
    val operationUnitLabel: String = "",
    val opHasMixedUnits: Boolean = false,
    val operationMaxDecimalPlaces: Int = 0,

    val sections: List<Section> = emptyList(),
    val selectedSectionId: Int = AppConstants.ALL_SECTIONS_ID,
    val selectedSectionName: String? = null,
    val sectionDistribution: List<PieChartPoint> = emptyList(),
    val showDismissedSections: Boolean = false,
    val sectionSelectorType: String = UiConstants.DEFAULT_SECTION_SELECTOR_TYPE,

    // New Analysis Data
    val intervalData: Map<Int, List<ChartPoint>> = emptyMap(),
    val heatmapData: List<HeatmapPoint> = emptyList(),
    val benchmarkData: List<BenchmarkData> = emptyList(),
    val benchmarkByPeriod: Map<String, List<BenchmarkData>> = emptyMap(),

    // Extra Reports (Volume & Combined)
    val equipmentVolumeData: Map<Int, List<ChartPoint>> = emptyMap(),
    val operationVolumeData: Map<Int, List<ChartPoint>> = emptyMap(),
    val combinedLogsData: Map<String, List<ChartPoint>> = emptyMap(),

    // Cost Analysis Data
    val equipmentCostData: Map<Int, List<ChartPoint>> = emptyMap(),
    val operationCostData: Map<Int, List<ChartPoint>> = emptyMap(),
    val costDistributionByEquipment: List<PieChartPoint> = emptyList(),
    val costDistributionByOperation: List<PieChartPoint> = emptyList(),
    val totalCost: Double = 0.0,
    val averageCostPerLog: Double = 0.0,
    val costVsUsageData: Map<Int, List<ChartPoint>> = emptyMap(),

    // Journal Data
    val journalLogs: List<MaintenanceLogDetails> = emptyList(),
    val predictions: List<PredictionDetails> = emptyList(),

    val startDate: Long? = null,
    val endDate: Long? = null,
    val timeGranularity: TimeGranularity? = null,
    val effectiveGranularity: TimeGranularity? = null,
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
    val selectedSectionId: Int,
    val startDate: Long?,
    val endDate: Long?,
    val timeGranularity: TimeGranularity?,
    val refreshKey: Int,
    val showDismissed: Boolean,
    val colorMode: String,
    val customColors: List<String>
)

private data class FilterCore(val equips: List<Equipment>, val ops: List<OperationType>, val logs: List<MaintenanceLogDetails>, val units: List<MeasurementUnit>, val sections: List<Section>)
private data class FilterStyles(val eColor: String?, val oColor: String?, val selections: SelectionState)
private data class StateExtras(val saved: List<ReportFilter>, val active: ReportFilter?, val current: ReportFilterState, val showDismissedSections: Boolean, val sectionSelectorType: String)
private data class FilterPersistence(val saved: List<ReportFilter>, val active: ReportFilter?, val current: ReportFilterState)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class ReportsViewModel(
    private val equipmentDao: EquipmentDao,
    private val maintenanceLogDao: MaintenanceLogDao,
    private val operationTypeDao: OperationTypeDao,
    private val measurementUnitDao: MeasurementUnitDao,
    private val imageRepository: ImageRepository,
    private val appSettingsManager: AppSettingsManager,
    private val reportFilterDao: ReportFilterDao,
    private val maintenanceManager: MaintenanceManager,
    private val resourceProvider: ResourceProvider,
    private val sectionRepository: SectionRepository
) : ViewModel() {

    private val _selectedEquipmentIds = MutableStateFlow<Set<Int>>(emptySet())
    private val _selectedOperationTypeIds = MutableStateFlow<Set<Int>>(emptySet())
    private val _selectedSectionId = MutableStateFlow(AppConstants.ALL_SECTIONS_ID)
    private val _startDate = MutableStateFlow<Long?>(null)
    private val _endDate = MutableStateFlow<Long?>(null)
    private val _timeGranularity = MutableStateFlow<TimeGranularity?>(null)
    private val _refreshTrigger = MutableStateFlow(0)
    private val _showDismissed = MutableStateFlow(false)
    private val _showDismissedSections = MutableStateFlow(false)
    private val _activeFilter = MutableStateFlow<ReportFilter?>(null)

    private var initializedEquipments = false
    private var initializedOperations = false

    private val currentState: Flow<ReportFilterState> = combine(
        combine(_selectedEquipmentIds, _selectedOperationTypeIds) { ids, ops -> ids to ops },
        combine(_startDate, _endDate) { s, e -> s to e },
        _timeGranularity,
        _showDismissed,
        _selectedSectionId
    ) { base, dates, gran, dismissed, sectionId ->
        ReportFilterState(base.first, base.second, sectionId, dates.first, dates.second, gran, dismissed)
    }

    private val selectionState: Flow<SelectionState> = combine(
        currentState,
        _refreshTrigger,
        appSettingsManager.reportsColorMode,
        appSettingsManager.reportsCustomColors,
        imageRepository.allColorsForReports
    ) { state, refresh, mode, custom, dbColors ->
        val dbColorList = dbColors.filter { !it.reportHidden }.map { it.hexValue }.takeIf { it.isNotEmpty() } 
            ?: UiConstants.DEFAULT_PALETTE

        val palette = if (mode == UiConstants.REPORTS_COLOR_MODE_CUSTOM) {
            custom ?: dbColorList
        } else {
            dbColorList
        }
        SelectionState(state.selectedEquipmentIds, state.selectedOperationTypeIds, state.selectedSectionId, state.startDate, state.endDate, state.timeGranularity, refresh, state.showDismissed, mode, palette)
    }

    init {
        viewModelScope.launch {
            val lastSession = reportFilterDao.getLastSession("ALL_REPORTS").firstOrNull()
            lastSession?.let { session ->
                try {
                    val state = Json.decodeFromString<ReportFilterState>(session.filterJson)
                    _selectedEquipmentIds.value = state.selectedEquipmentIds
                    _selectedOperationTypeIds.value = state.selectedOperationTypeIds
                    _selectedSectionId.value = state.selectedSectionId
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
            _showDismissed.flatMapLatest { if (it) equipmentDao.getAllEquipmentList() else equipmentDao.getActiveEquipmentList() }
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
            _showDismissed.flatMapLatest { if (it) equipmentDao.getAllEquipmentList() else equipmentDao.getActiveEquipmentList() }.distinctUntilChanged(),
            _showDismissed.flatMapLatest { if (it) operationTypeDao.getAllOperationTypes() else operationTypeDao.getActiveOperationTypes() }.distinctUntilChanged(),
            maintenanceLogDao.getLogsWithDetails(SimpleSQLiteQuery("SELECT l.*, e.description as equipmentDescription, ot.description as operationTypeDescription, e.photoUri as equipmentPhotoUri, e.iconIdentifier as equipmentIconIdentifier, ot.photoUri as operationTypePhotoUri, ot.iconIdentifier as operationTypeIconIdentifier, e.dismissed as equipmentDismissed, ot.dismissed as operationTypeDismissed, ot.isResettable as operationTypeIsResettable, ot.isSystem as operationTypeIsSystem, ot.hasValue as operationTypeHasValue, e.unitId as equipmentUnitId, ot.unitId as operationTypeUnitId, e.sectionId as equipmentSectionId, ot.sectionId as operationSectionId FROM maintenance_logs as l JOIN equipments as e ON l.equipmentId = e.id JOIN operation_types as ot ON l.operationTypeId = ot.id ORDER BY l.date ASC")).distinctUntilChanged(),
            measurementUnitDao.getAllUnits().distinctUntilChanged(),
            sectionRepository.allSections.distinctUntilChanged()
        ) { e, o, l, u, s -> FilterCore(e, o, l, u, s) },
        combine(
            imageRepository.getCategoryColor(Category.EQUIPMENT),
            imageRepository.getCategoryColor(Category.OPERATION),
            selectionState
        ) { ec, oc, s -> FilterStyles(ec, oc, s) },
        combine(
            reportFilterDao.getSavedFilters("ALL_REPORTS"),
            _activeFilter,
            currentState,
            _showDismissedSections,
            appSettingsManager.sectionSelectorType
        ) { saved, active, current, showSect, sectType -> 
            StateExtras(saved, active, current, showSect, sectType) 
        }
    ) { core, style, extras ->
        val sectionId = style.selections.selectedSectionId

        val filteredLogs = core.logs.filter { logDetail ->
            val date = logDetail.log.date
            val matchesDate = (style.selections.startDate == null || date >= style.selections.startDate) && 
                              (style.selections.endDate == null || date <= style.selections.endDate)
            val matchesVisibility = style.selections.showDismissed || !logDetail.log.dismissed
            val matchesSection = sectionId == AppConstants.ALL_SECTIONS_ID || 
                                 logDetail.equipmentSectionId == sectionId || 
                                 logDetail.operationSectionId == sectionId
            matchesDate && matchesVisibility && matchesSection
        }

        val fullyFilteredLogs = filteredLogs.filter { 
            it.log.equipmentId in style.selections.selectedEquipmentIds && 
            it.log.operationTypeId in style.selections.selectedOperationTypeIds 
        }

        val allPoints = fullyFilteredLogs.map { ChartPoint(it.log.date, it.log.value?.toFloat() ?: 0f) }
        val effectiveGranularity = if (style.selections.timeGranularity != null) {
            maintenanceManager.findBestGranularity(allPoints, style.selections.timeGranularity, false, false)
        } else {
            maintenanceManager.findAutoGranularity(allPoints, false, false)
        }

        val displayedEquips = if (sectionId == AppConstants.ALL_SECTIONS_ID) core.equips 
                              else core.equips.filter { it.sectionId == sectionId }
        val displayedOps = if (sectionId == AppConstants.ALL_SECTIONS_ID) core.ops
                           else {
                               val sectionOps = core.ops.filter { it.sectionId == sectionId }
                               val commonOps = core.ops.filter { it.sectionId == AppConstants.DEFAULT_SECTION_ID }
                               (sectionOps + commonOps).distinctBy { it.id }.sortedBy { it.displayOrder }
                           }

        val sortedSelectedEquipIds = core.equips.map { it.id }.filter { it in style.selections.selectedEquipmentIds }
        val sortedSelectedOpIds = core.ops.map { it.id }.filter { it in style.selections.selectedOperationTypeIds }
        
        val currentPalette = style.selections.customColors

        // Standard Trends
        val equipChartData = sortedSelectedEquipIds.associateWith { id ->
            val points = fullyFilteredLogs.filter { it.log.equipmentId == id && it.log.value != null }.map { ChartPoint(it.log.date, it.log.value!!.toFloat()) }
            val aggregated = maintenanceManager.aggregateData(points, effectiveGranularity)
            if (style.selections.timeGranularity == TimeGranularity.MONTHS) {
                Log.d("ReportsDebug", "EQUIP ID $id aggregated points: ${aggregated.size}")
                aggregated.forEach { Log.d("ReportsDebug", "  Point: ${Date(it.date)} -> ${it.value}") }
            }
            aggregated
        }

        val opChartData = sortedSelectedOpIds.associateWith { id ->
            val points = fullyFilteredLogs.filter { it.log.operationTypeId == id && it.log.value != null }.map { ChartPoint(it.log.date, it.log.value!!.toFloat(), it.equipmentDescription) }
            val aggregated = maintenanceManager.aggregateData(points, effectiveGranularity)
            if (style.selections.timeGranularity == TimeGranularity.MONTHS) {
                Log.d("ReportsDebug", "OP ID $id aggregated points: ${aggregated.size}")
                aggregated.forEach { Log.d("ReportsDebug", "  Point: ${Date(it.date)} -> ${it.value}") }
            }
            aggregated
        }

        // Global Distributions
        val equipDist = fullyFilteredLogs.groupBy { it.log.equipmentId }.map { (id, logs) ->
            val label = core.equips.find { it.id == id }?.description?.takeIf { it.isNotBlank() } ?: "ID: $id"
            val index = core.equips.indexOfFirst { it.id == id }
            val color = if (index != -1) currentPalette[index % currentPalette.size] else null
            PieChartPoint(label, logs.size.toFloat(), color, id)
        }.sortedByDescending { it.value }

        val opDist = fullyFilteredLogs.groupBy { it.log.operationTypeId }.map { (id, logs) ->
            val label = core.ops.find { it.id == id }?.description?.takeIf { it.isNotBlank() } ?: "ID: $id"
            val index = core.ops.indexOfFirst { it.id == id }
            val color = if (index != -1) currentPalette[index % currentPalette.size] else null
            PieChartPoint(label, logs.size.toFloat(), color, id)
        }.sortedByDescending { it.value }

        val sectionDist = fullyFilteredLogs.groupBy { it.equipmentSectionId }.map { (id, logs) ->
            val section = core.sections.find { it.id == id }
            val label = section?.name?.takeIf { it.isNotBlank() } ?: "ID: $id"
            val color = section?.color
            PieChartPoint(label, logs.size.toFloat(), color, id ?: 0)
        }.sortedByDescending { it.value }

        // Period-based Distributions
        val equipDistByPeriod = if (effectiveGranularity != null) {
            val dateFormat = maintenanceManager.getPeriodFormat(effectiveGranularity)
            val periods = fullyFilteredLogs.groupBy { dateFormat.format(Date(it.log.date)) }
            if (effectiveGranularity == TimeGranularity.HOURS && periods.size > 60) emptyMap()
            else {
                periods.mapValues { (_, logsInPeriod) ->
                    logsInPeriod.groupBy { it.log.equipmentId }.map { (id, logs) ->
                        val label = core.equips.find { it.id == id }?.description?.takeIf { it.isNotBlank() } ?: "ID: $id"
                        val index = core.equips.indexOfFirst { it.id == id }
                        val color = if (index != -1) currentPalette[index % currentPalette.size] else null
                        PieChartPoint(label, logs.size.toFloat(), color, id)
                    }.sortedByDescending { it.value }
                }
            }
        } else emptyMap()

        val opDistByPeriod = if (effectiveGranularity != null) {
            val dateFormat = maintenanceManager.getPeriodFormat(effectiveGranularity)
            val periods = fullyFilteredLogs.groupBy { dateFormat.format(Date(it.log.date)) }
            if (effectiveGranularity == TimeGranularity.HOURS && periods.size > 60) emptyMap()
            else {
                periods.mapValues { (_, logsInPeriod) ->
                    logsInPeriod.groupBy { it.log.operationTypeId }.map { (id, logs) ->
                        val label = core.ops.find { it.id == id }?.description?.takeIf { it.isNotBlank() } ?: "ID: $id"
                        val index = core.ops.indexOfFirst { it.id == id }
                        val color = if (index != -1) currentPalette[index % currentPalette.size] else null
                        PieChartPoint(label, logs.size.toFloat(), color, id)
                    }.sortedByDescending { it.value }
                }
            }
        } else emptyMap()

        // KPI
        val intervalData = sortedSelectedEquipIds.associateWith { id ->
            val rawPoints = fullyFilteredLogs.filter { it.log.equipmentId == id && it.log.value != null }
                .map { ChartPoint(it.log.date, it.log.value!!.toFloat()) }
                .sortedBy { it.date }
            
            val deltas = mutableListOf<ChartPoint>()
            for (i in 1 until rawPoints.size) {
                val delta = (rawPoints[i].value - rawPoints[i-1].value).coerceAtLeast(0f)
                deltas.add(ChartPoint(rawPoints[i].date, delta))
            }
            maintenanceManager.aggregateData(deltas, effectiveGranularity, isDelta = true)
        }

        // Heatmap
        val cal = Calendar.getInstance()
        val heatmapData = fullyFilteredLogs.groupBy { log ->
            cal.timeInMillis = log.log.date
            cal.get(Calendar.DAY_OF_WEEK) to cal.get(Calendar.MONTH)
        }.map { (key, logs) -> HeatmapPoint(key.first, key.second, logs.size) }

        // Benchmarking
        val benchmarkData = sortedSelectedEquipIds.map { id ->
            val equipLogs = fullyFilteredLogs.filter { it.log.equipmentId == id && it.log.value != null }.sortedBy { it.log.date }
            val totalUsage = if (equipLogs.size > 1) (equipLogs.last().log.value!! - equipLogs.first().log.value!!).toFloat() 
                             else equipLogs.firstOrNull()?.log?.value?.toFloat() ?: 0f
            val intervals = mutableListOf<Float>()
            for (i in 1 until equipLogs.size) { intervals.add((equipLogs[i].log.value!! - equipLogs[i-1].log.value!!).toFloat()) }
            val avg = if (intervals.isNotEmpty()) intervals.average().toFloat() else 0f
            BenchmarkData(equipmentName = core.equips.find { it.id == id }?.description ?: "ID: $id", totalValue = totalUsage, avgInterval = avg, count = equipLogs.size)
        }

        val benchmarkByPeriod = if (effectiveGranularity != null && effectiveGranularity != TimeGranularity.HOURS) {
            val dateFormat = maintenanceManager.getPeriodFormat(effectiveGranularity)
            val logsByPeriod = fullyFilteredLogs.groupBy { dateFormat.format(Date(it.log.date)) }
            logsByPeriod.mapValues { (period, _) ->
                sortedSelectedEquipIds.map { id ->
                    val usageInPeriod = intervalData[id]?.find { dateFormat.format(Date(it.date)) == period }?.value ?: 0f
                    val countInPeriod = fullyFilteredLogs.count { it.log.equipmentId == id && dateFormat.format(Date(it.log.date)) == period }
                    val avgInPeriod = if (countInPeriod > 0) usageInPeriod / countInPeriod else 0f
                    BenchmarkData(equipmentName = core.equips.find { it.id == id }?.description ?: "ID: $id", totalValue = usageInPeriod, avgInterval = avgInPeriod, count = countInPeriod, periodLabel = period)
                }
            }
        } else emptyMap()

        // Activity Volume
        val equipmentVolumeData = sortedSelectedEquipIds.associateWith { id ->
            val points = fullyFilteredLogs.filter { it.log.equipmentId == id }.map { ChartPoint(it.log.date, 1f) }
            maintenanceManager.aggregateData(points, effectiveGranularity, isCount = true)
        }
        val operationVolumeData = sortedSelectedOpIds.associateWith { id ->
            val points = fullyFilteredLogs.filter { it.log.operationTypeId == id }.map { ChartPoint(it.log.date, 1f) }
            maintenanceManager.aggregateData(points, effectiveGranularity, isCount = true)
        }

        // Combined Logs
        val combinedLogsData = mutableMapOf<String, List<ChartPoint>>()
        sortedSelectedEquipIds.forEach { eId ->
            sortedSelectedOpIds.forEach { oId ->
                val logs = fullyFilteredLogs.filter { it.log.equipmentId == eId && it.log.operationTypeId == oId && it.log.value != null }
                if (logs.isNotEmpty()) {
                    val eDesc = core.equips.find { it.id == eId }?.description ?: "E$eId"
                    val oDesc = core.ops.find { it.id == oId }?.description ?: "O$oId"
                    val points = logs.map { ChartPoint(it.log.date, it.log.value!!.toFloat()) }
                    combinedLogsData["$eDesc - $oDesc"] = maintenanceManager.aggregateData(points, effectiveGranularity)
                }
            }
        }

        // Cost Analysis
        val equipmentCostData = sortedSelectedEquipIds.associateWith { id ->
            val points = fullyFilteredLogs.filter { it.log.equipmentId == id && it.log.cost != null }.map { ChartPoint(it.log.date, it.log.cost!!.toFloat()) }
            maintenanceManager.aggregateData(points, effectiveGranularity, isDelta = true)
        }

        val operationCostData = sortedSelectedOpIds.associateWith { id ->
            val points = fullyFilteredLogs.filter { it.log.operationTypeId == id && it.log.cost != null }.map { ChartPoint(it.log.date, it.log.cost!!.toFloat()) }
            maintenanceManager.aggregateData(points, effectiveGranularity, isDelta = true)
        }

        val costDistEquip = fullyFilteredLogs.filter { it.log.cost != null }.groupBy { it.log.equipmentId }.map { (id, logs) ->
            val label = core.equips.find { it.id == id }?.description?.takeIf { it.isNotBlank() } ?: "ID: $id"
            val index = core.equips.indexOfFirst { it.id == id }
            val color = if (index != -1) currentPalette[index % currentPalette.size] else null
            PieChartPoint(label, logs.sumOf { it.log.cost!! }.toFloat(), color, id)
        }.sortedByDescending { it.value }

        val costDistOp = fullyFilteredLogs.filter { it.log.cost != null }.groupBy { it.log.operationTypeId }.map { (id, logs) ->
            val label = core.ops.find { it.id == id }?.description?.takeIf { it.isNotBlank() } ?: "ID: $id"
            val index = core.ops.indexOfFirst { it.id == id }
            val color = if (index != -1) currentPalette[index % currentPalette.size] else null
            PieChartPoint(label, logs.sumOf { it.log.cost!! }.toFloat(), color, id)
        }.sortedByDescending { it.value }

        val logsForCostSummary = fullyFilteredLogs.filter { it.log.cost != null }
        val totalCostVal = logsForCostSummary.sumOf { it.log.cost!! }
        val avgCostPerLog = if (logsForCostSummary.isNotEmpty()) totalCostVal / logsForCostSummary.size else 0.0

        val costVsUsageData = sortedSelectedEquipIds.associateWith { id ->
            val logs = fullyFilteredLogs.filter { it.log.equipmentId == id && it.log.cost != null && it.log.value != null }.sortedBy { it.log.date }
            if (logs.size >= 2) {
                val points = mutableListOf<ChartPoint>()
                for (i in 1 until logs.size) {
                    val deltaUsage = (logs[i].log.value!! - logs[i-1].log.value!!).coerceAtLeast(0.0)
                    val cost = logs[i].log.cost!!
                    val ratio = if (deltaUsage > 0) (cost / deltaUsage).toFloat() else 0f
                    points.add(ChartPoint(logs[i].log.date, ratio))
                }
                maintenanceManager.aggregateData(points, effectiveGranularity)
            } else emptyList()
        }
        
        val selectedEquipUnits = sortedSelectedEquipIds.mapNotNull { id -> core.units.find { it.id == core.equips.find { e -> e.id == id }?.unitId } }
        val selectedUnits = selectedEquipUnits.map { it.label }.distinct()
        val equipMaxDecimals = selectedEquipUnits.maxOfOrNull { it.decimalPlaces } ?: 0
        val selectedOpUnits = fullyFilteredLogs.mapNotNull { logDetail -> core.equips.find { it.id == logDetail.log.equipmentId }?.unitId?.let { unitId -> core.units.find { it.id == unitId } } }
        val opSelectedUnitLabels = selectedOpUnits.map { it.label }.distinct()
        val opMaxDecimals = selectedOpUnits.maxOfOrNull { it.decimalPlaces } ?: 0

        ReportsUiState(
            equipments = displayedEquips, selectedEquipmentIds = style.selections.selectedEquipmentIds, equipmentChartData = equipChartData,
            equipmentDistribution = equipDist, equipmentDistributionByPeriod = equipDistByPeriod,
            equipmentCategoryColor = style.eColor ?: UiConstants.DEFAULT_FALLBACK_COLOR,
            equipmentUnitLabel = selectedUnits.joinToString(", "), hasMixedUnits = selectedUnits.size > 1,
            equipmentMaxDecimalPlaces = equipMaxDecimals,
            operationTypes = displayedOps, selectedOperationTypeIds = style.selections.selectedOperationTypeIds, operationChartData = opChartData,
            operationDistribution = opDist, operationDistributionByPeriod = opDistByPeriod,
            operationCategoryColor = style.oColor ?: UiConstants.DEFAULT_FALLBACK_COLOR,
            operationUnitLabel = opSelectedUnitLabels.joinToString(", "), opHasMixedUnits = opSelectedUnitLabels.size > 1,
            operationMaxDecimalPlaces = opMaxDecimals,
            sections = core.sections, selectedSectionId = sectionId, selectedSectionName = core.sections.find { it.id == sectionId }?.name,
            sectionDistribution = sectionDist,
            showDismissedSections = extras.showDismissedSections, sectionSelectorType = extras.sectionSelectorType,
            intervalData = intervalData, heatmapData = heatmapData, benchmarkData = benchmarkData, benchmarkByPeriod = benchmarkByPeriod,
            equipmentVolumeData = equipmentVolumeData, operationVolumeData = operationVolumeData, combinedLogsData = combinedLogsData,
            startDate = style.selections.startDate, endDate = style.selections.endDate, timeGranularity = style.selections.timeGranularity,
            effectiveGranularity = effectiveGranularity,
            showDismissed = style.selections.showDismissed, colorMode = style.selections.colorMode, customColors = currentPalette,
            savedFilters = extras.saved, activeFilterName = extras.active?.name, isFilterDirty = extras.active != null && extras.active.filterJson != Json.encodeToString(extras.current),
            equipmentCostData = equipmentCostData, operationCostData = operationCostData,
            costDistributionByEquipment = costDistEquip, costDistributionByOperation = costDistOp,
            totalCost = totalCostVal, averageCostPerLog = avgCostPerLog,
            costVsUsageData = costVsUsageData,
            journalLogs = fullyFilteredLogs
        )
    }.flatMapLatest { state ->
        kotlinx.coroutines.flow.flow {
            val predictions = mutableListOf<PredictionDetails>()
            state.selectedEquipmentIds.forEach { eId ->
                val equip = state.equipments.find { it.id == eId } ?: return@forEach
                val trend = maintenanceManager.calculateTrend(equip)
                state.selectedOperationTypeIds.forEach { oId ->
                    val opType = state.operationTypes.find { it.id == oId } ?: return@forEach
                    if (opType.isPredictable) {
                        val lastLog = maintenanceLogDao.getLastLogForEquipmentAndOperation(eId, oId)
                        if (lastLog != null) {
                            val predictionResult = maintenanceManager.getOperationPrediction(eId, opType, lastLog, trend)
                            if (predictionResult != null) {
                                predictions.add(PredictionDetails(
                                    equipmentId = eId,
                                    equipmentDescription = equip.description,
                                    operationTypeId = oId,
                                    operationTypeDescription = opType.description,
                                    predictedDate = predictionResult.first,
                                    equipmentPhotoUri = equip.photoUri,
                                    equipmentIconIdentifier = equip.iconIdentifier,
                                    operationTypePhotoUri = opType.photoUri,
                                    operationTypeIconIdentifier = opType.iconIdentifier
                                ))
                            }
                        }
                    }
                }
            }
            emit(state.copy(predictions = predictions))
        }
    }.distinctUntilChanged()
    .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), initialValue = ReportsUiState())

    fun toggleEquipmentSelection(id: Int) { _selectedEquipmentIds.value = if (_selectedEquipmentIds.value.contains(id)) _selectedEquipmentIds.value - id else _selectedEquipmentIds.value + id }
    fun toggleOperationTypeSelection(id: Int) { _selectedOperationTypeIds.value = if (_selectedOperationTypeIds.value.contains(id)) _selectedOperationTypeIds.value - id else _selectedOperationTypeIds.value + id }
    fun selectAllEquipment() { _selectedEquipmentIds.value = uiState.value.equipments.map { it.id }.toSet() }
    fun invertEquipmentSelection() { _selectedEquipmentIds.value = uiState.value.equipments.map { it.id }.toSet() - _selectedEquipmentIds.value }
    fun clearEquipmentSelection() { _selectedEquipmentIds.value = emptySet() }
    fun selectAllOperationTypes() { _selectedOperationTypeIds.value = uiState.value.operationTypes.map { it.id }.toSet() }
    fun invertOperationTypeSelection() { _selectedOperationTypeIds.value = uiState.value.operationTypes.map { it.id }.toSet() - _selectedOperationTypeIds.value }
    fun clearOperationTypeSelection() { _selectedOperationTypeIds.value = emptySet() }
    fun setDateRange(start: Long?, end: Long?) { _startDate.value = start; _endDate.value = end }
    fun setTimeGranularity(granularity: TimeGranularity?) { _timeGranularity.value = if (_timeGranularity.value == granularity) null else granularity }
    fun resetFilters() { 
        _startDate.value = null
        _endDate.value = null
        _timeGranularity.value = null
        _activeFilter.value = null
        _showDismissed.value = false
    }
    fun resetDateFilters() { _startDate.value = null; _endDate.value = null }
    fun toggleShowDismissed() { _showDismissed.value = !_showDismissed.value }
    fun refresh() { _refreshTrigger.value += 1 }

    fun saveAsNewFilter(name: String) {
        viewModelScope.launch {
            val state = ReportFilterState(_selectedEquipmentIds.value, _selectedOperationTypeIds.value, _selectedSectionId.value, _startDate.value, _endDate.value, _timeGranularity.value, _showDismissed.value)
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
            val state = ReportFilterState(_selectedEquipmentIds.value, _selectedOperationTypeIds.value, _selectedSectionId.value, _startDate.value, _endDate.value, _timeGranularity.value, _showDismissed.value)
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
            _selectedSectionId.value = state.selectedSectionId
            _startDate.value = state.startDate
            _endDate.value = state.endDate
            _timeGranularity.value = state.timeGranularity
            _showDismissed.value = state.showDismissed
            _activeFilter.value = filter
            refresh()
        } catch (e: Exception) { }
    }

    fun onSectionSelected(id: Int) {
        _selectedSectionId.value = id
    }

    fun onToggleShowDismissedSections() {
        _showDismissedSections.value = !_showDismissedSections.value
    }

    fun deleteSavedFilter(id: Int) {
        viewModelScope.launch {
            if (_activeFilter.value?.id == id) _activeFilter.value = null
            reportFilterDao.deleteFilter(id)
        }
    }

    fun getCsvExportData(reportTitle: String): String {
        val state = uiState.value
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        
        val reportLabel = resourceProvider.getString(R.string.csv_export_report_label)
        val exportDateLabel = resourceProvider.getString(R.string.csv_export_date_label)
        val periodLabel = resourceProvider.getString(R.string.csv_export_period_label)
        val startLabel = resourceProvider.getString(R.string.csv_export_start_label)
        val endLabel = resourceProvider.getString(R.string.csv_export_end_label)
        val dataHeader = resourceProvider.getString(R.string.csv_export_data_header)
        val equipHeader = resourceProvider.getString(R.string.csv_export_equipment_header)
        val opHeader = resourceProvider.getString(R.string.csv_export_operation_header)
        val valHeader = resourceProvider.getString(R.string.csv_export_value_header)
        val unitHeader = resourceProvider.getString(R.string.csv_export_unit_header)

        sb.append("$reportLabel;${reportTitle}\n")
        sb.append("$exportDateLabel;${dateFormat.format(Date())}\n")
        sb.append("$periodLabel;${state.startDate?.let { dateFormat.format(Date(it)) } ?: startLabel} - ${state.endDate?.let { dateFormat.format(Date(it)) } ?: endLabel}\n\n")
        
        if (state.equipmentChartData.any { it.value.isNotEmpty() }) {
            sb.append("$dataHeader;$equipHeader;$valHeader;$unitHeader\n")
            state.equipmentChartData.forEach { (id, points) ->
                val name = state.equipments.find { it.id == id }?.description ?: "ID $id"
                points.forEach { p ->
                    val formattedValue = String.format(Locale.getDefault(), "%.${state.equipmentMaxDecimalPlaces}f", p.value)
                    sb.append("${dateFormat.format(Date(p.date))};\"$name\";$formattedValue;${state.equipmentUnitLabel}\n")
                }
            }
        } else if (state.operationChartData.any { it.value.isNotEmpty() }) {
            sb.append("$dataHeader;$opHeader;$valHeader;$unitHeader\n")
            state.operationChartData.forEach { (id, points) ->
                val name = state.operationTypes.find { it.id == id }?.description ?: "ID $id"
                points.forEach { p ->
                    val formattedValue = String.format(Locale.getDefault(), "%.${state.operationMaxDecimalPlaces}f", p.value)
                    sb.append("${dateFormat.format(Date(p.date))};\"$name\";$formattedValue;${state.operationUnitLabel}\n")
                }
            }
        }
        
        if (state.benchmarkData.isNotEmpty()) {
            val benchTitle = resourceProvider.getString(R.string.csv_export_benchmark_title)
            val totalUsageHeader = resourceProvider.getString(R.string.csv_export_total_usage_header)
            val avgIntHeader = resourceProvider.getString(R.string.csv_export_avg_interval_header)
            val countHeader = resourceProvider.getString(R.string.csv_export_count_header)
            
            sb.append("\n$benchTitle\n")
            sb.append("$equipHeader;$totalUsageHeader;$avgIntHeader;$countHeader\n")
            state.benchmarkData.forEach { b ->
                val total = String.format(Locale.getDefault(), "%.${state.equipmentMaxDecimalPlaces}f", b.totalValue)
                val avg = String.format(Locale.getDefault(), "%.${state.equipmentMaxDecimalPlaces}f", b.avgInterval)
                sb.append("\"${b.equipmentName}\";$total;$avg;${b.count}\n")
            }
        }
        
        if (state.equipmentDistribution.isNotEmpty() && reportTitle.contains("Freq", ignoreCase = true)) {
            val distTitle = resourceProvider.getString(R.string.csv_export_distribution_title)
            val nameHeader = resourceProvider.getString(R.string.csv_export_name_header)
            val occHeader = resourceProvider.getString(R.string.csv_export_occurrences_header)
            val percHeader = resourceProvider.getString(R.string.csv_export_percentage_header)
            
            sb.append("\n$distTitle\n")
            sb.append("$nameHeader;$occHeader;$percHeader\n")
            val dist = if (reportTitle.contains("Equip", ignoreCase = true)) state.equipmentDistribution else state.operationDistribution
            val total = dist.sumOf { it.value.toDouble() }.toFloat()
            dist.forEach { p ->
                val perc = if (total > 0) (p.value / total) * 100 else 0f
                sb.append("\"${p.label}\";${p.value.toInt()};${String.format(Locale.US, "%.1f%%", perc)}\n")
            }
        }
        return sb.toString()
    }
}
