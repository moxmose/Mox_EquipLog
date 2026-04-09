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
import androidx.sqlite.db.SimpleSQLiteQuery
import java.text.SimpleDateFormat

@Serializable
enum class TimeGranularity {
    HOURS, DAYS, WEEKS, MONTHS, YEARS
}

@Serializable
data class ChartPoint(
    val date: Long,
    val kilometers: Float,
    val label: String? = null
)

data class PieChartPoint(
    val label: String,
    val value: Float,
    val color: String? = null,
    val id: Int = -1
)

data class HeatmapPoint(
    val x: Int, // e.g. Day of week
    val y: Int, // e.g. Hour or Month
    val value: Int
)

data class BenchmarkData(
    val equipmentName: String,
    val totalValue: Float,
    val avgInterval: Float,
    val count: Int,
    val periodLabel: String? = null
)

@Serializable
data class ReportFilterState(
    val selectedEquipmentIds: Set<Int> = emptySet(),
    val selectedOperationTypeIds: Set<Int> = emptySet(),
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
    
    val operationTypes: List<OperationType> = emptyList(),
    val selectedOperationTypeIds: Set<Int> = emptySet(),
    val operationChartData: Map<Int, List<ChartPoint>> = emptyMap(),
    val operationDistribution: List<PieChartPoint> = emptyList(),
    val operationDistributionByPeriod: Map<String, List<PieChartPoint>> = emptyMap(),
    val operationCategoryColor: String = UiConstants.DEFAULT_FALLBACK_COLOR,
    val operationUnitLabel: String = "",
    val opHasMixedUnits: Boolean = false,

    // New Analysis Data
    val intervalData: Map<Int, List<ChartPoint>> = emptyMap(),
    val heatmapData: List<HeatmapPoint> = emptyList(),
    val benchmarkData: List<BenchmarkData> = emptyList(),
    val benchmarkByPeriod: Map<String, List<BenchmarkData>> = emptyMap(),

    // Extra Reports (Volume & Combined)
    val equipmentVolumeData: Map<Int, List<ChartPoint>> = emptyMap(),
    val operationVolumeData: Map<Int, List<ChartPoint>> = emptyMap(),
    val combinedLogsData: Map<String, List<ChartPoint>> = emptyMap(),

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
    val startDate: Long?,
    val endDate: Long?,
    val timeGranularity: TimeGranularity?,
    val refreshKey: Int,
    val showDismissed: Boolean,
    val colorMode: String,
    val customColors: List<String>
)

