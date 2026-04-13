@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.moxmose.moxequiplog.ui.reports

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.moxmose.moxequiplog.data.local.ReportFilter
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.ui.components.ImageIcon
import com.moxmose.moxequiplog.utils.UiConstants
import androidx.compose.ui.graphics.toArgb
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.ui.platform.LocalContext
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.common.Fill
import com.patrykandpatrick.vico.core.common.component.LineComponent
import com.patrykandpatrick.vico.core.common.component.ShapeComponent
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.pdf.PdfDocument
import androidx.compose.ui.platform.LocalView
import androidx.core.view.drawToBitmap
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

enum class ReportDestination {
    MENU, EQUIPMENTS, OPERATIONS, EQUIPMENTS_FREQ, OPERATIONS_FREQ, INTERVALS, HEATMAP, BENCHMARKING,
    EQUIPMENTS_VOL, OPERATIONS_VOL, COMBINED_LOGS
}

@Composable
fun ReportsScreen(modifier: Modifier = Modifier, viewModel: ReportsViewModel = koinViewModel(), onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    var currentDestination by remember { mutableStateOf(ReportDestination.MENU) }
    BackHandler(enabled = currentDestination != ReportDestination.MENU) { currentDestination = ReportDestination.MENU }
    AnimatedContent(targetState = currentDestination, label = "ReportsNavigation") { destination ->
        when (destination) {
            ReportDestination.MENU -> ReportsMenu(onNavigate = { currentDestination = it }, onBack = onBack)
            ReportDestination.EQUIPMENTS -> EquipmentsReportScreen(uiState, viewModel) { currentDestination = ReportDestination.MENU }
            ReportDestination.OPERATIONS -> OperationsReportScreen(uiState, viewModel) { currentDestination = ReportDestination.MENU }
            ReportDestination.EQUIPMENTS_FREQ -> EquipmentsFreqReportScreen(uiState, viewModel) { currentDestination = ReportDestination.MENU }
            ReportDestination.OPERATIONS_FREQ -> OperationsFreqReportScreen(uiState, viewModel) { currentDestination = ReportDestination.MENU }
            ReportDestination.INTERVALS -> IntervalsReportScreen(uiState, viewModel) { currentDestination = ReportDestination.MENU }
            ReportDestination.HEATMAP -> HeatmapReportScreen(uiState, viewModel) { currentDestination = ReportDestination.MENU }
            ReportDestination.BENCHMARKING -> BenchmarkingReportScreen(uiState, viewModel) { currentDestination = ReportDestination.MENU }
            ReportDestination.EQUIPMENTS_VOL -> EquipmentsVolReportScreen(uiState, viewModel) { currentDestination = ReportDestination.MENU }
            ReportDestination.OPERATIONS_VOL -> OperationsVolReportScreen(uiState, viewModel) { currentDestination = ReportDestination.MENU }
            ReportDestination.COMBINED_LOGS -> CombinedLogsReportScreen(uiState, viewModel) { currentDestination = ReportDestination.MENU }
        }
    }
}


@Composable
fun ReportBaseScreen(
    title: String,
    onBack: () -> Unit,
    uiState: ReportsUiState,
    viewModel: ReportsViewModel,
    selector: @Composable () -> Unit,
    onSelectAll: (() -> Unit)? = null,
    onInvertSelection: (() -> Unit)? = null,
    onClearSelection: (() -> Unit)? = null,
    granularityEnabled: Boolean = true,
    content: LazyListScope.() -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var isSharingPdf by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text(title) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }) }, containerColor = MaterialTheme.colorScheme.background) { padding ->
        if (isSharingPdf) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                Card {
                    Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text(stringResource(R.string.sharing_pdf_report))
                    }
                }
            }
        }

        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(16.dp)) {
            item { 
                ReportSelectionHeader(
                    selector = selector, 
                    showDismissed = uiState.showDismissed, 
                    onToggleShowDismissed = { viewModel.toggleShowDismissed(); viewModel.refresh() }, 
                    onSelectAll = onSelectAll?.let { { it(); viewModel.refresh() } }, 
                    onInvertSelection = onInvertSelection?.let { { it(); viewModel.refresh() } }, 
                    onClearSelection = onClearSelection?.let { { it(); viewModel.refresh() } }
                ) 
            }
            item { StandardFilterSection(startDate = uiState.startDate, endDate = uiState.endDate, granularity = uiState.timeGranularity, onDateRangeSelected = viewModel::setDateRange, onGranularitySelected = viewModel::setTimeGranularity, onReset = viewModel::resetFilters, onRefresh = viewModel::refresh, enabledGranularity = granularityEnabled) }
            item { FilterManagementRow(savedFilters = uiState.savedFilters, activeFilterName = uiState.activeFilterName, isDirty = uiState.isFilterDirty, onSaveNew = viewModel::saveAsNewFilter, onOverwrite = viewModel::overwriteActiveFilter, onApply = { viewModel.applySavedFilter(it); viewModel.refresh() }, onDelete = viewModel::deleteSavedFilter) }
            content()

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            val csvData = viewModel.getCsvExportData(title)
                            val fileName = "Export_${title.replace(" ", "_")}.csv"
                            shareCsvFile(context, fileName, csvData)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.export_data_csv))
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isSharingPdf = true
                                delay(500) // Aspetta che la UI sia stabile
                                try {
                                    val bitmap = view.drawToBitmap(Bitmap.Config.ARGB_8888)
                                    val fileName = "Report_${title.replace(" ", "_")}.pdf"
                                    val pdfFile = File(context.cacheDir, fileName)
                                    
                                    val document = PdfDocument()
                                    val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
                                    val page = document.startPage(pageInfo)
                                    page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                                    document.finishPage(page)
                                    
                                    pdfFile.outputStream().use { document.writeTo(it) }
                                    document.close()
                                    
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", pdfFile)
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Condividi Report PDF"))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    isSharingPdf = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.export_report_pdf))
                    }
                }
            }
        }
    }
}

