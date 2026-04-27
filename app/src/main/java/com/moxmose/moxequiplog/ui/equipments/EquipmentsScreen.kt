package com.moxmose.moxequiplog.ui.equipments

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.*
import com.moxmose.moxequiplog.ui.components.DraggableLazyColumn
import com.moxmose.moxequiplog.ui.components.ImagePickerDialog
import com.moxmose.moxequiplog.ui.maintenancelog.MaintenanceLogDialog
import com.moxmose.moxequiplog.ui.maintenancelog.MaintenanceLogViewModel
import com.moxmose.moxequiplog.ui.options.EquipmentIconProvider
import com.moxmose.moxequiplog.ui.options.OptionsViewModel
import com.moxmose.moxequiplog.utils.UiConstants
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun EquipmentsScreen(
    viewModel: EquipmentsViewModel = koinViewModel(), 
    optionsViewModel: OptionsViewModel = koinViewModel(),
    logsViewModel: MaintenanceLogViewModel = koinViewModel()
) {
    val activeEquipments by viewModel.activeEquipments.collectAsState()
    val allEquipments by viewModel.allEquipments.collectAsState()
    val equipmentImages by viewModel.equipmentImages.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val defaultEquipmentId by viewModel.defaultEquipmentId.collectAsState()
    val measurementUnits by viewModel.measurementUnits.collectAsState()
    val defaultUnitId by viewModel.defaultUnitId.collectAsState()
    val equipmentStatuses by viewModel.equipmentStatuses.collectAsState()
    
    val categoryColor by viewModel.categoryColor.collectAsState()
    val categoryDefaultIcon by viewModel.categoryDefaultIcon.collectAsState()
    val categoryDefaultPhoto by viewModel.categoryDefaultPhoto.collectAsState()

    val categoriesUiState by optionsViewModel.categoriesUiState.collectAsState()
    val categoryColorsMap = remember(categoriesUiState) { categoriesUiState.associate { it.category.id to it.color } }
    val categoryDefaultIconsMap = remember(categoriesUiState) { categoriesUiState.associate { it.category.id to it.defaultIconIdentifier } }
    val categoryDefaultPhotosMap = remember(categoriesUiState) { categoriesUiState.associate { it.category.id to it.defaultPhotoUri } }

    val syncCalendarByDefault by logsViewModel.syncCalendarByDefault.collectAsState()
    val googleAccountName by logsViewModel.googleAccountName.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(key1 = true) {
        viewModel.uiEvents.collectLatest { event ->
            val message = when(event) {
                is EquipmentsViewModel.UiEvent.DescriptionInvalid -> context.getString(R.string.description_invalid)
                is EquipmentsViewModel.UiEvent.AddEquipmentFailed -> context.getString(R.string.add_equipment_failed)
                is EquipmentsViewModel.UiEvent.UpdateEquipmentFailed -> context.getString(R.string.update_equipment_failed)
                is EquipmentsViewModel.UiEvent.UpdateEquipmentsFailed -> context.getString(R.string.update_equipments_failed)
                is EquipmentsViewModel.UiEvent.DismissEquipmentFailed -> context.getString(R.string.dismiss_equipment_failed)
                is EquipmentsViewModel.UiEvent.RestoreEquipmentFailed -> context.getString(R.string.restore_equipment_failed)
                is EquipmentsViewModel.UiEvent.AddImageFailed -> context.getString(R.string.add_image_failed)
                is EquipmentsViewModel.UiEvent.RemoveImageFailed -> context.getString(R.string.remove_image_failed)
                is EquipmentsViewModel.UiEvent.UpdateImageOrderFailed -> context.getString(R.string.update_image_order_failed)
                is EquipmentsViewModel.UiEvent.ToggleImageVisibilityFailed -> context.getString(R.string.toggle_image_visibility_failed)
                is EquipmentsViewModel.UiEvent.DatabaseCheckFailed -> context.getString(R.string.database_check_failed)
                is EquipmentsViewModel.UiEvent.PhotoUriInvalid -> context.getString(R.string.photo_uri_invalid)
                is EquipmentsViewModel.UiEvent.SetDefaultFailed -> context.getString(R.string.error_unknown)
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    var showDismissed by rememberSaveable { mutableStateOf(false) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    // Dialog state for "Operation Shortcut"
    var selectedPredictionForAdd by remember { mutableStateOf<Pair<Int, OperationStatus>?>(null) }
    var selectedPlannedForEdit by remember { mutableStateOf<Pair<Int, OperationStatus>?>(null) }

    if (selectedPredictionForAdd != null) {
        val (eqId, opStatus) = selectedPredictionForAdd!!
        val operationTypes by logsViewModel.allOperationTypes.collectAsState()
        MaintenanceLogDialog(
            equipments = activeEquipments,
            operationTypes = operationTypes.filter { !it.dismissed },
            measurementUnits = measurementUnits,
            onDismissRequest = { selectedPredictionForAdd = null },
            onConfirm = { log ->
                logsViewModel.addLog(log.equipmentId, log.operationTypeId, log.notes, log.value, log.date, log.color)
                selectedPredictionForAdd = null
            },
            defaultEquipmentId = eqId,
            defaultOperationTypeId = opStatus.operation.id,
            initialDate = opStatus.nextPresumedDate ?: System.currentTimeMillis(),
            equipmentCategoryColor = categoryColor,
            operationCategoryColor = categoryColorsMap[Category.OPERATION],
            syncCalendarByDefault = syncCalendarByDefault,
            googleAccountName = googleAccountName
        )
    }

    if (selectedPlannedForEdit != null) {
        // Logic for opening MaintenanceLogDialog in edit mode for the specific reminder
        // For now, simple add log shortcut if edit not fully available here
        val (eqId, opStatus) = selectedPlannedForEdit!!
        val operationTypes by logsViewModel.allOperationTypes.collectAsState()
        MaintenanceLogDialog(
            equipments = activeEquipments,
            operationTypes = operationTypes.filter { !it.dismissed },
            measurementUnits = measurementUnits,
            onDismissRequest = { selectedPlannedForEdit = null },
            onConfirm = { log ->
                logsViewModel.addLog(log.equipmentId, log.operationTypeId, log.notes, log.value, log.date, log.color)
                selectedPlannedForEdit = null
            },
            defaultEquipmentId = eqId,
            defaultOperationTypeId = opStatus.operation.id,
            initialDate = opStatus.nextPresumedDate ?: System.currentTimeMillis(),
            isEditMode = true, // Visual hint
            equipmentCategoryColor = categoryColor,
            operationCategoryColor = categoryColorsMap[Category.OPERATION],
            syncCalendarByDefault = syncCalendarByDefault,
            googleAccountName = googleAccountName
        )
    }

    val equipmentsToShow = if (showDismissed) allEquipments else activeEquipments

    EquipmentsScreenContent(
        equipments = equipmentsToShow,
        equipmentImages = equipmentImages,
        allCategories = allCategories,
        measurementUnits = measurementUnits,
        defaultUnitId = defaultUnitId,
        defaultIcon = categoryDefaultIcon,
        defaultPhotoUri = categoryDefaultPhoto,
        equipmentCategoryColor = categoryColor,
        onAddEquipment = viewModel::addEquipment,
        onUpdateEquipments = viewModel::updateEquipments,
        onUpdateEquipment = viewModel::updateEquipment,
        onDismissEquipment = viewModel::dismissEquipment,
        onRestoreEquipment = viewModel::restoreEquipment,
        showDismissed = showDismissed,
        onToggleShowDismissed = { showDismissed = !showDismissed },
        showAddDialog = showAddDialog,
        onShowAddDialogChange = { showAddDialog = it },
        onAddImage = viewModel::addImage,
        onToggleImageVisibility = viewModel::toggleImageVisibility,
        snackbarHostState = snackbarHostState,
        defaultEquipmentId = defaultEquipmentId,
        onToggleDefault = viewModel::toggleDefaultEquipment,
        categoryColors = categoryColorsMap,
        categoryDefaultIcons = categoryDefaultIconsMap,
        categoryDefaultPhotos = categoryDefaultPhotosMap,
        equipmentStatuses = equipmentStatuses,
        onPredictionAction = { eqId, status -> selectedPredictionForAdd = eqId to status },
        onPlannedAction = { eqId, status -> selectedPlannedForEdit = eqId to status }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentsScreenContent(
    equipments: List<Equipment>,
    equipmentImages: List<Image>,
    allCategories: List<Category>,
    measurementUnits: List<MeasurementUnit>,
    defaultUnitId: Int?,
    defaultIcon: String?,
    defaultPhotoUri: String?,
    equipmentCategoryColor: String?,
    showDismissed: Boolean,
    onToggleShowDismissed: () -> Unit,
    showAddDialog: Boolean,
    onShowAddDialogChange: (Boolean) -> Unit,
    onAddEquipment: (String, ImageIdentifier?, Int, Boolean, Int, TimeGranularity, Double?, TimeGranularity, Int, TimeGranularity) -> Unit,
    onUpdateEquipments: (List<Equipment>) -> Unit,
    onUpdateEquipment: (Equipment) -> Unit,
    onDismissEquipment: (Equipment) -> Unit,
    onRestoreEquipment: (Equipment) -> Unit,
    onAddImage: (ImageIdentifier, String) -> Unit,
    onToggleImageVisibility: (Image) -> Unit,
    snackbarHostState: SnackbarHostState,
    defaultEquipmentId: Int?,
    onToggleDefault: (Int) -> Unit,
    categoryColors: Map<String, String>,
    categoryDefaultIcons: Map<String, String?>,
    categoryDefaultPhotos: Map<String, String?>,
    equipmentStatuses: Map<Int, EquipmentStatus> = emptyMap(),
    onPredictionAction: (Int, OperationStatus) -> Unit,
    onPlannedAction: (Int, OperationStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val equipmentsState = remember(equipments) { equipments.toMutableStateList() }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(onClick = { onShowAddDialogChange(true) }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_equipment))
                }
                Spacer(modifier = Modifier.padding(8.dp))
                FloatingActionButton(
                    onClick = onToggleShowDismissed,
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(
                        imageVector = if (showDismissed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (showDismissed) stringResource(R.string.hide_dismissed) else stringResource(R.string.show_dismissed)
                    )
                }
            }
        }
    ) { paddingValues ->
        if (showAddDialog) {
            AddEquipmentDialog(
                defaultIcon = defaultIcon,
                defaultPhotoUri = defaultPhotoUri,
                imageLibrary = equipmentImages,
                categories = allCategories,
                measurementUnits = measurementUnits,
                defaultUnitId = defaultUnitId,
                equipmentCategoryColor = equipmentCategoryColor,
                categoryColors = categoryColors,
                categoryDefaultIcons = categoryDefaultIcons,
                categoryDefaultPhotos = categoryDefaultPhotos,
                onDismissRequest = { onShowAddDialogChange(false) },
                onConfirm = { desc, identifier, unitId, isResettable, window, windowUnit, avgValue, avgUnit, horizon, horizonUnit ->
                    onAddEquipment(desc, identifier, unitId, isResettable, window, windowUnit, avgValue, avgUnit, horizon, horizonUnit)
                    onShowAddDialogChange(false)
                },
                onAddImage = onAddImage,
                onToggleImageVisibility = onToggleImageVisibility
            )
        }

        Column(Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.set_as_default_instruction),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.hold_and_drag_to_reorder),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            DraggableLazyColumn(
                items = equipmentsState,
                key = { _, equipment -> equipment.id },
                onMove = { from, to ->
                    equipmentsState.add(to, equipmentsState.removeAt(from))
                },
                onDrop = {
                    val reorderedEquipments = equipmentsState.mapIndexed { index, equipment ->
                        equipment.copy(displayOrder = index)
                    }
                    onUpdateEquipments(reorderedEquipments)
                },
                modifier = Modifier.fillMaxSize(),
                itemContent = { _, equipment ->
                    EquipmentCard(
                        equipment = equipment,
                        equipmentImages = equipmentImages,
                        allCategories = allCategories,
                        measurementUnits = measurementUnits,
                        onUpdateEquipment = onUpdateEquipment,
                        onDismissEquipment = onDismissEquipment,
                        onRestoreEquipment = onRestoreEquipment,
                        onAddImage = onAddImage,
                        onToggleImageVisibility = onToggleImageVisibility,
                        equipmentCategoryColor = equipmentCategoryColor,
                        isDefault = equipment.id == defaultEquipmentId,
                        onToggleDefault = { onToggleDefault(equipment.id) },
                        status = equipmentStatuses[equipment.id],
                        onPredictionAction = { onPredictionAction(equipment.id, it) },
                        onPlannedAction = { onPlannedAction(equipment.id, it) },
                        categoryColors = categoryColors,
                        categoryDefaultIcons = categoryDefaultIcons,
                        categoryDefaultPhotos = categoryDefaultPhotos
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEquipmentDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String, ImageIdentifier?, Int, Boolean, Int, TimeGranularity, Double?, TimeGranularity, Int, TimeGranularity) -> Unit,
    defaultIcon: String?,
    defaultPhotoUri: String?,
    imageLibrary: List<Image>,
    categories: List<Category>,
    measurementUnits: List<MeasurementUnit>,
    defaultUnitId: Int?,
    equipmentCategoryColor: String?,
    categoryColors: Map<String, String>,
    categoryDefaultIcons: Map<String, String?>,
    categoryDefaultPhotos: Map<String, String?>,
    onAddImage: (ImageIdentifier, String) -> Unit,
    onToggleImageVisibility: (Image) -> Unit
) {
    var description by rememberSaveable { mutableStateOf("") }
    var photoUri by rememberSaveable { mutableStateOf<String?>(null) }
    var iconId by rememberSaveable { mutableStateOf<String?>(null) }
    var unitId by rememberSaveable(defaultUnitId) { mutableIntStateOf(defaultUnitId ?: 1) }
    var isResettable by rememberSaveable { mutableStateOf(false) }
    
    // Predictive Settings
    var usageWindow by rememberSaveable { mutableIntStateOf(30) }
    var usageWindowUnit by rememberSaveable { mutableStateOf(TimeGranularity.DAYS) }
    var manualAverageValue by rememberSaveable { mutableStateOf<Double?>(null) }
    var manualAverageValueStr by rememberSaveable { mutableStateOf("") }
    var manualAverageUnit by rememberSaveable { mutableStateOf(TimeGranularity.DAYS) }
    var visibilityHorizon by rememberSaveable { mutableIntStateOf(30) }
    var visibilityHorizonUnit by rememberSaveable { mutableStateOf(TimeGranularity.DAYS) }

    var isPristine by rememberSaveable { mutableStateOf(true) }
    var showImageSelectorDialog by remember { mutableStateOf(false) }

    val selectedUnit = measurementUnits.find { it.id == unitId }
    val unitLabel = selectedUnit?.label ?: ""

    if (isPristine && (defaultIcon != null || defaultPhotoUri != null)) {
        LaunchedEffect(defaultIcon, defaultPhotoUri) {
            iconId = defaultIcon
            photoUri = defaultPhotoUri
        }
    }

    if (showImageSelectorDialog) {
        ImagePickerDialog(
            onDismissRequest = { showImageSelectorDialog = false },
            photoUri = photoUri,
            iconIdentifier = iconId,
            onImageSelected = { (newIconId, newPhotoUri) ->
                isPristine = false
                iconId = newIconId
                photoUri = newPhotoUri
                showImageSelectorDialog = false
            },
            imageLibrary = imageLibrary,
            categories = categories,
            categoryColors = categoryColors,
            categoryDefaultIcons = categoryDefaultIcons,
            categoryDefaultPhotos = categoryDefaultPhotos,
            onAddImage = { uri, category -> onAddImage(ImageIdentifier.Photo(uri), category) },
            onRemoveImage = null,
            onUpdateImageOrder = null,
            onToggleImageVisibility = { uri, category -> imageLibrary.find { it.uri == uri && it.category == category }?.let { onToggleImageVisibility(it) } },
            onSetDefaultInCategory = null,
            isPhotoUsed = null,
            isPrefsMode = false,
            forcedCategory = Category.EQUIPMENT
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(R.string.add_a_new_equipment), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val borderColor = remember(equipmentCategoryColor, primaryColor) {
                        try {
                            equipmentCategoryColor?.toColorInt()?.let { Color(it) } ?: primaryColor
                        } catch (_: Exception) { primaryColor }
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .border(2.dp, borderColor, CircleShape)
                            .clickable { showImageSelectorDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUri != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(photoUri).crossfade(true).build(),
                                contentDescription = stringResource(R.string.equipment_photo),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            val icon = EquipmentIconProvider.getIcon(iconId)
                            Icon(
                                imageVector = icon,
                                contentDescription = stringResource(R.string.equipment_photo),
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    OutlinedTextField(
                        value = description,
                        onValueChange = { if (it.length <= 50) description = it },
                        label = { Text(stringResource(R.string.equipment_description)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                UnitSelector(
                    measurementUnits = measurementUnits,
                    selectedUnitId = unitId,
                    onUnitSelected = { unitId = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { isResettable = !isResettable },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(checked = isResettable, onCheckedChange = { isResettable = it })
                    Text(text = stringResource(R.string.equipment_is_resettable), style = MaterialTheme.typography.bodyMedium)
                }

                HorizontalDivider()

                Text(
                    text = stringResource(R.string.predictive_maintenance_settings),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                // Trend Window Section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = usageWindow.toString(),
                            onValueChange = { input -> input.toIntOrNull()?.let { if (it in 1..999) usageWindow = it } },
                            label = { Text("Window Value") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        TimeGranularitySelector(selected = usageWindowUnit, onSelected = { usageWindowUnit = it }, label = "Of last", modifier = Modifier.weight(1.2f))
                    }
                    Text(text = "Time window used to analyze history and calculate trends", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Manual Average Section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = manualAverageValueStr,
                            onValueChange = { input ->
                                val filtered = input.replace(',', '.')
                                if (filtered.isEmpty() || filtered.toDoubleOrNull() != null) {
                                    manualAverageValueStr = filtered
                                    manualAverageValue = filtered.toDoubleOrNull()
                                }
                            },
                            label = { Text(if (unitLabel.isNotBlank()) "Usage ($unitLabel)" else "Usage") },
                            placeholder = { Text("Fallback") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        TimeGranularitySelector(selected = manualAverageUnit, onSelected = { manualAverageUnit = it }, label = "Every", modifier = Modifier.weight(1.2f))
                    }
                    Text(text = "Optional: expected usage when history is missing (fallback)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Visibility Horizon Row
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = visibilityHorizon.toString(),
                            onValueChange = { input -> input.toIntOrNull()?.let { if (it in 1..999) visibilityHorizon = it } },
                            label = { Text("Event Horizon") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        TimeGranularitySelector(selected = visibilityHorizonUnit, onSelected = { visibilityHorizonUnit = it }, label = "Future span", modifier = Modifier.weight(1.2f))
                    }
                    Text(text = "How far into the future to show upcoming maintenance items", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    val identifier = when {
                        photoUri != null -> ImageIdentifier.Photo(photoUri!!)
                        iconId != null -> ImageIdentifier.Icon(iconId!!)
                        else -> null
                    }
                    onConfirm(description, identifier, unitId, isResettable, usageWindow, usageWindowUnit, manualAverageValue, manualAverageUnit, visibilityHorizon, visibilityHorizonUnit)
                }
            ) { Text(stringResource(R.string.button_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.button_cancel)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeGranularitySelector(
    selected: TimeGranularity,
    onSelected: (TimeGranularity) -> Unit,
    label: String? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = formatTimeGranularity(selected),
            onValueChange = {},
            readOnly = true,
            label = label?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable),
            textStyle = MaterialTheme.typography.bodyMedium
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TimeGranularity.entries.forEach { entry ->
                DropdownMenuItem(text = { Text(formatTimeGranularity(entry)) }, onClick = { onSelected(entry); expanded = false })
            }
        }
    }
}

fun formatTimeGranularity(granularity: TimeGranularity): String {
    return when (granularity) {
        TimeGranularity.MINUTES_5 -> "5 Minutes"
        TimeGranularity.MINUTES_15 -> "15 Minutes"
        else -> granularity.name.lowercase().replaceFirstChar { it.titlecase() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitSelector(
    measurementUnits: List<MeasurementUnit>,
    selectedUnitId: Int,
    onUnitSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedUnit = measurementUnits.find { it.id == selectedUnitId }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = if (selectedUnit != null) {
                if (selectedUnit.description.isNotBlank()) "${selectedUnit.label} - ${selectedUnit.description}"
                else selectedUnit.label
            } else "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.measurement_unit)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            measurementUnits.forEach { unit ->
                DropdownMenuItem(
                    text = { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text(unit.label, fontWeight = FontWeight.Bold); if (unit.description.isNotBlank()) Text(unit.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                    onClick = { onUnitSelected(unit.id); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEquipmentDialog(
    equipment: Equipment,
    onDismissRequest: () -> Unit,
    onConfirm: (Equipment) -> Unit,
    imageLibrary: List<Image>,
    categories: List<Category>,
    measurementUnits: List<MeasurementUnit>,
    equipmentCategoryColor: String?,
    categoryColors: Map<String, String>,
    categoryDefaultIcons: Map<String, String?>,
    categoryDefaultPhotos: Map<String, String?>,
    onAddImage: (ImageIdentifier, String) -> Unit,
    onToggleImageVisibility: (Image) -> Unit
) {
    var description by rememberSaveable { mutableStateOf(equipment.description) }
    var photoUri by rememberSaveable { mutableStateOf(equipment.photoUri) }
    var iconId by rememberSaveable { mutableStateOf(equipment.iconIdentifier) }
    var unitId by rememberSaveable { mutableIntStateOf(equipment.unitId) }
    var isResettable by rememberSaveable { mutableStateOf(equipment.isResettable) }
    
    // Predictive Settings
    var usageWindow by rememberSaveable { mutableIntStateOf(equipment.usageWindow) }
    var usageWindowUnit by rememberSaveable { mutableStateOf(equipment.usageWindowUnit) }
    var manualAverageValue by rememberSaveable { mutableStateOf(equipment.manualAverageValue) }
    var manualAverageValueStr by rememberSaveable { mutableStateOf(equipment.manualAverageValue?.toString() ?: "") }
    var manualAverageUnit by rememberSaveable { mutableStateOf(equipment.manualAverageUnit) }
    var visibilityHorizon by rememberSaveable { mutableIntStateOf(equipment.visibilityHorizon) }
    var visibilityHorizonUnit by rememberSaveable { mutableStateOf(equipment.visibilityHorizonUnit) }
    
    var showImageSelectorDialog by remember { mutableStateOf(false) }

    val selectedUnit = measurementUnits.find { it.id == unitId }
    val unitLabel = selectedUnit?.label ?: ""

    if (showImageSelectorDialog) {
        ImagePickerDialog(
            onDismissRequest = { showImageSelectorDialog = false },
            photoUri = photoUri,
            iconIdentifier = iconId,
            onImageSelected = { (newIconId, newPhotoUri) ->
                iconId = newIconId
                photoUri = newPhotoUri
                showImageSelectorDialog = false
            },
            imageLibrary = imageLibrary,
            categories = categories,
            categoryColors = categoryColors,
            categoryDefaultIcons = categoryDefaultIcons,
            categoryDefaultPhotos = categoryDefaultPhotos,
            onAddImage = { uri, category -> onAddImage(ImageIdentifier.Photo(uri), category) },
            onRemoveImage = null,
            onUpdateImageOrder = null,
            onToggleImageVisibility = { uri, category -> imageLibrary.find { it.uri == uri && it.category == category }?.let { onToggleImageVisibility(it) } },
            onSetDefaultInCategory = null,
            isPhotoUsed = null,
            isPrefsMode = false,
            forcedCategory = Category.EQUIPMENT
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(R.string.edit_equipment), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val borderColor = remember(equipmentCategoryColor, primaryColor) {
                        try {
                            equipmentCategoryColor?.toColorInt()?.let { Color(it) } ?: primaryColor
                        } catch (_: Exception) { primaryColor }
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .border(2.dp, borderColor, CircleShape)
                            .clickable { showImageSelectorDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUri != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(photoUri).crossfade(true).build(),
                                contentDescription = stringResource(R.string.equipment_photo),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            val icon = EquipmentIconProvider.getIcon(iconId)
                            Icon(
                                imageVector = icon,
                                contentDescription = stringResource(R.string.equipment_photo),
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    OutlinedTextField(
                        value = description,
                        onValueChange = { if (it.length <= 50) description = it },
                        label = { Text(stringResource(R.string.equipment_description)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                UnitSelector(
                    measurementUnits = measurementUnits,
                    selectedUnitId = unitId,
                    onUnitSelected = { unitId = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { isResettable = !isResettable },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(checked = isResettable, onCheckedChange = { isResettable = it })
                    Text(text = stringResource(R.string.equipment_is_resettable), style = MaterialTheme.typography.bodyMedium)
                }

                HorizontalDivider()

                Text(
                    text = stringResource(R.string.predictive_maintenance_settings),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                // Trend Window Section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = usageWindow.toString(),
                            onValueChange = { input ->
                                input.toIntOrNull()?.let { if (it in 1..999) usageWindow = it }
                            },
                            label = { Text("Window Value") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        
                        TimeGranularitySelector(
                            selected = usageWindowUnit,
                            onSelected = { usageWindowUnit = it },
                            label = "Of last",
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                    Text(
                        text = "Time window used to analyze history and calculate trends",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                // Manual Average Section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualAverageValueStr,
                            onValueChange = { input ->
                                if (input.isEmpty()) {
                                    manualAverageValueStr = ""
                                    manualAverageValue = null
                                } else {
                                    val filtered = input.replace(',', '.')
                                    if (filtered.toDoubleOrNull() != null) {
                                        manualAverageValueStr = filtered
                                        manualAverageValue = filtered.toDoubleOrNull()
                                    }
                                }
                            },
                            label = { Text(if (unitLabel.isNotBlank()) "Usage ($unitLabel)" else "Usage") },
                            placeholder = { Text("Fallback") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        
                        TimeGranularitySelector(
                            selected = manualAverageUnit,
                            onSelected = { manualAverageUnit = it },
                            label = "Every",
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                    Text(
                        text = "Optional: expected usage when history is missing (fallback)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                // Visibility Horizon Section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = visibilityHorizon.toString(),
                            onValueChange = { input -> input.toIntOrNull()?.let { if (it in 1..999) visibilityHorizon = it } },
                            label = { Text("Event Horizon") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        TimeGranularitySelector(selected = visibilityHorizonUnit, onSelected = { visibilityHorizonUnit = it }, label = "Future span", modifier = Modifier.weight(1.2f))
                    }
                    Text(text = "How far into the future to show upcoming maintenance items", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        equipment.copy(
                            description = description,
                            photoUri = photoUri,
                            iconIdentifier = iconId,
                            unitId = unitId,
                            isResettable = isResettable,
                            usageWindow = usageWindow,
                            usageWindowUnit = usageWindowUnit,
                            manualAverageValue = manualAverageValue,
                            manualAverageUnit = manualAverageUnit,
                            visibilityHorizon = visibilityHorizon,
                            visibilityHorizonUnit = visibilityHorizonUnit
                        )
                    )
                }
            ) { Text(stringResource(R.string.save_equipment)) }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.button_cancel)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentCard(
    equipment: Equipment,
    equipmentImages: List<Image>,
    allCategories: List<Category>,
    measurementUnits: List<MeasurementUnit>,
    onUpdateEquipment: (Equipment) -> Unit,
    onDismissEquipment: (Equipment) -> Unit,
    onRestoreEquipment: (Equipment) -> Unit,
    onAddImage: (ImageIdentifier, String) -> Unit,
    onToggleImageVisibility: (Image) -> Unit,
    equipmentCategoryColor: String?,
    isDefault: Boolean,
    onToggleDefault: () -> Unit,
    status: EquipmentStatus? = null,
    onPredictionAction: (OperationStatus) -> Unit,
    onPlannedAction: (OperationStatus) -> Unit,
    categoryColors: Map<String, String>,
    categoryDefaultIcons: Map<String, String?>,
    categoryDefaultPhotos: Map<String, String?>,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    
    var editedDescription by remember(equipment.description) { mutableStateOf(equipment.description) }
    var editedUnitId by remember(equipment.unitId) { mutableIntStateOf(equipment.unitId) }
    var editedIsResettable by remember(equipment.isResettable) { mutableStateOf(equipment.isResettable) }
    
    // Predictive Settings
    var editedUsageWindow by remember(equipment.usageWindow) { mutableIntStateOf(equipment.usageWindow) }
    var editedUsageWindowUnit by remember(equipment.usageWindowUnit) { mutableStateOf(equipment.usageWindowUnit) }
    var editedManualAverageValue by remember(equipment.manualAverageValue) { mutableStateOf(equipment.manualAverageValue) }
    var editedManualAverageValueStr by remember(equipment.manualAverageValue) { mutableStateOf(equipment.manualAverageValue?.toString() ?: "") }
    var editedManualAverageUnit by remember(equipment.manualAverageUnit) { mutableStateOf(equipment.manualAverageUnit) }
    var editedVisibilityHorizon by remember(equipment.visibilityHorizon) { mutableIntStateOf(equipment.visibilityHorizon) }
    var editedVisibilityHorizonUnit by remember(equipment.visibilityHorizonUnit) { mutableStateOf(equipment.visibilityHorizonUnit) }
    
    var showFullImageDialog by remember { mutableStateOf<String?>(null) }
    var showNoPictureDialog by remember { mutableStateOf(false) }
    var showImageSelectorDialog by remember { mutableStateOf(false) }

    val unit = measurementUnits.find { it.id == editedUnitId }
    val unitLabel = unit?.label ?: ""
    val decimalPlaces = unit?.decimalPlaces ?: 0
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val equipmentColor = remember(equipmentCategoryColor, primaryColor) {
        try { equipmentCategoryColor?.toColorInt()?.let { Color(it) } ?: primaryColor } catch (_: Exception) { primaryColor }
    }

    Box(contentAlignment = Alignment.BottomEnd) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize()
                .graphicsLayer(alpha = if (equipment.dismissed) 0.5f else 1f)
                .then(if (isDefault) Modifier.border(3.dp, equipmentColor, MaterialTheme.shapes.medium) else Modifier)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { if (!isEditing) isExpanded = !isExpanded }),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .border(2.dp, equipmentColor, CircleShape)
                            .clickable {
                                if (isEditing) showImageSelectorDialog = true
                                else if (equipment.photoUri != null) showFullImageDialog = equipment.photoUri
                                else if (equipment.iconIdentifier == null) showNoPictureDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (equipment.photoUri != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(equipment.photoUri).crossfade(true).build(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = EquipmentIconProvider.getIcon(equipment.iconIdentifier),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        if (isEditing) {
                            OutlinedTextField(
                                value = editedDescription,
                                onValueChange = { if (it.length <= 50) editedDescription = it },
                                label = { Text(stringResource(R.string.equipment_description)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = if (editedDescription.isNotBlank()) editedDescription else stringResource(R.string.id_no_description, equipment.id),
                                    modifier = Modifier.weight(1f, fill = false),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (unitLabel.isNotBlank()) {
                                    Surface(
                                        color = equipmentColor.copy(alpha = 0.2f),
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = unitLabel,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = equipmentColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            if (status != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    status.health.lastRecordedValue?.let { valStr ->
                                        Text(text = "Last: ${String.format(Locale.US, "%.${decimalPlaces}f", valStr)} $unitLabel", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    status.health.estimatedCurrentValue?.let { estStr ->
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = "Now (est): ${String.format(Locale.US, "%.${decimalPlaces}f", estStr)} $unitLabel", style = MaterialTheme.typography.labelSmall, color = equipmentColor, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        if (isEditing) {
                            IconButton(onClick = { if (equipment.dismissed) onRestoreEquipment(equipment) else onDismissEquipment(equipment) }) {
                                Icon(imageVector = if (equipment.dismissed) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                            }
                        }
                        IconButton(onClick = {
                            if (isEditing) {
                                onUpdateEquipment(
                                    equipment.copy(
                                        description = editedDescription, 
                                        unitId = editedUnitId, 
                                        isResettable = editedIsResettable,
                                        usageWindow = editedUsageWindow,
                                        usageWindowUnit = editedUsageWindowUnit,
                                        manualAverageValue = editedManualAverageValue,
                                        manualAverageUnit = editedManualAverageUnit,
                                        visibilityHorizon = editedVisibilityHorizon,
                                        visibilityHorizonUnit = editedVisibilityHorizonUnit
                                    )
                                )
                            }
                            isEditing = !isEditing
                            if (isEditing) isExpanded = true
                        }) {
                            Icon(imageVector = if (isEditing) Icons.Filled.Done else Icons.Filled.Edit, contentDescription = null)
                        }
                        IconButton(onClick = { onToggleDefault() }) {
                            Icon(imageVector = if (isDefault) Icons.Filled.Star else Icons.Filled.StarBorder, contentDescription = null, tint = if (isDefault) Color(0xFFFFB300) else LocalContentColor.current)
                        }
                        IconButton(onClick = {}) { Icon(imageVector = Icons.Filled.DragHandle, contentDescription = null) }
                    }
                }
                
                if (isEditing) {
                    Spacer(modifier = Modifier.height(8.dp))
                    UnitSelector(
                        measurementUnits = measurementUnits,
                        selectedUnitId = editedUnitId,
                        onUnitSelected = { editedUnitId = it }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { editedIsResettable = !editedIsResettable }.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(checked = editedIsResettable, onCheckedChange = { editedIsResettable = it })
                        Text(text = stringResource(R.string.equipment_is_resettable), style = MaterialTheme.typography.bodyMedium)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = stringResource(R.string.predictive_maintenance_settings),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Trend Window Section
                    Column(modifier = Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = editedUsageWindow.toString(),
                                onValueChange = { input ->
                                    input.toIntOrNull()?.let { if (it in 1..999) editedUsageWindow = it }
                                },
                                label = { Text("Window Value") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                            
                            TimeGranularitySelector(
                                selected = editedUsageWindowUnit,
                                onSelected = { editedUsageWindowUnit = it },
                                label = "Of last",
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                        Text(
                            text = "Time window used to analyze history and calculate trends",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    // Manual Average Section
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = editedManualAverageValueStr,
                                onValueChange = { input ->
                                    if (input.isEmpty()) {
                                        editedManualAverageValueStr = ""
                                        editedManualAverageValue = null
                                    } else {
                                        val filtered = input.replace(',', '.')
                                        if (filtered.toDoubleOrNull() != null) {
                                            editedManualAverageValueStr = filtered
                                            editedManualAverageValue = filtered.toDoubleOrNull()
                                        }
                                    }
                                },
                                label = { Text(if (unitLabel.isNotBlank()) "Usage ($unitLabel)" else "Usage") },
                                placeholder = { Text("Fallback") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                            
                            TimeGranularitySelector(
                                selected = editedManualAverageUnit,
                                onSelected = { editedManualAverageUnit = it },
                                label = "Every",
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                        Text(
                            text = "Optional: expected usage when history is missing (fallback)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    // Visibility Horizon Section
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = editedVisibilityHorizon.toString(),
                                onValueChange = { input -> input.toIntOrNull()?.let { if (it in 1..999) editedVisibilityHorizon = it } },
                                label = { Text("Event Horizon") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                            TimeGranularitySelector(selected = editedVisibilityHorizonUnit, onSelected = { editedVisibilityHorizonUnit = it }, label = "Future span", modifier = Modifier.weight(1.2f))
                        }
                        Text(text = "How far into the future to show upcoming maintenance items", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                }

                if (!isEditing && isExpanded && status != null && status.operationStatuses.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Upcoming Maintenance",
                        style = MaterialTheme.typography.labelSmall,
                        color = equipmentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Column(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        status.operationStatuses.sortedBy { it.nextPresumedDate ?: Long.MAX_VALUE }.take(5).forEach { opStatus ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = if (opStatus.isOverdue) Icons.Default.Warning else if (opStatus.isPlanned) Icons.Default.EventNote else Icons.Default.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (opStatus.isOverdue) MaterialTheme.colorScheme.error else if (opStatus.isPlanned) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = opStatus.operation.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = opStatus.nextPresumedDate?.let { dateFormat.format(Date(it)) } ?: "Never",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (opStatus.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { if (opStatus.isPlanned) onPlannedAction(opStatus) else onPredictionAction(opStatus) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (opStatus.isPlanned) Icons.Default.Edit else Icons.Default.Build,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = equipmentColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (isDefault) {
            Box(
                modifier = Modifier.padding(end = 4.dp, bottom = 4.dp).size(24.dp).clip(CircleShape).background(equipmentColor).border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White) }
        }
    }
}

@Composable
fun FullImageDialog(photoUri: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(R.string.button_ok))
            }
        },
        modifier = Modifier.padding(16.dp),
        text = {
            AsyncImage(
                model = photoUri,
                contentDescription = stringResource(R.string.full_size_equipment_photo),
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit
            )
        }
    )
}
