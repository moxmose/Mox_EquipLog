package com.moxmose.moxequiplog.ui.equipment

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.Equipment
import com.moxmose.moxequiplog.data.local.EquipmentDraft
import com.moxmose.moxequiplog.data.local.Image
import com.moxmose.moxequiplog.data.local.ImageIdentifier
import com.moxmose.moxequiplog.data.local.MeasurementUnit
import com.moxmose.moxequiplog.data.local.Section
import com.moxmose.moxequiplog.data.local.TimeGranularity
import com.moxmose.moxequiplog.ui.components.DraggableLazyColumn
import com.moxmose.moxequiplog.ui.components.SectionChipBar
import com.moxmose.moxequiplog.ui.equipment.components.AddEquipmentDialog
import com.moxmose.moxequiplog.ui.equipment.components.EquipmentCard
import com.moxmose.moxequiplog.ui.maintenancelog.components.MaintenanceLogDialog
import com.moxmose.moxequiplog.ui.maintenancelog.MaintenanceLogViewModel
import com.moxmose.moxequiplog.ui.options.OptionsViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EquipmentScreen(
    viewModel: EquipmentViewModel = koinViewModel(), 
    optionsViewModel: OptionsViewModel = koinViewModel(),
    logsViewModel: MaintenanceLogViewModel = koinViewModel()
) {
    val activeEquipments by viewModel.activeEquipments.collectAsState()
    val allEquipments by viewModel.allEquipments.collectAsState()
    val equipmentImages by viewModel.equipmentImages.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val allSections by viewModel.allSections.collectAsState()
    val selectedSectionId by viewModel.selectedSectionId.collectAsState()
    val showDismissedSections by viewModel.showDismissedSections.collectAsState()
    val defaultEquipmentId by viewModel.defaultEquipmentId.collectAsState()
    val measurementUnits by viewModel.measurementUnits.collectAsState()
    val defaultUnitId by viewModel.defaultUnitId.collectAsState()
    val equipmentStatuses by viewModel.equipmentStatuses.collectAsState()
    val allDrafts by viewModel.allDrafts.collectAsState()
    val addDraft by viewModel.addDraft.collectAsState()
    val allOperationTypes by logsViewModel.allOperationTypes.collectAsState()
    
    val categoryColor by viewModel.categoryColor.collectAsState()
    val categoryDefaultIcon by viewModel.categoryDefaultIcon.collectAsState()
    val categoryDefaultPhoto by viewModel.categoryDefaultPhoto.collectAsState()

    val showDismissed by viewModel.showDismissed.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val cloningEquipment by viewModel.cloningEquipment.collectAsState()
    val selectedPredictionForAdd by viewModel.selectedPredictionForAdd.collectAsState()
    val selectedPlannedForEdit by viewModel.selectedPlannedForEdit.collectAsState()
    val logAddDraft by logsViewModel.logAddDraft.collectAsState()
    val reminderAddDraft by logsViewModel.reminderAddDraft.collectAsState()

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
                is EquipmentViewModel.UiEvent.DescriptionInvalid -> context.getString(R.string.description_invalid)
                is EquipmentViewModel.UiEvent.AddEquipmentFailed -> context.getString(R.string.add_equipment_failed)
                is EquipmentViewModel.UiEvent.UpdateEquipmentFailed -> context.getString(R.string.update_equipment_failed)
                is EquipmentViewModel.UiEvent.UpdateEquipmentOrderFailed -> context.getString(R.string.update_equipment_order_failed)
                is EquipmentViewModel.UiEvent.DismissEquipmentFailed -> context.getString(R.string.dismiss_equipment_failed)
                is EquipmentViewModel.UiEvent.RestoreEquipmentFailed -> context.getString(R.string.restore_equipment_failed)
                is EquipmentViewModel.UiEvent.AddImageFailed -> context.getString(R.string.add_image_failed)
                is EquipmentViewModel.UiEvent.RemoveImageFailed -> context.getString(R.string.remove_image_failed)
                is EquipmentViewModel.UiEvent.UpdateImageOrderFailed -> context.getString(R.string.update_image_order_failed)
                is EquipmentViewModel.UiEvent.ToggleImageVisibilityFailed -> context.getString(R.string.toggle_image_visibility_failed)
                is EquipmentViewModel.UiEvent.DatabaseCheckFailed -> context.getString(R.string.database_check_failed)
                is EquipmentViewModel.UiEvent.PhotoUriInvalid -> context.getString(R.string.photo_uri_invalid)
                is EquipmentViewModel.UiEvent.SetDefaultFailed -> context.getString(R.string.error_unknown)
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    val equipmentsToShow = if (showDismissed) allEquipments else activeEquipments

    if (selectedPredictionForAdd != null) {
        val (eqId, opStatus) = selectedPredictionForAdd!!
        MaintenanceLogDialog(
            equipments = activeEquipments,
            operationTypes = allOperationTypes.filter { !it.dismissed },
            measurementUnits = measurementUnits,
            allSections = allSections,
            onDismissRequest = { viewModel.onPredictionAction(0, null) },
            onConfirm = { log ->
                val now = System.currentTimeMillis()
                if (log.date > now + 60000) { 
                    logsViewModel.addReminder(log.equipmentId, log.operationTypeId, log.date, log.value, syncCalendarByDefault)
                } else {
                    logsViewModel.addLog(
                        log.equipmentId, 
                        log.operationTypeId, 
                        log.notes, 
                        log.value, 
                        log.date, 
                        log.color, 
                        log.resetAfter,
                        log.cost,
                        log.isUnplanned
                    )
                }
                viewModel.onPredictionAction(0, null)
            },
            onSchedule = { equipmentId, opId, date, value, sync ->
                logsViewModel.addReminder(equipmentId, opId, date, value, sync)
                viewModel.onPredictionAction(0, null)
            },
            defaultEquipmentId = eqId,
            defaultOperationTypeId = opStatus.operation.id,
            initialDate = if ((opStatus.nextPresumedDate ?: 0L) > System.currentTimeMillis()) opStatus.nextPresumedDate!! else System.currentTimeMillis(),
            initialValue = equipmentStatuses[eqId]?.health?.estimatedCurrentValue?.let { String.format(Locale.US, "%.${measurementUnits.find { it.id == activeEquipments.find { e -> e.id == eqId }?.unitId }?.decimalPlaces ?: 0}f", it) } ?: "",
            initialTab = 1, // Start on "Planned" for predictions
            equipmentCategoryColor = categoryColor,
            operationCategoryColor = categoryColorsMap[Category.OPERATION],
            syncCalendarByDefault = syncCalendarByDefault,
            googleAccountName = googleAccountName,
            logDraft = logAddDraft,
            reminderDraft = reminderAddDraft,
            onUpdateLogDraft = logsViewModel::updateLogAddDraft,
            onUpdateReminderDraft = logsViewModel::updateReminderAddDraft
        )
    }

    if (selectedPlannedForEdit != null) {
        val (eqId, opStatus) = selectedPlannedForEdit!!
        MaintenanceLogDialog(
            equipments = activeEquipments,
            operationTypes = allOperationTypes.filter { !it.dismissed },
            measurementUnits = measurementUnits,
            allSections = allSections,
            onDismissRequest = { viewModel.onPlannedAction(0, null) },
            onConfirm = { log ->
                val now = System.currentTimeMillis()
                if (log.date > now + 60000) {
                    opStatus.reminderId?.let { id ->
                        logsViewModel.updateReminder(log.equipmentId, log.operationTypeId, log.date, log.value, syncCalendarByDefault, id)
                    }
                } else {
                    logsViewModel.addLog(
                        log.equipmentId, 
                        log.operationTypeId, 
                        log.notes, 
                        log.value, 
                        log.date, 
                        log.color, 
                        log.resetAfter,
                        log.cost,
                        log.isUnplanned
                    )
                }
                viewModel.onPlannedAction(0, null)
            },
            onSchedule = { equipmentId, opId, date, value, sync ->
                opStatus.reminderId?.let { id ->
                    logsViewModel.updateReminder(equipmentId, opId, date, value, sync, id)
                }
                viewModel.onPlannedAction(0, null)
            },
            onDeleteReminder = {
                // Here we would need a way to delete the reminder by ID
                viewModel.onPlannedAction(0, null)
            },
            defaultEquipmentId = eqId,
            defaultOperationTypeId = opStatus.operation.id,
            initialDate = opStatus.nextPresumedDate ?: System.currentTimeMillis(),
            initialValue = opStatus.plannedValue?.toString() ?: "",
            isEditMode = true,
            equipmentCategoryColor = categoryColor,
            operationCategoryColor = categoryColorsMap[Category.OPERATION],
            syncCalendarByDefault = syncCalendarByDefault,
            googleAccountName = googleAccountName,
            logDraft = logAddDraft,
            reminderDraft = reminderAddDraft,
            onUpdateLogDraft = logsViewModel::updateLogAddDraft,
            onUpdateReminderDraft = logsViewModel::updateReminderAddDraft
        )
    }

    EquipmentScreenContent(
        equipments = equipmentsToShow,
        equipmentImages = equipmentImages,
        allCategories = allCategories,
        allSections = allSections,
        selectedSectionId = selectedSectionId,
        onSectionSelected = viewModel::onSectionSelected,
        showDismissedSections = showDismissedSections,
        onToggleShowDismissedSections = viewModel::onToggleShowDismissedSections,
        measurementUnits = measurementUnits,
        defaultUnitId = defaultUnitId,
        defaultIcon = categoryDefaultIcon,
        defaultPhotoUri = categoryDefaultPhoto,
        equipmentCategoryColor = categoryColor,
        onAddEquipment = viewModel::addEquipment,
        onUpdateEquipments = viewModel::updateEquipments,
        onUpdateEquipment = viewModel::updateEquipment,
        onDeleteEquipment = viewModel::deleteEquipment,
        onDismissEquipment = viewModel::dismissEquipment,
        onRestoreEquipment = viewModel::restoreEquipment,
        showDismissed = showDismissed,
        onToggleShowDismissed = viewModel::onToggleShowDismissed,
        showAddDialog = showAddDialog,
        cloningEquipment = cloningEquipment,
        onShowAddDialogChange = viewModel::onShowAddDialogChange,
        onCloneEquipment = viewModel::onCloneEquipment,
        onAddImage = viewModel::addImage,
        onToggleImageVisibility = viewModel::toggleImageVisibility,
        snackbarHostState = snackbarHostState,
        defaultEquipmentId = defaultEquipmentId,
        onToggleDefault = viewModel::toggleDefaultEquipment,
        categoryColors = categoryColorsMap,
        categoryDefaultIcons = categoryDefaultIconsMap,
        categoryDefaultPhotos = categoryDefaultPhotosMap,
        equipmentStatuses = equipmentStatuses,
        allDrafts = allDrafts,
        addDraft = addDraft,
        onUpdateAddDraft = viewModel::updateAddDraft,
        onStartEdit = viewModel::startEditing,
        onCancelEdit = viewModel::cancelEditing,
        onToggleDefaultInDraft = viewModel::toggleDefaultInDraft,
        onUpdateDraft = viewModel::updateDraft,
        onSaveEdit = viewModel::saveEditing,
        onPredictionAction = viewModel::onPredictionAction,
        onPlannedAction = viewModel::onPlannedAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentScreenContent(
    equipments: List<Equipment>,
    equipmentImages: List<Image>,
    allCategories: List<Category>,
    allSections: List<Section>,
    selectedSectionId: Int,
    onSectionSelected: (Int) -> Unit,
    showDismissedSections: Boolean,
    onToggleShowDismissedSections: () -> Unit,
    measurementUnits: List<MeasurementUnit>,
    defaultUnitId: Int?,
    defaultIcon: String?,
    defaultPhotoUri: String?,
    equipmentCategoryColor: String?,
    showDismissed: Boolean,
    onToggleShowDismissed: () -> Unit,
    showAddDialog: Boolean,
    cloningEquipment: Equipment? = null,
    onShowAddDialogChange: (Boolean) -> Unit,
    onCloneEquipment: (Equipment) -> Unit,
    onAddEquipment: (String, ImageIdentifier?, Int, Int, Boolean, Int, TimeGranularity, Double?, TimeGranularity, Int, TimeGranularity, Boolean, Boolean) -> Unit,
    onUpdateEquipments: (List<Equipment>) -> Unit,
    onUpdateEquipment: (Equipment) -> Unit,
    onDeleteEquipment: (Equipment) -> Unit,
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
    allDrafts: Map<Int, EquipmentDraft> = emptyMap(),
    addDraft: EquipmentDraft? = null,
    onUpdateAddDraft: (EquipmentDraft) -> Unit,
    onStartEdit: (Equipment) -> Unit,
    onCancelEdit: (Int) -> Unit,
    onToggleDefaultInDraft: (Int) -> Unit,
    onUpdateDraft: (EquipmentDraft) -> Unit,
    onSaveEdit: (EquipmentDraft) -> Unit,
    onPredictionAction: (Int, OperationStatus) -> Unit,
    onPlannedAction: (Int, OperationStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val equipmentsState = remember(equipments) { equipments.toMutableStateList() }
    var expandAllTrigger by remember { mutableIntStateOf(0) }
    var collapseAllTrigger by remember { mutableIntStateOf(0) }

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
                allSections = allSections,
                selectedSectionId = selectedSectionId,
                showDismissedSections = showDismissedSections,
                defaultUnitId = defaultUnitId,
                equipmentCategoryColor = equipmentCategoryColor,
                categoryColors = categoryColors,
                categoryDefaultIcons = categoryDefaultIcons,
                categoryDefaultPhotos = categoryDefaultPhotos,
                onDismissRequest = { onShowAddDialogChange(false) },
                onConfirm = { desc, identifier, unitId, sectionId, isResettable, window, windowUnit, avgValue, avgUnit, horizon, horizonUnit, customWindow, customHorizon ->
                    onAddEquipment(desc, identifier, unitId, sectionId, isResettable, window, windowUnit, avgValue, avgUnit, horizon, horizonUnit, customWindow, customHorizon)
                    onShowAddDialogChange(false)
                },
                onAddImage = onAddImage,
                onToggleImageVisibility = onToggleImageVisibility,
                initialEquipment = cloningEquipment,
                draft = addDraft,
                onUpdateDraft = onUpdateAddDraft
            )
        }

        Column(Modifier.padding(paddingValues)) {
            SectionChipBar(
                sections = allSections,
                selectedSectionId = selectedSectionId,
                onSectionSelected = onSectionSelected,
                showDismissed = showDismissedSections,
                onToggleShowDismissed = onToggleShowDismissedSections
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(onClick = { collapseAllTrigger++ }) {
                        Icon(Icons.Default.UnfoldLess, contentDescription = "Collapse All", tint = MaterialTheme.colorScheme.primary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.set_as_default_and_expand_instruction),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.hold_and_drag_to_reorder),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    IconButton(onClick = { expandAllTrigger++ }) {
                        Icon(Icons.Default.UnfoldMore, contentDescription = "Expand All", tint = MaterialTheme.colorScheme.primary)
                    }
                }
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
                    val draft = allDrafts[equipment.id]
                    val isOrigDefault = equipment.id == defaultEquipmentId
                    EquipmentCard(
                        equipment = equipment,
                        equipmentImages = equipmentImages,
                        allCategories = allCategories,
                        measurementUnits = measurementUnits,
                        allSections = allSections,
                        showDismissedSections = showDismissedSections,
                        onUpdateEquipment = onUpdateEquipment,
                        onDeleteEquipment = onDeleteEquipment,
                        onDismissEquipment = onDismissEquipment,
                        onRestoreEquipment = onRestoreEquipment,
                        onCloneEquipment = onCloneEquipment,
                        draft = draft,
                        onStartEdit = { onStartEdit(equipment) },
                        onCancelEdit = { onCancelEdit(equipment.id) },
                        onUpdateDraft = onUpdateDraft,
                        onSaveEdit = onSaveEdit,
                        onAddImage = onAddImage,
                        onToggleImageVisibility = onToggleImageVisibility,
                        equipmentCategoryColor = equipmentCategoryColor,
                        isDefault = draft?.isDefault ?: isOrigDefault,
                        originalIsDefault = isOrigDefault,
                        onToggleDefault = { 
                            if (draft != null) onToggleDefaultInDraft(equipment.id)
                            else onToggleDefault(equipment.id)
                        },
                        status = equipmentStatuses[equipment.id],
                        onPredictionAction = { onPredictionAction(equipment.id, it) },
                        onPlannedAction = { onPlannedAction(equipment.id, it) },
                        categoryColors = categoryColors,
                        categoryDefaultIcons = categoryDefaultIcons,
                        categoryDefaultPhotos = categoryDefaultPhotos,
                        expandAllTrigger = expandAllTrigger,
                        collapseAllTrigger = collapseAllTrigger
                    )
                }
            )
        }
    }
}
