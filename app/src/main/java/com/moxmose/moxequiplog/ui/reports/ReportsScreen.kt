package com.moxmose.moxequiplog.ui.reports

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.Equipment
import com.moxmose.moxequiplog.data.local.OperationType
import com.moxmose.moxequiplog.data.local.ReportFilter
import com.moxmose.moxequiplog.ui.components.ImageIcon
import com.moxmose.moxequiplog.utils.UiConstants
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class ReportDestination {
    MENU,
    EQUIPMENTS,
    OPERATIONS,
    EQUIPMENTS_FREQ,
    OPERATIONS_FREQ
}

@Composable
fun ReportsScreen(
    modifier: Modifier = Modifier,
    viewModel: ReportsViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentDestination by remember { mutableStateOf(ReportDestination.MENU) }

    BackHandler(enabled = currentDestination != ReportDestination.MENU) {
        currentDestination = ReportDestination.MENU
    }

    AnimatedContent(
        targetState = currentDestination,
        label = "ReportsNavigation"
    ) { destination ->
        when (destination) {
            ReportDestination.MENU -> ReportsMenu(
                onNavigate = { currentDestination = it },
                onBack = onBack
            )
            ReportDestination.EQUIPMENTS -> EquipmentsReportScreen(
                uiState = uiState,
                viewModel = viewModel,
                onBack = { currentDestination = ReportDestination.MENU }
            )
            ReportDestination.OPERATIONS -> OperationsReportScreen(
                uiState = uiState,
                viewModel = viewModel,
                onBack = { currentDestination = ReportDestination.MENU }
            )
            ReportDestination.EQUIPMENTS_FREQ -> EquipmentsFreqReportScreen(
                uiState = uiState,
                viewModel = viewModel,
                onBack = { currentDestination = ReportDestination.MENU }
            )
            ReportDestination.OPERATIONS_FREQ -> OperationsFreqReportScreen(
                uiState = uiState,
                viewModel = viewModel,
                onBack = { currentDestination = ReportDestination.MENU }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsMenu(onNavigate: (ReportDestination) -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reports_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ReportMenuCard(
                title = stringResource(R.string.report_equipments_title),
                description = stringResource(R.string.report_equipments_desc),
                icon = Icons.Default.PrecisionManufacturing,
                onClick = { onNavigate(ReportDestination.EQUIPMENTS) }
            )
            ReportMenuCard(
                title = stringResource(R.string.report_operations_title),
                description = stringResource(R.string.report_operations_desc),
                icon = Icons.Default.Settings,
                onClick = { onNavigate(ReportDestination.OPERATIONS) }
            )
            ReportMenuCard(
                title = stringResource(R.string.report_equipments_freq_title),
                description = stringResource(R.string.report_equipments_freq_desc),
                icon = Icons.Default.PieChart,
                onClick = { onNavigate(ReportDestination.EQUIPMENTS_FREQ) }
            )
            ReportMenuCard(
                title = stringResource(R.string.report_operations_freq_title),
                description = stringResource(R.string.report_operations_freq_desc),
                icon = Icons.Default.BarChart,
                onClick = { onNavigate(ReportDestination.OPERATIONS_FREQ) }
            )
        }
    }
}

@Composable
fun ReportMenuCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentsReportScreen(
    uiState: ReportsUiState,
    viewModel: ReportsViewModel,
    onBack: () -> Unit
) {
    val categoryColor = Color(uiState.equipmentCategoryColor.toColorInt())
    val chartColors = rememberChartColors(uiState.colorMode, uiState.customColors)
    val sortedSelectedIds = remember(uiState.equipments, uiState.selectedEquipmentIds) {
        uiState.equipments.map { it.id }.filter { it in uiState.selectedEquipmentIds }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.report_equipments_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        EquipmentMultiSelector(
                            equipments = uiState.equipments,
                            selectedIds = uiState.selectedEquipmentIds,
                            onToggleSelection = { 
                                viewModel.toggleEquipmentSelection(it)
                                viewModel.refresh()
                            },
                            categoryColor = categoryColor,
                            chartColors = chartColors,
                            sortedSelectedIds = sortedSelectedIds
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleShowDismissed() },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (uiState.showDismissed) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                        )
                    ) {
                        Icon(
                            imageVector = if (uiState.showDismissed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (uiState.showDismissed) stringResource(R.string.hide_dismissed) else stringResource(R.string.show_dismissed),
                            tint = if (uiState.showDismissed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.selectAllEquipment()
                            viewModel.refresh()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SelectAll,
                            contentDescription = stringResource(R.string.selection_all),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.invertEquipmentSelection()
                            viewModel.refresh()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = stringResource(R.string.selection_invert),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.clearEquipmentSelection()
                            viewModel.refresh()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterAltOff,
                            contentDescription = stringResource(R.string.selection_clear),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                StandardFilterSection(
                    startDate = uiState.startDate,
                    endDate = uiState.endDate,
                    granularity = uiState.timeGranularity,
                    onDateRangeSelected = viewModel::setDateRange,
                    onGranularitySelected = viewModel::setTimeGranularity,
                    onReset = viewModel::resetFilters,
                    onRefresh = viewModel::refresh
                )
            }

            item {
                FilterManagementRow(
                    savedFilters = uiState.savedFilters,
                    activeFilterName = uiState.activeFilterName,
                    isDirty = uiState.isFilterDirty,
                    onSaveNew = viewModel::saveAsNewFilter,
                    onOverwrite = viewModel::overwriteActiveFilter,
                    onApply = viewModel::applySavedFilter,
                    onDelete = viewModel::deleteSavedFilter
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val unitsPart = if (uiState.equipmentUnitLabel.isNotBlank()) {
                            uiState.equipmentUnitLabel.split(", ").joinToString("") { "[$it]" }
                        } else ""
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$unitsPart ${stringResource(R.string.report_value_over_time)}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (uiState.hasMixedUnits) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = MaterialTheme.shapes.extraSmall
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp).padding(2.dp),
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                        
                        if (uiState.hasMixedUnits) {
                            Text(
                                text = stringResource(R.string.mixed_units_warning),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        MultiLineChart(
                            chartDataMap = uiState.equipmentChartData,
                            granularity = uiState.timeGranularity,
                            finalColors = chartColors
                        )
                        
                        if (uiState.equipmentChartData.any { it.value.isNotEmpty() }) {
                            Spacer(modifier = Modifier.height(16.dp))
                            ChartLegend(
                                items = sortedSelectedIds.mapNotNull { id -> 
                                    uiState.equipments.find { it.id == id }?.description?.let { id to it }
                                },
                                colors = chartColors
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationsReportScreen(
    uiState: ReportsUiState,
    viewModel: ReportsViewModel,
    onBack: () -> Unit
) {
    val categoryColor = Color(uiState.operationCategoryColor.toColorInt())
    val chartColors = rememberChartColors(uiState.colorMode, uiState.customColors)
    val sortedSelectedIds = remember(uiState.operationTypes, uiState.selectedOperationTypeIds) {
        uiState.operationTypes.map { it.id }.filter { it in uiState.selectedOperationTypeIds }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.report_operations_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OperationMultiSelector(
                            operationTypes = uiState.operationTypes,
                            selectedIds = uiState.selectedOperationTypeIds,
                            onToggleSelection = { 
                                viewModel.toggleOperationTypeSelection(it)
                                viewModel.refresh()
                            },
                            categoryColor = categoryColor,
                            chartColors = chartColors,
                            sortedSelectedIds = sortedSelectedIds
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleShowDismissed() },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (uiState.showDismissed) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                        )
                    ) {
                        Icon(
                            imageVector = if (uiState.showDismissed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (uiState.showDismissed) stringResource(R.string.hide_dismissed) else stringResource(R.string.show_dismissed),
                            tint = if (uiState.showDismissed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.selectAllOperationTypes()
                            viewModel.refresh()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SelectAll,
                            contentDescription = stringResource(R.string.selection_all),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.invertOperationTypeSelection()
                            viewModel.refresh()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = stringResource(R.string.selection_invert),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.clearOperationTypeSelection()
                            viewModel.refresh()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterAltOff,
                            contentDescription = stringResource(R.string.selection_clear),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                StandardFilterSection(
                    startDate = uiState.startDate,
                    endDate = uiState.endDate,
                    granularity = uiState.timeGranularity,
                    onDateRangeSelected = viewModel::setDateRange,
                    onGranularitySelected = viewModel::setTimeGranularity,
                    onReset = viewModel::resetFilters,
                    onRefresh = viewModel::refresh
                )
            }

            item {
                FilterManagementRow(
                    savedFilters = uiState.savedFilters,
                    activeFilterName = uiState.activeFilterName,
                    isDirty = uiState.isFilterDirty,
                    onSaveNew = viewModel::saveAsNewFilter,
                    onOverwrite = viewModel::overwriteActiveFilter,
                    onApply = viewModel::applySavedFilter,
                    onDelete = viewModel::deleteSavedFilter
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val unitsPart = if (uiState.operationUnitLabel.isNotBlank()) {
                            uiState.operationUnitLabel.split(", ").joinToString("") { "[$it]" }
                        } else ""
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$unitsPart ${stringResource(R.string.report_value_over_time)}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (uiState.opHasMixedUnits) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = MaterialTheme.shapes.extraSmall
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp).padding(2.dp),
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        if (uiState.opHasMixedUnits) {
                            Text(
                                text = stringResource(R.string.mixed_units_warning),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        MultiLineChart(
                            chartDataMap = uiState.operationChartData,
                            granularity = uiState.timeGranularity,
                            finalColors = chartColors
                        )
                        
                        if (uiState.operationChartData.any { it.value.isNotEmpty() }) {
                            Spacer(modifier = Modifier.height(16.dp))
                            ChartLegend(
                                items = sortedSelectedIds.mapNotNull { id -> 
                                    uiState.operationTypes.find { it.id == id }?.description?.let { id to it }
                                },
                                colors = chartColors
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentsFreqReportScreen(
    uiState: ReportsUiState,
    viewModel: ReportsViewModel,
    onBack: () -> Unit
) {
    val categoryColor = Color(uiState.equipmentCategoryColor.toColorInt())
    val chartColors = rememberChartColors(uiState.colorMode, uiState.customColors)
    val sortedSelectedIds = remember(uiState.equipments, uiState.selectedEquipmentIds) {
        uiState.equipments.map { it.id }.filter { it in uiState.selectedEquipmentIds }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.report_equipments_freq_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        EquipmentMultiSelector(
                            equipments = uiState.equipments,
                            selectedIds = uiState.selectedEquipmentIds,
                            onToggleSelection = { 
                                viewModel.toggleEquipmentSelection(it)
                                viewModel.refresh()
                            },
                            categoryColor = categoryColor,
                            chartColors = chartColors,
                            sortedSelectedIds = sortedSelectedIds
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleShowDismissed() },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (uiState.showDismissed) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                        )
                    ) {
                        Icon(
                            imageVector = if (uiState.showDismissed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (uiState.showDismissed) stringResource(R.string.hide_dismissed) else stringResource(R.string.show_dismissed),
                            tint = if (uiState.showDismissed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.selectAllEquipment()
                            viewModel.refresh()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SelectAll,
                            contentDescription = stringResource(R.string.selection_all),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.invertEquipmentSelection()
                            viewModel.refresh()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = stringResource(R.string.selection_invert),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.clearEquipmentSelection()
                            viewModel.refresh()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterAltOff,
                            contentDescription = stringResource(R.string.selection_clear),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                StandardFilterSection(
                    startDate = uiState.startDate,
                    endDate = uiState.endDate,
                    granularity = uiState.timeGranularity,
                    onDateRangeSelected = viewModel::setDateRange,
                    onGranularitySelected = viewModel::setTimeGranularity,
                    onReset = viewModel::resetFilters,
                    onRefresh = viewModel::refresh
                )
            }

            item {
                FilterManagementRow(
                    savedFilters = uiState.savedFilters,
                    activeFilterName = uiState.activeFilterName,
                    isDirty = uiState.isFilterDirty,
                    onSaveNew = viewModel::saveAsNewFilter,
                    onOverwrite = viewModel::overwriteActiveFilter,
                    onApply = viewModel::applySavedFilter,
                    onDelete = viewModel::deleteSavedFilter
                )
            }

            item {
                PieChartCard(
                    title = stringResource(R.string.report_section_equipments_freq),
                    distribution = uiState.equipmentDistribution,
                    baseColor = categoryColor
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationsFreqReportScreen(
    uiState: ReportsUiState,
    viewModel: ReportsViewModel,
    onBack: () -> Unit
) {
    val categoryColor = Color(uiState.operationCategoryColor.toColorInt())
    val chartColors = rememberChartColors(uiState.colorMode, uiState.customColors)
    val sortedSelectedIds = remember(uiState.operationTypes, uiState.selectedOperationTypeIds) {
        uiState.operationTypes.map { it.id }.filter { it in uiState.selectedOperationTypeIds }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.report_operations_freq_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OperationMultiSelector(
                            operationTypes = uiState.operationTypes,
                            selectedIds = uiState.selectedOperationTypeIds,
                            onToggleSelection = { 
                                viewModel.toggleOperationTypeSelection(it)
                                viewModel.refresh()
                            },
                            categoryColor = categoryColor,
                            chartColors = chartColors,
                            sortedSelectedIds = sortedSelectedIds
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleShowDismissed() },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (uiState.showDismissed) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                        )
                    ) {
                        Icon(
                            imageVector = if (uiState.showDismissed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (uiState.showDismissed) stringResource(R.string.hide_dismissed) else stringResource(R.string.show_dismissed),
                            tint = if (uiState.showDismissed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.selectAllOperationTypes()
                            viewModel.refresh()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SelectAll,
                            contentDescription = stringResource(R.string.selection_all),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.invertOperationTypeSelection()
                            viewModel.refresh()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = stringResource(R.string.selection_invert),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.clearOperationTypeSelection()
                            viewModel.refresh()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterAltOff,
                            contentDescription = stringResource(R.string.selection_clear),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                StandardFilterSection(
                    startDate = uiState.startDate,
                    endDate = uiState.endDate,
                    granularity = uiState.timeGranularity,
                    onDateRangeSelected = viewModel::setDateRange,
                    onGranularitySelected = viewModel::setTimeGranularity,
                    onReset = viewModel::resetFilters,
                    onRefresh = viewModel::refresh
                )
            }

            item {
                FilterManagementRow(
                    savedFilters = uiState.savedFilters,
                    activeFilterName = uiState.activeFilterName,
                    isDirty = uiState.isFilterDirty,
                    onSaveNew = viewModel::saveAsNewFilter,
                    onOverwrite = viewModel::overwriteActiveFilter,
                    onApply = viewModel::applySavedFilter,
                    onDelete = viewModel::deleteSavedFilter
                )
            }

            item {
                PieChartCard(
                    title = stringResource(R.string.report_section_operations_freq),
                    distribution = uiState.operationDistribution,
                    baseColor = categoryColor
                )
            }
        }
    }
}

@Composable
fun FilterManagementRow(
    savedFilters: List<ReportFilter>,
    activeFilterName: String?,
    isDirty: Boolean,
    onSaveNew: (String) -> Unit,
    onOverwrite: () -> Unit,
    onApply: (ReportFilter) -> Unit,
    onDelete: (Int) -> Unit
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var filterName by remember { mutableStateOf("") }
    var showLoadMenu by remember { mutableStateOf(false) }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(stringResource(R.string.save_filter)) },
            text = {
                OutlinedTextField(
                    value = filterName,
                    onValueChange = { filterName = it },
                    label = { Text(stringResource(R.string.filter_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (filterName.isNotBlank()) {
                            onSaveNew(filterName)
                            filterName = ""
                            showSaveDialog = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.button_add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icona Filter (senza sbarra) per identificare la riga preset
        Icon(
            imageVector = Icons.Default.FilterAlt,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.width(8.dp))

        // Etichetta del filtro attivo con stato dirty
        Text(
            text = activeFilterName ?: stringResource(R.string.custom_filter),
            style = MaterialTheme.typography.labelSmall,
            color = if (isDirty) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary,
            fontStyle = if (isDirty) FontStyle.Italic else FontStyle.Normal,
            fontWeight = if (activeFilterName != null) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Pulsante Carica (Cartella)
        Box {
            IconButton(
                onClick = { showLoadMenu = true },
                enabled = savedFilters.isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = stringResource(R.string.load_filter),
                    tint = if (savedFilters.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
            
            DropdownMenu(
                expanded = showLoadMenu,
                onDismissRequest = { showLoadMenu = false }
            ) {
                savedFilters.forEach { filter ->
                    DropdownMenuItem(
                        text = { Text(filter.name ?: "", style = MaterialTheme.typography.bodyMedium) },
                        onClick = {
                            onApply(filter)
                            showLoadMenu = false
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { onDelete(filter.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                }
            }
        }

        // Pulsante Sovrascrivi (Floppy) - visibile solo se dirty
        if (activeFilterName != null && isDirty) {
            IconButton(onClick = onOverwrite) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = stringResource(R.string.overwrite_filter),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Pulsante Salva Nuovo (PostAdd)
        IconButton(onClick = { showSaveDialog = true }) {
            Icon(
                imageVector = Icons.Default.PostAdd,
                contentDescription = stringResource(R.string.save_new_filter),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun PieChartCard(
    title: String,
    distribution: List<PieChartPoint>,
    baseColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (distribution.isEmpty()) {
                NoDataPlaceholder()
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PieChart(
                        modifier = Modifier.size(140.dp),
                        points = distribution,
                        baseColor = baseColor
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        distribution.take(5).forEachIndexed { index, point ->
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .size(12.dp)
                                        .clip(MaterialTheme.shapes.extraSmall)
                                        .graphicsLayer(alpha = (1f - (index * 0.15f)).coerceAtLeast(0.2f))
                                        .background(baseColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.occurrences_format, point.label, point.value.toInt()),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        if (distribution.size > 5) {
                            Text(
                                text = "...",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(start = 20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PieChart(
    modifier: Modifier = Modifier,
    points: List<PieChartPoint>,
    baseColor: Color
) {
    val total = points.sumOf { it.value.toDouble() }.toFloat()
    
    Canvas(modifier = modifier) {
        var startAngle = -90f
        points.forEachIndexed { index, point ->
            val sweepAngle = (point.value / total) * 360f
            val colorAlpha = (1f - (index * 0.15f)).coerceAtMost(1f).coerceAtLeast(0.2f)
            
            drawArc(
                color = baseColor.copy(alpha = colorAlpha),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                size = Size(size.minDimension, size.minDimension),
                topLeft = Offset((size.width - size.minDimension) / 2, (size.height - size.minDimension) / 2)
            )
            
            drawArc(
                color = Color.White.copy(alpha = 0.3f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                style = Stroke(width = 1.dp.toPx()),
                size = Size(size.minDimension, size.minDimension),
                topLeft = Offset((size.width - size.minDimension) / 2, (size.height - size.minDimension) / 2)
            )
            
            startAngle += sweepAngle
        }
    }
}

@Composable
fun NoDataPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.report_no_data),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentMultiSelector(
    equipments: List<Equipment>,
    selectedIds: Set<Int>,
    onToggleSelection: (Int) -> Unit,
    categoryColor: Color,
    chartColors: List<Color>,
    sortedSelectedIds: List<Int>
) {
    var expanded by remember { mutableStateOf(false) }
    val displayText = if (selectedIds.isEmpty()) {
        stringResource(R.string.select_equipment)
    } else {
        val firstSelected = equipments.find { it.id == selectedIds.first() }?.description?.takeIf { it.isNotBlank() }
        if (selectedIds.size > 1) {
            "$firstSelected + ${selectedIds.size - 1}"
        } else {
            firstSelected ?: "ID: ${selectedIds.first()}"
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.navigation_equipments)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = categoryColor,
                focusedLabelColor = categoryColor
            ),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable, true)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            equipments.forEach { equipment ->
                val isSelected = equipment.id in selectedIds
                val colorIndex = if (isSelected) sortedSelectedIds.indexOf(equipment.id) else -1
                
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null
                            )
                            
                            if (isSelected && colorIndex != -1) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(chartColors[colorIndex % chartColors.size])
                                )
                            } else {
                                Spacer(modifier = Modifier.size(12.dp))
                            }
                            
                            ImageIcon(
                                iconIdentifier = equipment.iconIdentifier,
                                photoUri = equipment.photoUri,
                                modifier = Modifier.size(24.dp),
                                tint = if (isSelected) categoryColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = equipment.description,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    onClick = { onToggleSelection(equipment.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationMultiSelector(
    operationTypes: List<OperationType>,
    selectedIds: Set<Int>,
    onToggleSelection: (Int) -> Unit,
    categoryColor: Color,
    chartColors: List<Color>,
    sortedSelectedIds: List<Int>
) {
    var expanded by remember { mutableStateOf(false) }
    val displayText = if (selectedIds.isEmpty()) {
        stringResource(R.string.select_operation_type)
    } else {
        val firstSelected = operationTypes.find { it.id == selectedIds.first() }?.description?.takeIf { it.isNotBlank() }
        if (selectedIds.size > 1) {
            "$firstSelected + ${selectedIds.size - 1}"
        } else {
            firstSelected ?: "ID: ${selectedIds.first()}"
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.navigation_operations)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = categoryColor,
                focusedLabelColor = categoryColor
            ),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable, true)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            operationTypes.forEach { op ->
                val isSelected = op.id in selectedIds
                val colorIndex = if (isSelected) sortedSelectedIds.indexOf(op.id) else -1

                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null
                            )

                            if (isSelected && colorIndex != -1) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(chartColors[colorIndex % chartColors.size])
                                )
                            } else {
                                Spacer(modifier = Modifier.size(12.dp))
                            }

                            ImageIcon(
                                iconIdentifier = op.iconIdentifier,
                                photoUri = op.photoUri,
                                modifier = Modifier.size(24.dp),
                                tint = if (isSelected) categoryColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = op.description,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    onClick = {
                        onToggleSelection(op.id)
                    }
                )
            }
        }
    }
}

@Composable
fun MultiLineChart(
    chartDataMap: Map<Int, List<ChartPoint>>, 
    granularity: TimeGranularity,
    finalColors: List<Color>
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    
    val dataWithPoints = remember(chartDataMap) {
        chartDataMap.filter { it.value.isNotEmpty() }
    }
    
    val lineStyles = remember(dataWithPoints, chartDataMap.keys, finalColors) {
        val allIds = chartDataMap.keys.toList()
        dataWithPoints.keys.map { id ->
            val colorIndex = allIds.indexOf(id)
            LineCartesianLayer.Line(
                fill = LineCartesianLayer.LineFill.single(
                    fill = fill(finalColors[colorIndex % finalColors.size])
                )
            )
        }
    }

    val dateFormat = remember(granularity) {
        when (granularity) {
            TimeGranularity.HOURS -> SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
            TimeGranularity.DAYS, TimeGranularity.WEEKS -> SimpleDateFormat("dd/MM", Locale.getDefault())
            TimeGranularity.MONTHS -> SimpleDateFormat("MM/yy", Locale.getDefault())
            TimeGranularity.YEARS -> SimpleDateFormat("yyyy", Locale.getDefault())
        }
    }

    LaunchedEffect(dataWithPoints, granularity) {
        if (dataWithPoints.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries {
                    dataWithPoints.values.forEach { points ->
                        series(
                            x = points.map { it.date },
                            y = points.map { it.kilometers }
                        )
                    }
                }
            }
        }
    }

    if (dataWithPoints.isNotEmpty()) {
        key(granularity, dataWithPoints.keys, finalColors) {
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(
                        lineProvider = LineCartesianLayer.LineProvider.series(lineStyles)
                    ),
                    startAxis = VerticalAxis.rememberStart(),
                    bottomAxis = HorizontalAxis.rememberBottom(
                        valueFormatter = CartesianValueFormatter { _, value, _ ->
                            dateFormat.format(Date(value.toLong()))
                        }
                    ),
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            )
        }
    } else {
        NoDataPlaceholder()
    }
}

@Composable
fun ChartLegend(
    items: List<Pair<Int, String>>,
    colors: List<Color>
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEachIndexed { index, pair ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(colors[index % colors.size])
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = pair.second,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun rememberChartColors(colorMode: String, customColors: List<String>): List<Color> {
    return remember(colorMode, customColors) {
        if (colorMode == UiConstants.REPORTS_COLOR_MODE_CUSTOM && customColors.isNotEmpty()) {
            customColors.map { Color(it.toColorInt()) }
        } else {
            listOf(
                Color(0xFF4285F4), Color(0xFF34A853), Color(0xFFFBBC05),
                Color(0xFFEA4335), Color(0xFF9C27B0), Color(0xFF00BCD4)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardFilterSection(
    startDate: Long?,
    endDate: Long?,
    granularity: TimeGranularity,
    onDateRangeSelected: (Long?, Long?) -> Unit,
    onGranularitySelected: (TimeGranularity) -> Unit,
    onReset: () -> Unit,
    onRefresh: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    var showDatePickerRange by remember { mutableStateOf(false) }

    if (showDatePickerRange) {
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = startDate,
            initialSelectedEndDateMillis = endDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerRange = false },
            confirmButton = {
                TextButton(onClick = {
                    onDateRangeSelected(
                        dateRangePickerState.selectedStartDateMillis,
                        dateRangePickerState.selectedEndDateMillis
                    )
                    showDatePickerRange = false
                    onRefresh()
                }) {
                    Text(stringResource(R.string.button_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerRange = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = { Text(stringResource(R.string.date), modifier = Modifier.padding(16.dp)) },
                showModeToggle = false,
                modifier = Modifier.fillMaxWidth().height(400.dp)
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showDatePickerRange = true },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (startDate != null && endDate != null) {
                            "${dateFormat.format(Date(startDate))} - ${dateFormat.format(Date(endDate))}"
                        } else {
                            stringResource(R.string.date)
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                IconButton(
                    onClick = {
                        onReset()
                        onRefresh()
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterAltOff,
                        contentDescription = stringResource(R.string.filter_reset),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TimeGranularity.entries.forEach { entry ->
                    val isSelected = entry == granularity
                    val label = when (entry) {
                        TimeGranularity.HOURS -> stringResource(R.string.granularity_hours)
                        TimeGranularity.DAYS -> stringResource(R.string.granularity_days)
                        TimeGranularity.WEEKS -> stringResource(R.string.granularity_weeks)
                        TimeGranularity.MONTHS -> stringResource(R.string.granularity_months)
                        TimeGranularity.YEARS -> stringResource(R.string.granularity_years)
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { 
                            onGranularitySelected(entry)
                            onRefresh() 
                        },
                        label = { 
                            Text(
                                text = label, 
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
