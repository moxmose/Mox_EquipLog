package com.moxmose.moxequiplog.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.Equipment
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
//import com.patrykandpatrick.vico.compose.cartesian.axis.rememberHorizontalAxis
//import com.patrykandpatrick.vico.compose.cartesian.axis.rememberVerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .statusBarsPadding()
    ) {
        Text(
            text = stringResource(R.string.navigation_reports),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        EquipmentSelector(
            equipments = uiState.equipments,
            selectedId = uiState.selectedEquipmentId,
            onEquipmentSelected = { viewModel.selectEquipment(it.id) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.chartData.isNotEmpty()) {
            KilometersChart(chartData = uiState.chartData)
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No data available for this equipment")
            }
        }
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
            value = selectedEquipment?.description ?: "Select Equipment",
            onValueChange = {},
            readOnly = true,
            label = { Text("Equipment") },
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

@Composable
fun KilometersChart(chartData: List<ChartPoint>) {
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
            startAxis = VerticalAxis.rememberStart(), //rememberVerticalAxis(),
            bottomAxis = HorizontalAxis.rememberBottom( //rememberHorizontalAxis(
                valueFormatter = CartesianValueFormatter { context, value, _ ->
                    val index:Int = value.roundToInt()
                    if (index in chartData.indices) {
                        dateFormat.format(Date(chartData[index].date))
                    } else ""
                }
            ),
        ),
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    )
}