fun shareCsvFile(context: Context, fileName: String, content: String) {
    try {
        val file = File(context.cacheDir, fileName)
        file.writeText(content)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Esporta report"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun ReportSelectionHeader(
    selector: @Composable (() -> Unit),
    showDismissed: Boolean,
    onToggleShowDismissed: () -> Unit,
    onSelectAll: (() -> Unit)? = null,
    onInvertSelection: (() -> Unit)? = null,
    onClearSelection: (() -> Unit)? = null
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.weight(1f)) { selector() }
        
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            IconButton(onClick = onToggleShowDismissed, colors = IconButtonDefaults.iconButtonColors(containerColor = if (showDismissed) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)) { 
                Icon(imageVector = if (showDismissed) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = if (showDismissed) stringResource(R.string.hide_dismissed) else stringResource(R.string.show_dismissed), tint = if (showDismissed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) 
            }
            
            if (onSelectAll != null && onInvertSelection != null && onClearSelection != null) {
                IconButton(onClick = onSelectAll) { Icon(imageVector = Icons.Default.SelectAll, contentDescription = stringResource(R.string.selection_all), tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = onInvertSelection) { Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = stringResource(R.string.selection_invert), tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = onClearSelection) { Icon(imageVector = Icons.Default.FilterAltOff, contentDescription = stringResource(R.string.selection_clear), tint = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> GenericMultiSelector(
    items: List<T>,
    selectedIds: Set<Int>,
    onToggleSelection: (Int) -> Unit,
    categoryColor: Color,    chartColors: List<Color>,
    label: String,
    placeholder: String,
    category: String,
    getId: (T) -> Int,
    getDescription: (T) -> String,
    getIconIdentifier: (T) -> String?,
    getPhotoUri: (T) -> String?
) {
    var expanded by remember { mutableStateOf(false) }

    val displayText = if (selectedIds.isEmpty()) placeholder else {
        val firstSelected = items.find { getId(it) == selectedIds.first() }?.let(getDescription)?.takeIf { it.isNotBlank() }
        if (selectedIds.size > 1) "$firstSelected + ${selectedIds.size - 1}" else firstSelected ?: "ID: ${selectedIds.first()}"
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = categoryColor,
                focusedLabelColor = categoryColor
            ),
            singleLine = true,
            maxLines = 1,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEachIndexed { index, item ->
                val id = getId(item)
                val isSelected = id in selectedIds

                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(checked = isSelected, onCheckedChange = null)

                            // Pallino colore grafico
                            val dotColor = if (chartColors.isNotEmpty()) chartColors[index % chartColors.size] else categoryColor
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(if (isSelected) dotColor else Color.Transparent.copy(alpha = 0.1f)))

                            ImageIcon(
                                iconIdentifier = getIconIdentifier(item),
                                photoUri = getPhotoUri(item),
                                modifier = Modifier.size(24.dp),
                                category = category,
                                tint = if (isSelected) categoryColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = getDescription(item),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    },
                    onClick = {
                        onToggleSelection(id)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}


// --- SCREENS ---

@Composable
fun EquipmentsReportScreen(uiState: ReportsUiState, viewModel: ReportsViewModel, onBack: () -> Unit) {
    val categoryColor = Color(uiState.equipmentCategoryColor.toColorInt())
    val chartColors = rememberChartColors(uiState.colorMode, uiState.customColors)
    val allIds = remember(uiState.equipments) { uiState.equipments.map { it.id } }
    val selectedIdsOnly = remember(uiState.equipments, uiState.selectedEquipmentIds) { uiState.equipments.map { it.id }.filter { it in uiState.selectedEquipmentIds } }
    
    ReportBaseScreen(title = stringResource(R.string.report_equipments_title), onBack = onBack, uiState = uiState, viewModel = viewModel, selector = { GenericMultiSelector(items = uiState.equipments, selectedIds = uiState.selectedEquipmentIds, onToggleSelection = { viewModel.toggleEquipmentSelection(it); viewModel.refresh() }, categoryColor = categoryColor, chartColors = chartColors, label = stringResource(R.string.navigation_equipments), placeholder = stringResource(R.string.select_equipment), category = Category.EQUIPMENT, getId = { it.id }, getDescription = { it.description }, getIconIdentifier = { it.iconIdentifier }, getPhotoUri = { it.photoUri }) }, onSelectAll = viewModel::selectAllEquipment, onInvertSelection = viewModel::invertEquipmentSelection, onClearSelection = viewModel::clearEquipmentSelection) {
        item { EquipmentChartCard(chartData = uiState.equipmentChartData, unitLabel = uiState.equipmentUnitLabel, hasMixedUnits = uiState.hasMixedUnits, requestedGranularity = uiState.timeGranularity, effectiveGranularity = uiState.effectiveGranularity, colors = chartColors, allStableIds = allIds, selectedIds = selectedIdsOnly, equipments = uiState.equipments) }
    }
}

@Composable
fun OperationsReportScreen(uiState: ReportsUiState, viewModel: ReportsViewModel, onBack: () -> Unit) {
    val categoryColor = Color(uiState.operationCategoryColor.toColorInt())
    val chartColors = rememberChartColors(uiState.colorMode, uiState.customColors)
    val allIds = remember(uiState.operationTypes) { uiState.operationTypes.map { it.id } }
    val selectedIdsOnly = remember(uiState.operationTypes, uiState.selectedOperationTypeIds) { uiState.operationTypes.map { it.id }.filter { it in uiState.selectedOperationTypeIds } }
    
    ReportBaseScreen(title = stringResource(R.string.report_operations_title), onBack = onBack, uiState = uiState, viewModel = viewModel, selector = { GenericMultiSelector(items = uiState.operationTypes, selectedIds = uiState.selectedOperationTypeIds, onToggleSelection = { viewModel.toggleOperationTypeSelection(it); viewModel.refresh() }, categoryColor = categoryColor, chartColors = chartColors, label = stringResource(R.string.navigation_operations), placeholder = stringResource(R.string.select_operation_type), category = Category.OPERATION, getId = { it.id }, getDescription = { it.description }, getIconIdentifier = { it.iconIdentifier }, getPhotoUri = { it.photoUri }) }, onSelectAll = viewModel::selectAllOperationTypes, onInvertSelection = viewModel::invertOperationTypeSelection, onClearSelection = viewModel::clearOperationTypeSelection) {
        item { Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) { Column(modifier = Modifier.padding(16.dp)) { val unitsPart = if (uiState.operationUnitLabel.isNotBlank()) uiState.operationUnitLabel.split(", ").joinToString("") { "[$it]" } else "" ; Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(text = "$unitsPart ${stringResource(R.string.report_value_over_time)}", style = MaterialTheme.typography.titleMedium) ; if (uiState.opHasMixedUnits) Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.extraSmall) { Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp).padding(2.dp), tint = MaterialTheme.colorScheme.onErrorContainer) } } ; if (uiState.opHasMixedUnits) Text(text = stringResource(R.string.mixed_units_warning), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp)) ; Spacer(modifier = Modifier.height(16.dp)); MultiLineChart(chartDataMap = uiState.operationChartData, requestedGranularity = uiState.timeGranularity, effectiveGranularity = uiState.effectiveGranularity, finalColors = chartColors, allStableIds = allIds) ; if (uiState.operationChartData.any { it.value.isNotEmpty() }) { Spacer(modifier = Modifier.height(16.dp)); ChartLegend(items = selectedIdsOnly.mapNotNull { id -> uiState.operationTypes.find { it.id == id }?.description?.let { id to it } }, colors = chartColors, allStableIds = allIds) } } } }
    }
}

@Composable
fun EquipmentsFreqReportScreen(uiState: ReportsUiState, viewModel: ReportsViewModel, onBack: () -> Unit) {
    val categoryColor = Color(uiState.equipmentCategoryColor.toColorInt())
    val chartColors = rememberChartColors(uiState.colorMode, uiState.customColors)
    ReportBaseScreen(title = stringResource(R.string.report_equipments_freq_title), onBack = onBack, uiState = uiState, viewModel = viewModel, selector = { GenericMultiSelector(items = uiState.equipments, selectedIds = uiState.selectedEquipmentIds, onToggleSelection = { viewModel.toggleEquipmentSelection(it); viewModel.refresh() }, categoryColor = categoryColor, chartColors = chartColors, label = stringResource(R.string.navigation_equipments), placeholder = stringResource(R.string.select_equipment), category = Category.EQUIPMENT, getId = { it.id }, getDescription = { it.description }, getIconIdentifier = { it.iconIdentifier }, getPhotoUri = { it.photoUri }) }, onSelectAll = viewModel::selectAllEquipment, onInvertSelection = viewModel::invertEquipmentSelection, onClearSelection = viewModel::clearEquipmentSelection) {
        if (uiState.timeGranularity == null || uiState.equipmentDistributionByPeriod.isEmpty()) {
            item { PieChartCard(title = stringResource(R.string.report_section_equipments_freq), distribution = uiState.equipmentDistribution) }
        } else {
            items(uiState.equipmentDistributionByPeriod.keys.toList().sorted()) { period ->
                PieChartCard(title = "${stringResource(R.string.report_section_equipments_freq)}: $period", distribution = uiState.equipmentDistributionByPeriod[period] ?: emptyList())
            }
        }
    }
}

@Composable
fun OperationsFreqReportScreen(uiState: ReportsUiState, viewModel: ReportsViewModel, onBack: () -> Unit) {
    val categoryColor = Color(uiState.operationCategoryColor.toColorInt())
    val chartColors = rememberChartColors(uiState.colorMode, uiState.customColors)
    ReportBaseScreen(title = stringResource(R.string.report_operations_freq_title), onBack = onBack, uiState = uiState, viewModel = viewModel, selector = { GenericMultiSelector(items = uiState.operationTypes, selectedIds = uiState.selectedOperationTypeIds, onToggleSelection = { viewModel.toggleOperationTypeSelection(it); viewModel.refresh() }, categoryColor = categoryColor, chartColors = chartColors, label = stringResource(R.string.navigation_operations), placeholder = stringResource(R.string.select_operation_type), category = Category.OPERATION, getId = { it.id }, getDescription = { it.description }, getIconIdentifier = { it.iconIdentifier }, getPhotoUri = { it.photoUri }) }, onSelectAll = viewModel::selectAllOperationTypes, onInvertSelection = viewModel::invertOperationTypeSelection, onClearSelection = viewModel::clearOperationTypeSelection) {
        if (uiState.timeGranularity == null || uiState.operationDistributionByPeriod.isEmpty()) {
            item { PieChartCard(title = stringResource(R.string.report_section_operations_freq), distribution = uiState.operationDistribution) }
        } else {
            items(uiState.operationDistributionByPeriod.keys.toList().sorted()) { period ->
                PieChartCard(title = "${stringResource(R.string.report_section_operations_freq)}: $period", distribution = uiState.operationDistributionByPeriod[period] ?: emptyList())
            }
        }
    }
}

@Composable
fun IntervalsReportScreen(uiState: ReportsUiState, viewModel: ReportsViewModel, onBack: () -> Unit) {
    val categoryColor = Color(uiState.equipmentCategoryColor.toColorInt())
    val chartColors = rememberChartColors(uiState.colorMode, uiState.customColors)
    val allIds = remember(uiState.equipments) { uiState.equipments.map { it.id } }
    val selectedIdsOnly = remember(uiState.equipments, uiState.selectedEquipmentIds) { uiState.equipments.map { it.id }.filter { it in uiState.selectedEquipmentIds } }
    
    ReportBaseScreen(title = stringResource(R.string.report_intervals_title), onBack = onBack, uiState = uiState, viewModel = viewModel, selector = { GenericMultiSelector(items = uiState.equipments, selectedIds = uiState.selectedEquipmentIds, onToggleSelection = { viewModel.toggleEquipmentSelection(it); viewModel.refresh() }, categoryColor = categoryColor, chartColors = chartColors, label = stringResource(R.string.navigation_equipments), placeholder = stringResource(R.string.select_equipment), category = Category.EQUIPMENT, getId = { it.id }, getDescription = { it.description }, getIconIdentifier = { it.iconIdentifier }, getPhotoUri = { it.photoUri }) }, onSelectAll = viewModel::selectAllEquipment, onInvertSelection = viewModel::invertEquipmentSelection, onClearSelection = viewModel::clearEquipmentSelection) {
        item { EquipmentChartCard(title = stringResource(R.string.report_delta_label), chartData = uiState.intervalData, unitLabel = uiState.equipmentUnitLabel, hasMixedUnits = uiState.hasMixedUnits, requestedGranularity = uiState.timeGranularity, effectiveGranularity = uiState.effectiveGranularity, colors = chartColors, allStableIds = allIds, selectedIds = selectedIdsOnly, equipments = uiState.equipments) }
    }
}

@Composable
fun HeatmapReportScreen(uiState: ReportsUiState, viewModel: ReportsViewModel, onBack: () -> Unit) {
    val categoryColor = Color(uiState.equipmentCategoryColor.toColorInt())
    ReportBaseScreen(title = stringResource(R.string.report_heatmap_title), onBack = onBack, uiState = uiState, viewModel = viewModel, selector = { GenericMultiSelector(items = uiState.equipments, selectedIds = uiState.selectedEquipmentIds, onToggleSelection = { viewModel.toggleEquipmentSelection(it); viewModel.refresh() }, categoryColor = categoryColor, chartColors = emptyList(), label = stringResource(R.string.navigation_equipments), placeholder = stringResource(R.string.select_equipment), category = Category.EQUIPMENT, getId = { it.id }, getDescription = { it.description }, getIconIdentifier = { it.iconIdentifier }, getPhotoUri = { it.photoUri }) }, onSelectAll = viewModel::selectAllEquipment, onInvertSelection = viewModel::invertEquipmentSelection, onClearSelection = viewModel::clearEquipmentSelection, granularityEnabled = false) {
        item { HeatmapCard(title = stringResource(R.string.report_heatmap_title), data = uiState.heatmapData, baseColor = categoryColor) }
    }
}

@Composable
fun BenchmarkingReportScreen(uiState: ReportsUiState, viewModel: ReportsViewModel, onBack: () -> Unit) {
    val categoryColor = Color(uiState.equipmentCategoryColor.toColorInt())
    val chartColors = rememberChartColors(uiState.colorMode, uiState.customColors)
    ReportBaseScreen(title = stringResource(R.string.report_benchmarking_title), onBack = onBack, uiState = uiState, viewModel = viewModel, selector = { GenericMultiSelector(items = uiState.equipments, selectedIds = uiState.selectedEquipmentIds, onToggleSelection = { viewModel.toggleEquipmentSelection(it); viewModel.refresh() }, categoryColor = categoryColor, chartColors = chartColors, label = stringResource(R.string.navigation_equipments), placeholder = stringResource(R.string.select_equipment), category = Category.EQUIPMENT, getId = { it.id }, getDescription = { it.description }, getIconIdentifier = { it.iconIdentifier }, getPhotoUri = { it.photoUri }) }, onSelectAll = viewModel::selectAllEquipment, onInvertSelection = viewModel::invertEquipmentSelection, onClearSelection = viewModel::clearEquipmentSelection) {
        if (uiState.timeGranularity == null || uiState.timeGranularity == TimeGranularity.HOURS) {
            item { BenchmarkCard(data = uiState.benchmarkData, colors = chartColors) }
        } else {
            items(uiState.benchmarkByPeriod.keys.toList().sorted()) { period ->
                BenchmarkCard(title = "${stringResource(R.string.report_benchmarking_title)}: $period", data = uiState.benchmarkByPeriod[period] ?: emptyList(), colors = chartColors)
            }
        }
    }
}

@Composable
fun EquipmentsVolReportScreen(uiState: ReportsUiState, viewModel: ReportsViewModel, onBack: () -> Unit) {
    val categoryColor = Color(uiState.equipmentCategoryColor.toColorInt())
    val chartColors = rememberChartColors(uiState.colorMode, uiState.customColors)
    val allIds = remember(uiState.equipments) { uiState.equipments.map { it.id } }
    val selectedIdsOnly = remember(uiState.equipments, uiState.selectedEquipmentIds) { uiState.equipments.map { it.id }.filter { it in uiState.selectedEquipmentIds } }
    
    ReportBaseScreen(title = stringResource(R.string.report_equipments_vol_title), onBack = onBack, uiState = uiState, viewModel = viewModel, selector = { GenericMultiSelector(items = uiState.equipments, selectedIds = uiState.selectedEquipmentIds, onToggleSelection = { viewModel.toggleEquipmentSelection(it); viewModel.refresh() }, categoryColor = categoryColor, chartColors = chartColors, label = stringResource(R.string.navigation_equipments), placeholder = stringResource(R.string.select_equipment), category = Category.EQUIPMENT, getId = { it.id }, getDescription = { it.description }, getIconIdentifier = { it.iconIdentifier }, getPhotoUri = { it.photoUri }) }, onSelectAll = viewModel::selectAllEquipment, onInvertSelection = viewModel::invertEquipmentSelection, onClearSelection = viewModel::clearEquipmentSelection) {
        item { EquipmentChartCard(title = stringResource(R.string.report_volume_label), chartData = uiState.equipmentVolumeData, unitLabel = "", hasMixedUnits = false, requestedGranularity = uiState.timeGranularity, effectiveGranularity = uiState.effectiveGranularity, colors = chartColors, allStableIds = allIds, selectedIds = selectedIdsOnly, equipments = uiState.equipments) }
    }
}

@Composable
fun OperationsVolReportScreen(uiState: ReportsUiState, viewModel: ReportsViewModel, onBack: () -> Unit) {
    val categoryColor = Color(uiState.operationCategoryColor.toColorInt())
    val chartColors = rememberChartColors(uiState.colorMode, uiState.customColors)
    val allIds = remember(uiState.operationTypes) { uiState.operationTypes.map { it.id } }
    val selectedIdsOnly = remember(uiState.operationTypes, uiState.selectedOperationTypeIds) { uiState.operationTypes.map { it.id }.filter { it in uiState.selectedOperationTypeIds } }
    
    ReportBaseScreen(title = stringResource(R.string.report_operations_vol_title), onBack = onBack, uiState = uiState, viewModel = viewModel, selector = { GenericMultiSelector(items = uiState.operationTypes, selectedIds = uiState.selectedOperationTypeIds, onToggleSelection = { viewModel.toggleOperationTypeSelection(it); viewModel.refresh() }, categoryColor = categoryColor, chartColors = chartColors, label = stringResource(R.string.navigation_operations), placeholder = stringResource(R.string.select_operation_type), category = Category.OPERATION, getId = { it.id }, getDescription = { it.description }, getIconIdentifier = { it.iconIdentifier }, getPhotoUri = { it.photoUri }) }, onSelectAll = viewModel::selectAllOperationTypes, onInvertSelection = viewModel::invertOperationTypeSelection, onClearSelection = viewModel::clearOperationTypeSelection) {
        item { Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) { Column(modifier = Modifier.padding(16.dp)) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(text = stringResource(R.string.report_volume_label), style = MaterialTheme.typography.titleMedium) } ; Spacer(modifier = Modifier.height(16.dp)); MultiLineChart(chartDataMap = uiState.operationVolumeData, requestedGranularity = uiState.timeGranularity, effectiveGranularity = uiState.effectiveGranularity, finalColors = chartColors, allStableIds = allIds) ; if (uiState.operationVolumeData.any { it.value.isNotEmpty() }) { Spacer(modifier = Modifier.height(16.dp)); ChartLegend(items = selectedIdsOnly.mapNotNull { id -> uiState.operationTypes.find { it.id == id }?.description?.let { id to it } }, colors = chartColors, allStableIds = allIds) } } } }
    }
}

