package com.moxmose.moxequiplog.ui.reports

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.Equipment
import com.moxmose.moxequiplog.data.local.OperationType
import com.moxmose.moxequiplog.ui.components.ImageIcon
import com.moxmose.moxequiplog.utils.UiConstants
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineSpec
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.shader.color
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.common.shader.DynamicShader
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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

                    Box(modifier = Modifier.weight(1f)) {
                        EquipmentMultiSelector(
                            equipments = uiState.equipments,
                            selectedIds = uiState.selectedEquipmentIds,
                            onToggleSelection = { 
                                viewModel.toggleEquipmentSelection(it)
                                viewModel.refresh()
                            },
                            categoryColor = categoryColor
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
                ReportsSettingsCard(
                    colorMode = uiState.colorMode,
                    onColorModeChange = viewModel::setColorMode
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.km_trend),
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (uiState.equipmentUnitLabel.isNotBlank()) {
                                Surface(
                                    color = if (uiState.hasMixedUnits) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.extraSmall
                                ) {
                                    Text(
                                        text = uiState.equipmentUnitLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = if (uiState.hasMixedUnits) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
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
                        
                        if (uiState.equipmentChartData.values.all { it.isEmpty() }) {
                            NoDataPlaceholder()
                        } else {
                            MultiLineChart(
                                chartDataMap = uiState.equipmentChartData,
                                granularity = uiState.timeGranularity,
                                colorMode = uiState.colorMode,
                                customColors = uiState.customColors
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportsSettingsCard(
    colorMode: String,
    onColorModeChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.options_sections_colors_title),
                style = MaterialTheme.typography.titleSmall
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = colorMode == UiConstants.REPORTS_COLOR_MODE_M3,
                    onClick = { onColorModeChange(UiConstants.REPORTS_COLOR_MODE_M3) },
                    label = { Text("Material 3") },
                    leadingIcon = if (colorMode == UiConstants.REPORTS_COLOR_MODE_M3) {
                        { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                    } else null
                )
                FilterChip(
                    selected = colorMode == UiConstants.REPORTS_COLOR_MODE_CUSTOM,
                    onClick = { onColorModeChange(UiConstants.REPORTS_COLOR_MODE_CUSTOM) },
                    label = { Text("Custom") },
                    leadingIcon = if (colorMode == UiConstants.REPORTS_COLOR_MODE_CUSTOM) {
                        { Icon(Icons.Default.Palette, null, Modifier.size(18.dp)) }
                    } else null
                )
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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

                    Box(modifier = Modifier.weight(1f)) {
                        OperationMultiSelector(
                            operationTypes = uiState.operationTypes,
                            selectedIds = uiState.selectedOperationTypeIds,
                            onToggleSelection = { 
                                viewModel.toggleOperationTypeSelection(it)
                                viewModel.refresh()
                            },
                            categoryColor = categoryColor
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
                ReportsSettingsCard(
                    colorMode = uiState.colorMode,
                    onColorModeChange = viewModel::setColorMode
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
                        Text(
                            text = stringResource(R.string.km_trend),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        if (uiState.operationChartData.values.all { it.isEmpty() }) {
                            NoDataPlaceholder()
                        } else {
                            MultiLineChart(
                                chartDataMap = uiState.operationChartData,
                                granularity = uiState.timeGranularity,
                                colorMode = uiState.colorMode,
                                customColors = uiState.customColors
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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

                    Box(modifier = Modifier.weight(1f)) {
                        EquipmentMultiSelector(
                            equipments = uiState.equipments,
                            selectedIds = uiState.selectedEquipmentIds,
                            onToggleSelection = { 
                                viewModel.toggleEquipmentSelection(it)
                                viewModel.refresh()
                            },
                            categoryColor = categoryColor
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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

                    Box(modifier = Modifier.weight(1f)) {
                        OperationMultiSelector(
                            operationTypes = uiState.operationTypes,
                            selectedIds = uiState.selectedOperationTypeIds,
                            onToggleSelection = { 
                                viewModel.toggleOperationTypeSelection(it)
                                viewModel.refresh()
                            },
                            categoryColor = categoryColor
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

@Composable
fun DistributionCard(
    title: String,
    distribution: List<PieChartPoint>,
    categoryColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.distribution_analysis) + " - " + title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            if (distribution.isEmpty()) {
                Text(
                    text = stringResource(R.string.report_no_data),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val total = distribution.sumOf { it.value.toDouble() }.toFloat()
                distribution.forEach { item ->
                    val percentage = if (total > 0) (item.value / total) else 0f
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = item.label, style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = "${item.value.toInt()} (${(percentage * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { percentage },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small),
                            color = categoryColor,
                            trackColor = categoryColor.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentMultiSelector(
    equipments: List<Equipment>,
    selectedIds: Set<Int>,
    onToggleSelection: (Int) -> Unit,
    categoryColor: Color
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
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = equipment.id in selectedIds,
                                onCheckedChange = null
                            )
                            ImageIcon(
                                iconIdentifier = equipment.iconIdentifier,
                                photoUri = equipment.photoUri,
                                modifier = Modifier.size(24.dp),
                                tint = if (equipment.id in selectedIds) categoryColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = equipment.description,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (equipment.id in selectedIds) FontWeight.Bold else FontWeight.Normal
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
    categoryColor: Color
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
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = op.id in selectedIds,
                                onCheckedChange = null
                            )
                            ImageIcon(
                                iconIdentifier = op.iconIdentifier,
                                photoUri = op.photoUri,
                                modifier = Modifier.size(24.dp),
                                tint = if (op.id in selectedIds) categoryColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = op.description,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (op.id in selectedIds) FontWeight.Bold else FontWeight.Normal
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
    colorMode: String,
    customColors: List<String>
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val dateFormat = remember(granularity) {
        when (granularity) {
            TimeGranularity.HOURS -> SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
            TimeGranularity.DAYS, TimeGranularity.WEEKS -> SimpleDateFormat("dd/MM", Locale.getDefault())
            TimeGranularity.MONTHS -> SimpleDateFormat("MM/yy", Locale.getDefault())
            TimeGranularity.YEARS -> SimpleDateFormat("yyyy", Locale.getDefault())
        }
    }

    val chartColors = remember(colorMode, customColors) {
        if (colorMode == UiConstants.REPORTS_COLOR_MODE_CUSTOM && customColors.isNotEmpty()) {
            customColors.map { Color(it.toColorInt()) }
        } else {
            null // Use default Vico/M3 logic
        }
    }

    val m3Colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
    )

    val finalColors = chartColors ?: m3Colors

    LaunchedEffect(chartDataMap, granularity) {
        val validSeries = chartDataMap.values.filter { it.isNotEmpty() }
        if (validSeries.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries {
                    validSeries.forEach { points ->
                        series(
                            x = points.map { it.date },
                            y = points.map { it.kilometers }
                        )
                    }
                }
            }
        }
    }

    key(granularity, chartDataMap.keys, colorMode) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lines = chartDataMap.keys.mapIndexed { index, _ ->
                        rememberLineSpec(
                            shader = DynamicShader.color(finalColors[index % finalColors.size])
                        )
                    }
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