private data class FilterCore(val equips: List<Equipment>, val ops: List<OperationType>, val logs: List<MaintenanceLogDetails>, val units: List<MeasurementUnit>)
private data class FilterStyles(val eColor: String?, val oColor: String?, val selections: SelectionState)
private data class FilterPersistence(val saved: List<ReportFilter>, val active: ReportFilter?, val current: ReportFilterState)

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
    private val _timeGranularity = MutableStateFlow<TimeGranularity?>(null)
    private val _refreshTrigger = MutableStateFlow(0)
    private val _showDismissed = MutableStateFlow(false)
    private val _activeFilter = MutableStateFlow<ReportFilter?>(null)

    private var initializedEquipments = false
    private var initializedOperations = false

    private val currentState: Flow<ReportFilterState> = combine(
        combine(_selectedEquipmentIds, _selectedOperationTypeIds) { ids, ops -> ids to ops },
        combine(_startDate, _endDate) { s, e -> s to e },
        _timeGranularity,
        _showDismissed
    ) { base, dates, gran, dismissed ->
        ReportFilterState(base.first, base.second, dates.first, dates.second, gran, dismissed)
    }

    private val selectionState: Flow<SelectionState> = combine(
        currentState,
        _refreshTrigger,
        appSettingsManager.reportsColorMode,
        appSettingsManager.reportsCustomColors,
        imageRepository.allColorsForReports
    ) { state, refresh, mode, custom, dbColors ->
        // Usiamo i colori del database (caricati da default_data.xml) come base
        val dbColorList = dbColors.filter { !it.reportHidden }.map { it.hexValue }.takeIf { it.isNotEmpty() } 
            ?: listOf("#4285F4", "#34A853", "#FBBC05", "#EA4335", "#9C27B0", "#00BCD4")

        val palette = if (mode == UiConstants.REPORTS_COLOR_MODE_CUSTOM) {
            custom ?: dbColorList
        } else {
            // In modalità M3 o default, usiamo comunque la palette dal database per coerenza
            // Se in futuro vorrai colori dinamici da Material3, potrai cambiare qui.
            dbColorList
        }
        SelectionState(state.selectedEquipmentIds, state.selectedOperationTypeIds, state.startDate, state.endDate, state.timeGranularity, refresh, state.showDismissed, mode, palette)
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
            maintenanceLogDao.getLogsWithDetails(SimpleSQLiteQuery("SELECT l.*, e.description as equipmentDescription, ot.description as operationTypeDescription, e.photoUri as equipmentPhotoUri, e.iconIdentifier as equipmentIconIdentifier, ot.photoUri as operationTypePhotoUri, ot.iconIdentifier as operationTypeIconIdentifier, e.dismissed as equipmentDismissed, ot.dismissed as operationTypeDismissed FROM maintenance_logs as l JOIN equipments as e ON l.equipmentId = e.id JOIN operation_types as ot ON l.operationTypeId = ot.id ORDER BY l.date ASC")),
            measurementUnitDao.getAllUnits()
        ) { e, o, l, u -> FilterCore(e, o, l, u) },
        combine(
            imageRepository.getCategoryColor(Category.EQUIPMENT),
            imageRepository.getCategoryColor(Category.OPERATION),
            selectionState
        ) { ec, oc, s -> FilterStyles(ec, oc, s) },
        combine(
            reportFilterDao.getSavedFilters("ALL_REPORTS"),
            _activeFilter,
            currentState
        ) { saved, active, current -> FilterPersistence(saved, active, current) }
    ) { core, style, persist ->
        val filteredLogs = core.logs.filter { logDetail ->
            val date = logDetail.log.date
            val matchesDate = (style.selections.startDate == null || date >= style.selections.startDate) && 
                              (style.selections.endDate == null || date <= style.selections.endDate)
            val matchesVisibility = style.selections.showDismissed || !logDetail.log.dismissed
            matchesDate && matchesVisibility
        }

        // Calcolo della granularità effettiva globale per questa vista
        val allPoints = filteredLogs.map { ChartPoint(it.log.date, it.log.kilometers?.toFloat() ?: 0f) }
        val effectiveGranularity = if (style.selections.timeGranularity != null) {
            findBestGranularity(allPoints, style.selections.timeGranularity, false, false)
        } else {
            findAutoGranularity(allPoints, false, false)
        }

        val sortedSelectedEquipIds = core.equips.map { it.id }.filter { it in style.selections.selectedEquipmentIds }
        val sortedSelectedOpIds = core.ops.map { it.id }.filter { it in style.selections.selectedOperationTypeIds }
        
        val currentPalette = style.selections.customColors

        // Standard Trends
        val equipChartData = sortedSelectedEquipIds.associateWith { id ->
            val points = filteredLogs.filter { it.log.equipmentId == id && it.log.kilometers != null }.map { ChartPoint(it.log.date, it.log.kilometers!!.toFloat()) }
            aggregateData(points, effectiveGranularity)
        }

        val opChartData = sortedSelectedOpIds.associateWith { id ->
            val points = filteredLogs.filter { it.log.operationTypeId == id && it.log.kilometers != null }.map { ChartPoint(it.log.date, it.log.kilometers!!.toFloat(), it.equipmentDescription) }
            aggregateData(points, effectiveGranularity)
        }

        // Global Distributions (Pie Charts) with STABLE COLORS
        val equipDist = filteredLogs.filter { it.log.equipmentId in style.selections.selectedEquipmentIds }.groupBy { it.log.equipmentId }.map { (id, logs) ->
            val label = core.equips.find { it.id == id }?.description?.takeIf { it.isNotBlank() } ?: "ID: $id"
            val index = core.equips.indexOfFirst { it.id == id }
            val color = if (index != -1) currentPalette[index % currentPalette.size] else null
            PieChartPoint(label, logs.size.toFloat(), color, id)
        }.sortedByDescending { it.value }

        val opDist = filteredLogs.filter { it.log.operationTypeId in style.selections.selectedOperationTypeIds }.groupBy { it.log.operationTypeId }.map { (id, logs) ->
            val label = core.ops.find { it.id == id }?.description?.takeIf { it.isNotBlank() } ?: "ID: $id"
            val index = core.ops.indexOfFirst { it.id == id }
            val color = if (index != -1) currentPalette[index % currentPalette.size] else null
            PieChartPoint(label, logs.size.toFloat(), color, id)
        }.sortedByDescending { it.value }

        // Period-based Distributions (Multiple Pie Charts)
        val equipDistByPeriod = if (effectiveGranularity != null) {
            val dateFormat = getPeriodFormat(effectiveGranularity)
            val periods = filteredLogs.groupBy { dateFormat.format(Date(it.log.date)) }
            if (effectiveGranularity == TimeGranularity.HOURS && periods.size > 60) emptyMap()
            else {
                periods.mapValues { (_, logsInPeriod) ->
                    logsInPeriod.filter { it.log.equipmentId in style.selections.selectedEquipmentIds }.groupBy { it.log.equipmentId }.map { (id, logs) ->
                        val label = core.equips.find { it.id == id }?.description?.takeIf { it.isNotBlank() } ?: "ID: $id"
                        val index = core.equips.indexOfFirst { it.id == id }
                        val color = if (index != -1) currentPalette[index % currentPalette.size] else null
                        PieChartPoint(label, logs.size.toFloat(), color, id)
                    }.sortedByDescending { it.value }
                }
            }
        } else emptyMap()

        val opDistByPeriod = if (effectiveGranularity != null) {
            val dateFormat = getPeriodFormat(effectiveGranularity)
            val periods = filteredLogs.groupBy { dateFormat.format(Date(it.log.date)) }
            if (effectiveGranularity == TimeGranularity.HOURS && periods.size > 60) emptyMap()
            else {
                periods.mapValues { (_, logsInPeriod) ->
                    logsInPeriod.filter { it.log.operationTypeId in style.selections.selectedOperationTypeIds }.groupBy { it.log.operationTypeId }.map { (id, logs) ->
                        val label = core.ops.find { it.id == id }?.description?.takeIf { it.isNotBlank() } ?: "ID: $id"
                        val index = core.ops.indexOfFirst { it.id == id }
                        val color = if (index != -1) currentPalette[index % currentPalette.size] else null
                        PieChartPoint(label, logs.size.toFloat(), color, id)
                    }.sortedByDescending { it.value }
                }
            }
        } else emptyMap()

        // 1. Interval Analysis (KPI)
        val intervalData = sortedSelectedEquipIds.associateWith { id ->
            val rawPoints = filteredLogs.filter { it.log.equipmentId == id && it.log.kilometers != null }
                .map { ChartPoint(it.log.date, it.log.kilometers!!.toFloat()) }
                .sortedBy { it.date }
            
            val deltas = mutableListOf<ChartPoint>()
            for (i in 1 until rawPoints.size) {
                val current = rawPoints[i].kilometers
                val previous = rawPoints[i-1].kilometers
                val delta = (current - previous).coerceAtLeast(0f)
                deltas.add(ChartPoint(rawPoints[i].date, delta))
            }
            aggregateData(deltas, effectiveGranularity, isDelta = true)
        }

        // 2. Heatmap Data
        val cal = Calendar.getInstance()
        val heatmapData = filteredLogs.groupBy { log ->
            cal.timeInMillis = log.log.date
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            val month = cal.get(Calendar.MONTH)
            dow to month
        }.map { (key, logs) -> HeatmapPoint(key.first, key.second, logs.size) }

        // 3. Benchmarking (Global Usage Analysis)
        val benchmarkData = sortedSelectedEquipIds.map { id ->
            val equipLogs = filteredLogs.filter { it.log.equipmentId == id && it.log.kilometers != null }.sortedBy { it.log.date }
            val totalUsage = if (equipLogs.size > 1) (equipLogs.last().log.kilometers!! - equipLogs.first().log.kilometers!!).toFloat() 
                             else equipLogs.firstOrNull()?.log?.kilometers?.toFloat() ?: 0f
            
            val intervals = mutableListOf<Float>()
            for (i in 1 until equipLogs.size) { intervals.add((equipLogs[i].log.kilometers!! - equipLogs[i-1].log.kilometers!!).toFloat()) }
            val avg = if (intervals.isNotEmpty()) intervals.average().toFloat() else 0f
            
            BenchmarkData(equipmentName = core.equips.find { it.id == id }?.description ?: "ID: $id", totalValue = totalUsage, avgInterval = avg, count = equipLogs.size)
        }

        // 3b. Benchmarking by Period (Fixed for Granularity)
        val benchmarkByPeriod = if (effectiveGranularity != null && effectiveGranularity != TimeGranularity.HOURS) {
            val dateFormat = getPeriodFormat(effectiveGranularity)
            val logsByPeriod = filteredLogs.groupBy { dateFormat.format(Date(it.log.date)) }
            
            logsByPeriod.mapValues { (period, _) ->
                sortedSelectedEquipIds.map { id ->
                    // Use correctly aggregated usage from intervalData for this period
                    val usageInPeriod = intervalData[id]?.find { dateFormat.format(Date(it.date)) == period }?.kilometers ?: 0f
                    val countInPeriod = filteredLogs.count { it.log.equipmentId == id && dateFormat.format(Date(it.log.date)) == period }
                    
                    // KPI: Average km per log in this period
                    val avgInPeriod = if (countInPeriod > 0) usageInPeriod / countInPeriod else 0f
                    
                    BenchmarkData(
                        equipmentName = core.equips.find { it.id == id }?.description ?: "ID: $id", 
                        totalValue = usageInPeriod, 
                        avgInterval = avgInPeriod, 
                        count = countInPeriod, 
                        periodLabel = period
                    )
                }
            }
        } else emptyMap()

        // 4. NEW: Activity Volume (Count) over time
        val equipmentVolumeData = sortedSelectedEquipIds.associateWith { id ->
            val points = filteredLogs.filter { it.log.equipmentId == id }.map { ChartPoint(it.log.date, 1f) }
            aggregateData(points, effectiveGranularity, isCount = true)
        }
        val operationVolumeData = sortedSelectedOpIds.associateWith { id ->
            val points = filteredLogs.filter { it.log.operationTypeId == id }.map { ChartPoint(it.log.date, 1f) }
            aggregateData(points, effectiveGranularity, isCount = true)
        }

        // 5. NEW: Combined Logs Trend (Value for specific Equipment + Operation pairs)
        val combinedLogsData = mutableMapOf<String, List<ChartPoint>>()
        sortedSelectedEquipIds.forEach { eId ->
            sortedSelectedOpIds.forEach { oId ->
                val logs = filteredLogs.filter { it.log.equipmentId == eId && it.log.operationTypeId == oId && it.log.kilometers != null }
                if (logs.isNotEmpty()) {
                    val eDesc = core.equips.find { it.id == eId }?.description ?: "E$eId"
                    val oDesc = core.ops.find { it.id == oId }?.description ?: "O$oId"
                    val key = "$eDesc - $oDesc"
                    val points = logs.map { ChartPoint(it.log.date, it.log.kilometers!!.toFloat()) }
                    combinedLogsData[key] = aggregateData(points, effectiveGranularity)
                }
            }
        }
        
        val selectedUnits = sortedSelectedEquipIds.mapNotNull { id -> core.units.find { it.id == core.equips.find { e -> e.id == id }?.unitId }?.label }.distinct()
        val opSelectedUnits = sortedSelectedOpIds.flatMap { opId -> filteredLogs.filter { it.log.operationTypeId == opId }.mapNotNull { logDetail -> core.equips.find { it.id == logDetail.log.equipmentId }?.unitId?.let { unitId -> core.units.find { it.id == unitId }?.label } } }.distinct()

        ReportsUiState(
            equipments = core.equips, selectedEquipmentIds = style.selections.selectedEquipmentIds, equipmentChartData = equipChartData,
            equipmentDistribution = equipDist, equipmentDistributionByPeriod = equipDistByPeriod,
            equipmentCategoryColor = style.eColor ?: UiConstants.DEFAULT_FALLBACK_COLOR,
            equipmentUnitLabel = selectedUnits.joinToString(", "), hasMixedUnits = selectedUnits.size > 1,
            operationTypes = core.ops, selectedOperationTypeIds = style.selections.selectedOperationTypeIds, operationChartData = opChartData,
            operationDistribution = opDist, operationDistributionByPeriod = opDistByPeriod,
            operationCategoryColor = style.oColor ?: UiConstants.DEFAULT_FALLBACK_COLOR,
            operationUnitLabel = opSelectedUnits.joinToString(", "), opHasMixedUnits = opSelectedUnits.size > 1,
            intervalData = intervalData, heatmapData = heatmapData, benchmarkData = benchmarkData, benchmarkByPeriod = benchmarkByPeriod,
            equipmentVolumeData = equipmentVolumeData, operationVolumeData = operationVolumeData, combinedLogsData = combinedLogsData,
            startDate = style.selections.startDate, endDate = style.selections.endDate, timeGranularity = style.selections.timeGranularity,
            effectiveGranularity = effectiveGranularity,
            showDismissed = style.selections.showDismissed, colorMode = style.selections.colorMode, customColors = currentPalette,
            savedFilters = persist.saved, activeFilterName = persist.active?.name, isFilterDirty = persist.active != null && persist.active.filterJson != Json.encodeToString(persist.current)
        )
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = ReportsUiState())

    private fun getPeriodFormat(granularity: TimeGranularity) = when (granularity) {
        TimeGranularity.DAYS -> SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        TimeGranularity.WEEKS -> SimpleDateFormat("'W'ww/yy", Locale.getDefault())
        TimeGranularity.MONTHS -> SimpleDateFormat("MM/yy", Locale.getDefault())
        TimeGranularity.YEARS -> SimpleDateFormat("yyyy", Locale.getDefault())
        TimeGranularity.HOURS -> SimpleDateFormat("dd/MM HH:00", Locale.getDefault())
    }

    private fun aggregateData(points: List<ChartPoint>, granularity: TimeGranularity?, isDelta: Boolean = false, isCount: Boolean = false): List<ChartPoint> {
        if (points.isEmpty()) return emptyList()
        
        // Esegui l'aggregazione con la granularità fornita (già calcolata o auto)
        var result = performAggregation(points, granularity, isDelta, isCount)

        // Data Capping & Smoothing sempre attivo per garantire leggibilità
        if (result.size > UiConstants.MAX_CHART_POINTS) {
            result = smoothData(result, UiConstants.MAX_CHART_POINTS)
        }

        return result
    }

    private fun findBestGranularity(points: List<ChartPoint>, requested: TimeGranularity, isDelta: Boolean, isCount: Boolean): TimeGranularity {
        if (points.size < 2) return requested
        
        var current = requested
        while (current != TimeGranularity.HOURS) {
            // Prova a vedere quanti punti produce questa granularità
            val testAggregation = performAggregation(points, current, isDelta, isCount)
            if (testAggregation.size >= 2) {
                return current
            }
            // Se produce meno di 2 punti, scendi di livello
            current = when (current) {
                TimeGranularity.YEARS -> TimeGranularity.MONTHS
                TimeGranularity.MONTHS -> TimeGranularity.WEEKS
                TimeGranularity.WEEKS -> TimeGranularity.DAYS
                else -> TimeGranularity.HOURS
            }
        }
        return current
    }

    private fun findAutoGranularity(points: List<ChartPoint>, isDelta: Boolean, isCount: Boolean): TimeGranularity? {
        if (points.size <= 10) return null // Pochi dati: mostrali grezzi
        
        val granularities = listOf(TimeGranularity.YEARS, TimeGranularity.MONTHS, TimeGranularity.WEEKS, TimeGranularity.DAYS, TimeGranularity.HOURS)
        
        for (gran in granularities) {
            val count = performAggregation(points, gran, isDelta, isCount).size
            // Cerchiamo una granularità che ci dia tra i 5 e i 25 punti per una visione "compatta"
            if (count in 5..25) return gran
        }
        return TimeGranularity.HOURS
    }

    private fun performAggregation(points: List<ChartPoint>, granularity: TimeGranularity?, isDelta: Boolean, isCount: Boolean): List<ChartPoint> {
        if (granularity == null) return points.sortedBy { it.date }
        
        val calendar = Calendar.getInstance()
        val grouped = points.groupBy { point ->
            calendar.timeInMillis = point.date
            when (granularity) {
                TimeGranularity.HOURS -> { calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0) }
                TimeGranularity.DAYS -> { calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0) }
                TimeGranularity.WEEKS -> { 
                    calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                    calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0) 
                }
                TimeGranularity.MONTHS -> { calendar.set(Calendar.DAY_OF_MONTH, 1); calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0) }
                TimeGranularity.YEARS -> { calendar.set(Calendar.DAY_OF_YEAR, 1); calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0) }
            }
            calendar.timeInMillis
        }

        return grouped.map { (timestamp, groupedPoints) -> 
            val value = when {
                isCount -> groupedPoints.size.toFloat()
                isDelta -> groupedPoints.sumOf { it.kilometers.toDouble() }.toFloat()
                else -> groupedPoints.maxOf { it.kilometers }
            }
            ChartPoint(timestamp, value) 
        }.sortedBy { it.date }
    }

    private fun smoothData(points: List<ChartPoint>, maxPoints: Int): List<ChartPoint> {
        if (points.size <= maxPoints) return points
        val chunkSize = (points.size.toDouble() / maxPoints).toInt().coerceAtLeast(1)
        return points.chunked(chunkSize).map { chunk ->
            val avgDate = chunk.map { it.date }.average().toLong()
            val avgValue = chunk.map { it.kilometers }.average().toFloat()
            ChartPoint(avgDate, avgValue)
        }
    }

    fun toggleEquipmentSelection(id: Int) { _selectedEquipmentIds.value = if (_selectedEquipmentIds.value.contains(id)) _selectedEquipmentIds.value - id else _selectedEquipmentIds.value + id }
    fun toggleOperationTypeSelection(id: Int) { _selectedOperationTypeIds.value = if (_selectedOperationTypeIds.value.contains(id)) _selectedOperationTypeIds.value - id else _selectedOperationTypeIds.value + id }
    fun selectAllEquipment() { _selectedEquipmentIds.value = uiState.value.equipments.map { it.id }.toSet() }
    fun invertEquipmentSelection() { _selectedEquipmentIds.value = uiState.value.equipments.map { it.id }.toSet() - _selectedEquipmentIds.value }
    fun clearEquipmentSelection() { _selectedEquipmentIds.value = emptySet() }
    fun selectAllOperationTypes() { _selectedOperationTypeIds.value = uiState.value.operationTypes.map { it.id }.toSet() }
    fun invertOperationTypeSelection() { _selectedOperationTypeIds.value = uiState.value.operationTypes.map { it.id }.toSet() - _selectedOperationTypeIds.value }
    fun clearOperationTypeSelection() { _selectedOperationTypeIds.value = emptySet() }
    fun setDateRange(start: Long?, end: Long?) { _startDate.value = start; _endDate.value = end }
    fun setTimeGranularity(granularity: TimeGranularity?) {
        _timeGranularity.value = if (_timeGranularity.value == granularity) null else granularity
    }
    fun resetFilters() { _startDate.value = null; _endDate.value = null; _timeGranularity.value = null; _showDismissed.value = false; selectAllEquipment(); selectAllOperationTypes(); _activeFilter.value = null }
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

    fun getCsvExportData(reportTitle: String): String {
        val state = uiState.value
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        // Intestazione
        sb.append("Report;${reportTitle}\n")
        sb.append("Data Esportazione;${dateFormat.format(Date())}\n")
        sb.append("Periodo;${state.startDate?.let { dateFormat.format(Date(it)) } ?: "Inizio"} - ${state.endDate?.let { dateFormat.format(Date(it)) } ?: "Fine"}\n\n")

        // 1. Dati Trend (Equipments o Operations)
        if (state.equipmentChartData.any { it.value.isNotEmpty() }) {
            sb.append("Data;Attrezzatura;Valore;Unita\n")
            state.equipmentChartData.forEach { (id, points) ->
                val name = state.equipments.find { it.id == id }?.description ?: "ID $id"
                points.forEach { p ->
                    sb.append("${dateFormat.format(Date(p.date))};\"$name\";${p.kilometers};${state.equipmentUnitLabel}\n")
                }
            }
        } else if (state.operationChartData.any { it.value.isNotEmpty() }) {
            sb.append("Data;Operazione;Valore;Unita\n")
            state.operationChartData.forEach { (id, points) ->
                val name = state.operationTypes.find { it.id == id }?.description ?: "ID $id"
                points.forEach { p ->
                    sb.append("${dateFormat.format(Date(p.date))};\"$name\";${p.kilometers};${state.operationUnitLabel}\n")
                }
            }
        }

        // 2. Dati Benchmark
        if (state.benchmarkData.isNotEmpty()) {
            sb.append("\nAnalisi Benchmark\n")
            sb.append("Attrezzatura;Utilizzo Totale;Media Intervallo;Conteggio Log\n")
            state.benchmarkData.forEach { b ->
                sb.append("\"${b.equipmentName}\";${b.totalValue};${b.avgInterval};${b.count}\n")
            }
        }

        // 3. Distribuzioni (Pie Charts)
        if (state.equipmentDistribution.isNotEmpty() && reportTitle.contains("Freq", ignoreCase = true)) {
            sb.append("\nDistribuzione\n")
            sb.append("Nome;Occorrenze;Percentuale\n")
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