@Composable
fun CombinedLogsReportScreen(uiState: ReportsUiState, viewModel: ReportsViewModel, onBack: () -> Unit) {
    val chartColors = rememberChartColors(uiState.colorMode, uiState.customColors)
    ReportBaseScreen(
        title = stringResource(R.string.report_combined_logs_title),
        onBack = onBack,
        uiState = uiState,
        viewModel = viewModel,
        selector = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Riga Equipaggiamenti
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        GenericMultiSelector(
                            items = uiState.equipments,
                            selectedIds = uiState.selectedEquipmentIds,
                            onToggleSelection = { viewModel.toggleEquipmentSelection(it); viewModel.refresh() },
                            categoryColor = Color(uiState.equipmentCategoryColor.toColorInt()),
                            chartColors = emptyList(),
                            label = stringResource(R.string.navigation_equipments),
                            placeholder = stringResource(R.string.select_equipment),
                            category = Category.EQUIPMENT,
                            getId = { it.id },
                            getDescription = { it.description },
                            getIconIdentifier = { it.iconIdentifier },
                            getPhotoUri = { it.photoUri }
                        )
                    }
                    IconButton(onClick = { viewModel.selectAllEquipment(); viewModel.refresh() }) {
                        Icon(Icons.Default.SelectAll, contentDescription = stringResource(R.string.selection_all), tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.invertEquipmentSelection(); viewModel.refresh() }) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = stringResource(R.string.selection_invert), tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.clearEquipmentSelection(); viewModel.refresh() }) {
                        Icon(Icons.Default.FilterAltOff, contentDescription = stringResource(R.string.selection_clear), tint = MaterialTheme.colorScheme.primary)
                    }
                }

                // Riga Operazioni
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        GenericMultiSelector(
                            items = uiState.operationTypes,
                            selectedIds = uiState.selectedOperationTypeIds,
                            onToggleSelection = { viewModel.toggleOperationTypeSelection(it); viewModel.refresh() },
                            categoryColor = Color(uiState.operationCategoryColor.toColorInt()),
                            chartColors = emptyList(),
                            label = stringResource(R.string.navigation_operations),
                            placeholder = stringResource(R.string.select_operation_type),
                            category = Category.OPERATION,
                            getId = { it.id },
                            getDescription = { it.description },
                            getIconIdentifier = { it.iconIdentifier },
                            getPhotoUri = { it.photoUri }
                        )
                    }
                    IconButton(onClick = { viewModel.selectAllOperationTypes(); viewModel.refresh() }) {
                        Icon(Icons.Default.SelectAll, contentDescription = stringResource(R.string.selection_all), tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.invertOperationTypeSelection(); viewModel.refresh() }) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = stringResource(R.string.selection_invert), tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.clearOperationTypeSelection(); viewModel.refresh() }) {
                        Icon(Icons.Default.FilterAltOff, contentDescription = stringResource(R.string.selection_clear), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        onSelectAll = null,
        onInvertSelection = null,
        onClearSelection = null
    ) {
        item { 
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) { 
                Column(modifier = Modifier.padding(16.dp)) { 
                    val unitsPart = if (uiState.operationUnitLabel.isNotBlank()) uiState.operationUnitLabel.split(", ").joinToString("") { "[$it]" } else ""
                    Text(text = "$unitsPart ${stringResource(R.string.report_value_over_time)}", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    val dataWithPoints = uiState.combinedLogsData.filter { it.value.isNotEmpty() }
                    if (dataWithPoints.isNotEmpty()) {
                        val modelProducer = remember { CartesianChartModelProducer() }
                        val lineStyles = remember(dataWithPoints, chartColors) { 
                            dataWithPoints.keys.mapIndexed { index, _ -> 
                                val color = chartColors[if (chartColors.isNotEmpty()) index % chartColors.size else 0]
                                LineCartesianLayer.Line(
                                    fill = LineCartesianLayer.LineFill.single(fill = Fill(color.toArgb())),
                                    pointProvider = LineCartesianLayer.PointProvider.single(
                                        LineCartesianLayer.Point(
                                            ShapeComponent(
                                                shape = CorneredShape.Pill,
                                                color = color.toArgb(),
                                            ),
                                            sizeDp = 4f
                                        )
                                    )
                                )
                            } 
                        }
                        val effectiveGranularity = uiState.effectiveGranularity
                        val dateFormat = remember(effectiveGranularity) { when (effectiveGranularity) { null -> SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()) ; TimeGranularity.HOURS -> SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) ; TimeGranularity.DAYS, TimeGranularity.WEEKS -> SimpleDateFormat("dd/MM", Locale.getDefault()) ; TimeGranularity.MONTHS -> SimpleDateFormat("MM/yy", Locale.getDefault()) ; TimeGranularity.YEARS -> SimpleDateFormat("yyyy", Locale.getDefault()) } }
                        LaunchedEffect(dataWithPoints, effectiveGranularity) { modelProducer.runTransaction { lineSeries { dataWithPoints.values.forEach { points -> series(x = points.map { it.date }, y = points.map { it.kilometers }) } } } }
                        key(uiState.timeGranularity, effectiveGranularity, dataWithPoints.keys) {
                            CartesianChartHost(
                                chart = rememberCartesianChart(
                                    rememberLineCartesianLayer(lineProvider = LineCartesianLayer.LineProvider.series(lineStyles)),
                                    startAxis = VerticalAxis.rememberStart(),
                                    bottomAxis = HorizontalAxis.rememberBottom(
                                        valueFormatter = CartesianValueFormatter { _, value, _ -> dateFormat.format(Date(value.toLong())) },
                                        label = rememberAxisLabelComponent(color = if (uiState.timeGranularity != null && uiState.timeGranularity != effectiveGranularity) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                                    ),
                                ),
                                modelProducer = modelProducer,
                                modifier = Modifier.fillMaxWidth().height(250.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start), verticalArrangement = Arrangement.spacedBy(8.dp)) { 
                            dataWithPoints.keys.forEachIndexed { index, key -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) { val boxColor = if (chartColors.isNotEmpty()) chartColors[index % chartColors.size] else Color.Gray ; Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(boxColor)) ; Spacer(modifier = Modifier.width(6.dp)); Text(text = key, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp) } } 
                        }
                    } else NoDataPlaceholder()
                } 
            } 
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsMenu(onNavigate: (ReportDestination) -> Unit, onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.reports_title)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }) }, containerColor = Color.Transparent) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(horizontal = 16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            item { Text(stringResource(R.string.um_trend), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp)) }
            item { ReportMenuCard(title = stringResource(R.string.report_equipments_title), description = stringResource(R.string.report_equipments_desc), icon = Icons.Default.PrecisionManufacturing, onClick = { onNavigate(ReportDestination.EQUIPMENTS) }) }
            item { ReportMenuCard(title = stringResource(R.string.report_operations_title), description = stringResource(R.string.report_operations_desc), icon = Icons.Default.Settings, onClick = { onNavigate(ReportDestination.OPERATIONS) }) }
            item { ReportMenuCard(title = stringResource(R.string.report_combined_logs_title), description = stringResource(R.string.report_combined_logs_desc), icon = Icons.Default.QueryStats, onClick = { onNavigate(ReportDestination.COMBINED_LOGS) }) }
            
            item { Text(stringResource(R.string.distribution_analysis), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp)) }
            item { ReportMenuCard(title = stringResource(R.string.report_equipments_freq_title), description = stringResource(R.string.report_equipments_freq_desc), icon = Icons.Default.PieChart, onClick = { onNavigate(ReportDestination.EQUIPMENTS_FREQ) }) }
            item { ReportMenuCard(title = stringResource(R.string.report_operations_freq_title), description = stringResource(R.string.report_operations_freq_desc), icon = Icons.Default.BarChart, onClick = { onNavigate(ReportDestination.OPERATIONS_FREQ) }) }
            item { ReportMenuCard(title = stringResource(R.string.report_equipments_vol_title), description = stringResource(R.string.report_equipments_vol_desc), icon = Icons.Default.Numbers, onClick = { onNavigate(ReportDestination.EQUIPMENTS_VOL) }) }
            item { ReportMenuCard(title = stringResource(R.string.report_operations_vol_title), description = stringResource(R.string.report_operations_vol_desc), icon = Icons.Default.History, onClick = { onNavigate(ReportDestination.OPERATIONS_VOL) }) }

            item { Text("Advanced Analytics", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp)) }
            item { ReportMenuCard(title = stringResource(R.string.report_intervals_title), description = stringResource(R.string.report_intervals_desc), icon = Icons.AutoMirrored.Filled.TrendingUp, onClick = { onNavigate(ReportDestination.INTERVALS) }) }
            item { ReportMenuCard(title = stringResource(R.string.report_heatmap_title), description = stringResource(R.string.report_heatmap_desc), icon = Icons.Default.GridView, onClick = { onNavigate(ReportDestination.HEATMAP) }) }
            item { ReportMenuCard(title = stringResource(R.string.report_benchmarking_title), description = stringResource(R.string.report_benchmarking_desc), icon = Icons.Default.Compare, onClick = { onNavigate(ReportDestination.BENCHMARKING) }) }
        }
    }
}

