package com.moxmose.moxequiplog.ui.reports

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.Equipment
import com.moxmose.moxequiplog.data.local.OperationType
import com.moxmose.moxequiplog.ui.components.ImageIcon
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class ReportDestination {
    MENU,
    EQUIPMENTS,
    OPERATIONS
}

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = koinViewModel()
) {
    var currentSubDestination by remember { mutableStateOf(ReportDestination.MENU) }

    BackHandler(enabled = currentSubDestination != ReportDestination.MENU) {
        currentSubDestination = ReportDestination.MENU
    }

    AnimatedContent(
        targetState = currentSubDestination,
        label = "ReportNavigation"
    ) { destination ->
        when (destination) {
            ReportDestination.MENU -> ReportMenu(
                onNavigate = { currentSubDestination = it }
            )
            ReportDestination.EQUIPMENTS -> EquipmentReportsScreen(
                viewModel = viewModel,
                onBack = { currentSubDestination = ReportDestination.MENU }
            )
            ReportDestination.OPERATIONS -> OperationReportsScreen(
                viewModel = viewModel,
                onBack = { currentSubDestination = ReportDestination.MENU }
            )
        }
    }
}

@Composable
fun ReportMenu(onNavigate: (ReportDestination) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.navigation_reports),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        item {
            ReportMenuCard(
                title = stringResource(R.string.report_section_equipments),
                subtitle = stringResource(R.string.report_equipment_subtitle),
                icon = Icons.AutoMirrored.Filled.DirectionsBike,
                onClick = { onNavigate(ReportDestination.EQUIPMENTS) }
            )
        }

        item {
            ReportMenuCard(
                title = stringResource(R.string.report_section_operations),
                subtitle = stringResource(R.string.report_operation_subtitle),
                icon = Icons.Default.Build,
                onClick = { onNavigate(ReportDestination.OPERATIONS) }
            )
        }
    }
}

@Composable
fun ReportMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Column(modifier = Modifier.weight(1.0f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentReportsScreen(
    viewModel: ReportsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val categoryColor = remember(uiState.equipmentCategoryColor) {
        try { Color(uiState.equipmentCategoryColor.toColorInt()) } catch (_: Exception) { Color.Gray }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.report_section_equipments)) },
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
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val unitPart = if (uiState.equipmentUnitLabel.isNotEmpty()) "[${uiState.equipmentUnitLabel}] " else ""
                        Text(
                            text = unitPart + stringResource(R.string.report_value_over_time),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        if (uiState.equipmentChartData.isNotEmpty()) {
                            MultiLineChart(chartDataMap = uiState.equipmentChartData, granularity = uiState.timeGranularity)
                        } else {
                            NoDataPlaceholder()
                        }
                    }
                }
            }

            item {
                DistributionCard(
                    title = stringResource(R.string.report_section_equipments),
                    distribution = uiState.equipmentDistribution,
                    categoryColor = categoryColor
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationReportsScreen(
    viewModel: ReportsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val categoryColor = remember(uiState.operationCategoryColor) {
        try { Color(uiState.operationCategoryColor.toColorInt()) } catch (_: Exception) { Color.Gray }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.report_section_operations)) },
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
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.report_value_to_date),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        if (uiState.operationChartData.isNotEmpty()) {
                            MultiLineChart(chartDataMap = uiState.operationChartData, granularity = uiState.timeGranularity)
                        } else {
                            NoDataPlaceholder()
                        }
                    }
                }
            }

            item {
                DistributionCard(
                    title = stringResource(R.string.report_section_operations),
                    distribution = uiState.operationDistribution,
                    categoryColor = categoryColor
                )
            }
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
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            equipments.forEach { equipment ->
                val isSelected = selectedIds.contains(equipment.id)
                DropdownMenuItem(
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isSelected, onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = (equipment.description.takeIf { it.isNotBlank() } ?: stringResource(R.string.id_no_description, equipment.id)) + if (equipment.dismissed) " " + stringResource(R.string.dismissed_suffix) else "",
                                color = if (equipment.dismissed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else Color.Unspecified
                            )
                        }
                    },
                    leadingIcon = {
                        ImageIcon(
                            photoUri = equipment.photoUri,
                            iconIdentifier = equipment.iconIdentifier,
                            modifier = Modifier.size(24.dp).graphicsLayer(alpha = if (equipment.dismissed) 0.5f else 1f),
                            category = Category.EQUIPMENT,
                            borderColor = categoryColor,
                            contentPadding = 2.dp
                        )
                    },
                    onClick = {
                        onToggleSelection(equipment.id)
                    }
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
        stringResource(R.string.select_operation)
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
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            operationTypes.forEach { op ->
                val isSelected = selectedIds.contains(op.id)
                DropdownMenuItem(
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isSelected, onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = (op.description.takeIf { it.isNotBlank() } ?: stringResource(R.string.id_no_description, op.id)) + if (op.dismissed) " " + stringResource(R.string.dismissed_suffix) else "",
                                color = if (op.dismissed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else Color.Unspecified
                            )
                        }
                    },
                    leadingIcon = {
                        ImageIcon(
                            photoUri = op.photoUri,
                            iconIdentifier = op.iconIdentifier,
                            modifier = Modifier.size(24.dp).graphicsLayer(alpha = if (op.dismissed) 0.5f else 1f),
                            category = Category.OPERATION,
                            borderColor = categoryColor,
                            contentPadding = 2.dp
                        )
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
fun MultiLineChart(chartDataMap: Map<Int, List<ChartPoint>>, granularity: TimeGranularity) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val dateFormat = remember(granularity) {
        when (granularity) {
            TimeGranularity.HOURS -> SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
            TimeGranularity.DAYS, TimeGranularity.WEEKS -> SimpleDateFormat("dd/MM", Locale.getDefault())
            TimeGranularity.MONTHS -> SimpleDateFormat("MM/yy", Locale.getDefault())
            TimeGranularity.YEARS -> SimpleDateFormat("yyyy", Locale.getDefault())
        }
    }

    LaunchedEffect(chartDataMap, granularity) {
        // Filtriamo le serie che non hanno punti per evitare l'eccezione "Series can't be empty"
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

    key(granularity, chartDataMap.keys) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(),
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
