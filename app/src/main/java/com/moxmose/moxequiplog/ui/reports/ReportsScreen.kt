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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.Equipment
import com.moxmose.moxequiplog.data.local.OperationType
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
import kotlin.math.roundToInt

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
                subtitle = "Analisi km e utilizzo dei mezzi",
                icon = Icons.AutoMirrored.Filled.DirectionsBike,
                onClick = { onNavigate(ReportDestination.EQUIPMENTS) }
            )
        }

        item {
            ReportMenuCard(
                title = stringResource(R.string.report_section_operations),
                subtitle = "Frequenza e km per singola operazione",
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
fun EquipmentReportsScreen(
    viewModel: ReportsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

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
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EquipmentSelector(
                equipments = uiState.equipments,
                selectedId = uiState.selectedEquipmentId,
                onEquipmentSelected = { viewModel.selectEquipment(it.id) }
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.report_km_over_time),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    if (uiState.equipmentChartData.isNotEmpty()) {
                        GenericChart(chartData = uiState.equipmentChartData)
                    } else {
                        NoDataPlaceholder()
                    }
                }
            }
            
            // Qui potrai aggiungere altri filtri o grafici in futuro
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
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OperationSelector(
                operationTypes = uiState.operationTypes,
                selectedId = uiState.selectedOperationTypeId,
                onOperationSelected = { viewModel.selectOperationType(it.id) }
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.report_km_to_date),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    if (uiState.operationChartData.isNotEmpty()) {
                        GenericChart(chartData = uiState.operationChartData)
                    } else {
                        NoDataPlaceholder()
                    }
                }
            }
            
            // Qui potrai aggiungere altri filtri o grafici in futuro
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
fun EquipmentSelector(
    equipments: List<Equipment>,
    selectedId: Int?,
    onEquipmentSelected: (Equipment) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedEquipment = equipments.find { it.id == selectedId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedEquipment?.description ?: stringResource(R.string.select_equipment),
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
                DropdownMenuItem(
                    text = { Text(equipment.description) },
                    onClick = {
                        onEquipmentSelected(equipment)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationSelector(
    operationTypes: List<OperationType>,
    selectedId: Int?,
    onOperationSelected: (OperationType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOp = operationTypes.find { it.id == selectedId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedOp?.description ?: stringResource(R.string.select_operation),
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
                DropdownMenuItem(
                    text = { Text(op.description) },
                    onClick = {
                        onOperationSelected(op)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun GenericChart(chartData: List<ChartPoint>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val dateFormat = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }

    LaunchedEffect(chartData) {
        modelProducer.runTransaction {
            lineSeries {
                series(chartData.map { it.kilometers })
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = CartesianValueFormatter { _, value, _ ->
                    val index = value.roundToInt()
                    if (index in chartData.indices) {
                        dateFormat.format(Date(chartData[index].date))
                    } else ""
                }
            ),
        ),
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
    )
}