@Composable
fun ReportMenuCard(title: String, description: String, icon: ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            // Applicando weight(1f) qui, la colonna occuperà solo lo spazio disponibile tra le due icone
            Column(modifier = Modifier.weight(1f)) { 
                Text(text = title, style = MaterialTheme.typography.titleMedium) 
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) 
            }
            Spacer(modifier = Modifier.width(16.dp)) 
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun EquipmentChartCard(title: String? = null, chartData: Map<Int, List<ChartPoint>>, unitLabel: String, hasMixedUnits: Boolean, requestedGranularity: TimeGranularity?, effectiveGranularity: TimeGranularity?, colors: List<Color>, allStableIds: List<Int>, selectedIds: List<Int>, equipments: List<Equipment>) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            val unitsPart = if (unitLabel.isNotBlank()) unitLabel.split(", ").joinToString("") { "[$it]" } else ""
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(text = "$unitsPart ${title ?: stringResource(R.string.report_value_over_time)}", style = MaterialTheme.typography.titleMedium) ; if (hasMixedUnits) Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.extraSmall) { Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp).padding(2.dp), tint = MaterialTheme.colorScheme.onErrorContainer) } }
            if (hasMixedUnits) Text(text = stringResource(R.string.mixed_units_warning), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
            Spacer(modifier = Modifier.height(16.dp)) ; MultiLineChart(chartDataMap = chartData, requestedGranularity = requestedGranularity, effectiveGranularity = effectiveGranularity, finalColors = colors, allStableIds = allStableIds) ; if (chartData.any { it.value.isNotEmpty() }) { Spacer(modifier = Modifier.height(16.dp)); ChartLegend(items = selectedIds.mapNotNull { id -> equipments.find { it.id == id }?.description?.let { id to it } }, colors = colors, allStableIds = allStableIds) }
        }
    }
}

@Composable
fun HeatmapCard(title: String, data: List<HeatmapPoint>, baseColor: Color) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            if (data.isEmpty()) NoDataPlaceholder() else {
                val maxVal = data.maxOfOrNull { it.value } ?: 1 ; val days = listOf("S", "M", "T", "W", "T", "F", "S")
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { days.forEach { day -> Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) { Text(day, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp) } } }
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (month in 0..11) { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { for (dow in 1..7) { val point = data.find { it.x == dow && it.y == month } ; val alpha = if (point != null) (point.value.toFloat() / maxVal).coerceAtLeast(0.1f) else 0.05f ; Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)).background(baseColor.copy(alpha = alpha))) } } }
                    }
                }
            }
        }
    }
}

@Composable
fun BenchmarkCard(title: String? = null, data: List<BenchmarkData>, colors: List<Color>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(data) { if (data.isNotEmpty()) { modelProducer.runTransaction { columnSeries { series(data.map { it.totalValue }) ; series(data.map { it.avgInterval }) } } } }
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp)) {
            Text(title ?: stringResource(R.string.report_benchmarking_title), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            if (data.isEmpty()) NoDataPlaceholder() else {
                CartesianChartHost(chart = rememberCartesianChart(rememberColumnCartesianLayer(columnProvider = ColumnCartesianLayer.ColumnProvider.series(colors.take(2).map { color -> LineComponent(color = color.toArgb(), thicknessDp = 16f) })), startAxis = VerticalAxis.rememberStart(), bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = CartesianValueFormatter { _, value, _ -> data.getOrNull(value.toInt())?.equipmentName ?: "" })), modelProducer = modelProducer, modifier = Modifier.fillMaxWidth().height(250.dp) )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(12.dp).background(colors[0])); Spacer(modifier = Modifier.width(4.dp)); Text(stringResource(R.string.report_benchmark_total), style = MaterialTheme.typography.labelSmall) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(12.dp).background(colors[1])); Spacer(modifier = Modifier.width(4.dp)); Text(stringResource(R.string.report_benchmark_avg_interval), style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
    }
}

@Composable
fun FilterManagementRow(savedFilters: List<ReportFilter>, activeFilterName: String?, isDirty: Boolean, onSaveNew: (String) -> Unit, onOverwrite: () -> Unit, onApply: (ReportFilter) -> Unit, onDelete: (Int) -> Unit) {
    var showSaveDialog by remember { mutableStateOf(false) } ; var filterName by remember { mutableStateOf("") } ; var showLoadMenu by remember { mutableStateOf(false) }
    if (showSaveDialog) { AlertDialog(onDismissRequest = { showSaveDialog = false }, title = { Text(stringResource(R.string.save_filter)) }, text = { OutlinedTextField(value = filterName, onValueChange = { filterName = it }, label = { Text(stringResource(R.string.filter_name)) }, modifier = Modifier.fillMaxWidth(), singleLine = true) }, confirmButton = { TextButton(onClick = { if (filterName.isNotBlank()) { onSaveNew(filterName); filterName = ""; showSaveDialog = false } }) { Text(stringResource(R.string.button_add)) } }, dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text(stringResource(R.string.button_cancel)) } }) }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.FilterAlt, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = activeFilterName ?: stringResource(R.string.custom_filter), style = MaterialTheme.typography.labelSmall, color = if (isDirty) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary, fontStyle = if (isDirty) FontStyle.Italic else FontStyle.Normal, fontWeight = if (activeFilterName != null) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Box {
            IconButton(onClick = { showLoadMenu = true }, enabled = savedFilters.isNotEmpty()) { Icon(imageVector = Icons.Default.FolderOpen, contentDescription = stringResource(R.string.load_filter), tint = if (savedFilters.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)) }
            DropdownMenu(expanded = showLoadMenu, onDismissRequest = { showLoadMenu = false }) { savedFilters.forEach { filter -> DropdownMenuItem(text = { Text(filter.name ?: "", style = MaterialTheme.typography.bodyMedium) }, onClick = { onApply(filter); showLoadMenu = false }, trailingIcon = { IconButton(onClick = { onDelete(filter.id) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error) } }) } }
        }
        if (activeFilterName != null && isDirty) { IconButton(onClick = onOverwrite) { Icon(imageVector = Icons.Default.Save, contentDescription = stringResource(R.string.overwrite_filter), tint = MaterialTheme.colorScheme.secondary) } }
        IconButton(onClick = { showSaveDialog = true }) { Icon(imageVector = Icons.Default.PostAdd, contentDescription = stringResource(R.string.save_new_filter), tint = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
fun PieChartCard(title: String, distribution: List<PieChartPoint>) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
            if (distribution.isEmpty()) NoDataPlaceholder() else {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    PieChart(modifier = Modifier.size(140.dp), points = distribution)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { distribution.take(5).forEach { point -> Row(verticalAlignment = Alignment.Top) { val color = point.color?.let { Color(it.toColorInt()) } ?: Color.Gray ; Box(modifier = Modifier.padding(top = 4.dp).size(12.dp).clip(CircleShape).background(color)) ; Spacer(modifier = Modifier.width(8.dp)); Text(text = stringResource(R.string.occurrences_format, point.label, point.value.toInt()), style = MaterialTheme.typography.labelSmall) } } ; if (distribution.size > 5) Text(text = "...", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 20.dp)) }
                }
            }
        }
    }
}

@Composable
fun PieChart(modifier: Modifier = Modifier, points: List<PieChartPoint>) {
    val total = points.sumOf { it.value.toDouble() }.toFloat()
    Canvas(modifier = modifier) {
        var startAngle = -90f
        points.forEach { point -> 
            val sweepAngle = (point.value / total) * 360f ; 
            val color = point.color?.let { Color(it.toColorInt()) } ?: Color.Gray
            drawArc(color = color, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = true, size = Size(size.minDimension, size.minDimension), topLeft = Offset((size.width - size.minDimension) / 2, (size.height - size.minDimension) / 2)) ; 
            drawArc(color = Color.White.copy(alpha = 0.3f), startAngle = startAngle, sweepAngle = sweepAngle, useCenter = true, style = Stroke(width = 1.dp.toPx()), size = Size(size.minDimension, size.minDimension), topLeft = Offset((size.width - size.minDimension) / 2, (size.height - size.minDimension) / 2)) ; 
            startAngle += sweepAngle 
        }
    }
}

@Composable
fun NoDataPlaceholder() { Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { Text(text = stringResource(R.string.report_no_data), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable
fun MultiLineChart(chartDataMap: Map<Int, List<ChartPoint>>, requestedGranularity: TimeGranularity?, effectiveGranularity: TimeGranularity?, finalColors: List<Color>, allStableIds: List<Int>) {
    val modelProducer = remember { CartesianChartModelProducer() } ; val dataWithPoints = remember(chartDataMap) { chartDataMap.filter { it.value.isNotEmpty() } }
    val lineStyles = remember(dataWithPoints, allStableIds, finalColors) { 
        dataWithPoints.keys.map { id -> 
            val colorIndex = allStableIds.indexOf(id) ; 
            val color = finalColors[if (finalColors.isNotEmpty()) colorIndex % finalColors.size else 0]
            LineCartesianLayer.Line(
                fill = LineCartesianLayer.LineFill.single(fill = Fill(color.toArgb())),
                pointProvider = LineCartesianLayer.PointProvider.single(
                    LineCartesianLayer.Point(
                        ShapeComponent(
                            shape = CorneredShape.Pill,
                            color = color.toArgb(),
                        ),
                        sizeDp = 4f
                    )
                )
            )
        } 
    }
    val dateFormat = remember(effectiveGranularity) { when (effectiveGranularity) { null -> SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()) ; TimeGranularity.HOURS -> SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) ; TimeGranularity.DAYS, TimeGranularity.WEEKS -> SimpleDateFormat("dd/MM", Locale.getDefault()) ; TimeGranularity.MONTHS -> SimpleDateFormat("MM/yy", Locale.getDefault()) ; TimeGranularity.YEARS -> SimpleDateFormat("yyyy", Locale.getDefault()) } }
    LaunchedEffect(dataWithPoints, effectiveGranularity) { if (dataWithPoints.isNotEmpty()) { modelProducer.runTransaction { lineSeries { dataWithPoints.values.forEach { points -> series(x = points.map { it.date }, y = points.map { it.kilometers }) } } } } }
    if (dataWithPoints.isNotEmpty()) { key(requestedGranularity, effectiveGranularity, dataWithPoints.keys, finalColors) { 
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(lineProvider = LineCartesianLayer.LineProvider.series(lineStyles)), 
                startAxis = VerticalAxis.rememberStart(), 
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = CartesianValueFormatter { _, value, _ -> dateFormat.format(Date(value.toLong())) },
                    label = rememberAxisLabelComponent(color = if (requestedGranularity != null && requestedGranularity != effectiveGranularity) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                ),
            ), 
            modelProducer = modelProducer, 
            modifier = Modifier.fillMaxWidth().height(250.dp)
        ) 
    } } else NoDataPlaceholder()
}

@Composable
fun ChartLegend(items: List<Pair<Int, String>>, colors: List<Color>, allStableIds: List<Int>) { 
    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start), verticalArrangement = Arrangement.spacedBy(8.dp)) { 
        items.forEach { pair -> 
            val colorIndex = allStableIds.indexOf(pair.first)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) { 
                val boxColor = if (colors.isNotEmpty()) colors[colorIndex % colors.size] else Color.Gray
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(boxColor)) ;
                Spacer(modifier = Modifier.width(6.dp)); 
                Text(text = pair.second, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp) 
            } 
        } 
    } 
}

@Composable
fun rememberChartColors(colorMode: String, customColors: List<String>): List<Color> {
    return remember(colorMode, customColors) {
        if (customColors.isNotEmpty()) {
            customColors.map { Color(it.toColorInt()) }
        } else {
            // Fallback minimo di sicurezza se la lista fosse vuota
            listOf(Color(0xFF4285F4), Color(0xFF34A853), Color(0xFFFBBC05), Color(0xFFEA4335), Color(0xFF9C27B0), Color(0xFF00BCD4))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardFilterSection(startDate: Long?, endDate: Long?, granularity: TimeGranularity?, onDateRangeSelected: (Long?, Long?) -> Unit, onGranularitySelected: (TimeGranularity?) -> Unit, onReset: () -> Unit, onRefresh: () -> Unit, enabledGranularity: Boolean = true) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) } ; var showDatePickerRange by remember { mutableStateOf(false) }
    if (showDatePickerRange) { val dateRangePickerState = rememberDateRangePickerState(initialSelectedStartDateMillis = startDate, initialSelectedEndDateMillis = endDate) ; DatePickerDialog(onDismissRequest = { showDatePickerRange = false }, confirmButton = { TextButton(onClick = { onDateRangeSelected(dateRangePickerState.selectedStartDateMillis, dateRangePickerState.selectedEndDateMillis) ; showDatePickerRange = false ; onRefresh() }) { Text(stringResource(R.string.button_ok)) } }, dismissButton = { TextButton(onClick = { showDatePickerRange = false }) { Text(stringResource(R.string.button_cancel)) } }) { DateRangePicker(state = dateRangePickerState, title = { Text(stringResource(R.string.date), modifier = Modifier.padding(16.dp)) }, showModeToggle = false, modifier = Modifier.fillMaxWidth().height(400.dp) ) } }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) { Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedButton(onClick = { showDatePickerRange = true }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp)) { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(4.dp)); Text(text = if (startDate != null && endDate != null) { "${dateFormat.format(Date(startDate))} - ${dateFormat.format(Date(endDate))}" } else { stringResource(R.string.date) }, style = MaterialTheme.typography.labelMedium) } ; IconButton(onClick = { onReset(); onRefresh() }, modifier = Modifier.size(40.dp)) { Icon(imageVector = Icons.Default.FilterAltOff, contentDescription = stringResource(R.string.filter_reset), tint = MaterialTheme.colorScheme.primary) } } ; if (enabledGranularity) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { TimeGranularity.entries.forEach { entry -> val isSelected = entry == granularity ; FilterChip(selected = isSelected, onClick = { onGranularitySelected(entry); onRefresh() }, label = { Text(text = when (entry) { TimeGranularity.HOURS -> stringResource(R.string.granularity_hours) ; TimeGranularity.DAYS -> stringResource(R.string.granularity_days) ; TimeGranularity.WEEKS -> stringResource(R.string.granularity_weeks) ; TimeGranularity.MONTHS -> stringResource(R.string.granularity_months) ; TimeGranularity.YEARS -> stringResource(R.string.granularity_years) }, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }, modifier = Modifier.weight(1f)) } } } } }
}
