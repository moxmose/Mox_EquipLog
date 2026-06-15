package com.moxmose.moxequiplog.ui.operations

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.*
import com.moxmose.moxequiplog.ui.components.DraggableLazyColumn
import com.moxmose.moxequiplog.ui.components.SectionChipBar
import com.moxmose.moxequiplog.ui.maintenancelog.components.MaintenanceLogDialog
import com.moxmose.moxequiplog.ui.maintenancelog.MaintenanceLogViewModel
import com.moxmose.moxequiplog.ui.operations.components.AddOperationTypeDialog
import com.moxmose.moxequiplog.ui.operations.components.OperationTypeCard
import com.moxmose.moxequiplog.ui.options.OptionsViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun OperationTypeScreen(
    viewModel: OperationsTypeViewModel = koinViewModel(), 
    optionsViewModel: OptionsViewModel = koinViewModel(),
    logsViewModel: MaintenanceLogViewModel = koinViewModel()
) {
    val activeOperationTypes by viewModel.activeOperationTypes.collectAsState()
    val allOperationTypes by viewModel.allOperationTypes.collectAsState()
    val allSections by viewModel.allSections.collectAsState()
    val selectedSectionId by viewModel.selectedSectionId.collectAsState()
    val showDismissedSections by viewModel.showDismissedSections.collectAsState()
    val operationTypeImages by viewModel.operationImages.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val defaultOperationTypeId by viewModel.defaultOperationTypeId.collectAsState()
    val operationStatuses by viewModel.operationStatuses.collectAsState()
    val allDrafts by viewModel.allDrafts.collectAsState()
    val addDraft by viewModel.addDraft.collectAsState()
    
    val categoryColor by viewModel.categoryColor.collectAsState()
    val categoryDefaultIcon by viewModel.categoryDefaultIcon.collectAsState()
    val categoryDefaultPhoto by viewModel.categoryDefaultPhoto.collectAsState()

    val showDismissed by viewModel.showDismissed.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val cloningOperationType by viewModel.cloningOperationType.collectAsState()
    val selectedAffectedEquipmentForAdd by viewModel.selectedAffectedEquipmentForAdd.collectAsState()
    val logAddDraft by logsViewModel.logAddDraft.collectAsState()
    val reminderAddDraft by logsViewModel.reminderAddDraft.collectAsState()

    val categoriesUiState by optionsViewModel.categoriesUiState.collectAsState()
    val categoryColorsMap = remember(categoriesUiState) { categoriesUiState.associate { it.category.id to it.color } }
    val categoryDefaultIconsMap = remember(categoriesUiState) { categoriesUiState.associate { it.category.id to it.defaultIconIdentifier } }
    val categoryDefaultPhotosMap = remember(categoriesUiState) { categoriesUiState.associate { it.category.id to it.defaultPhotoUri } }

    val syncCalendarByDefault by logsViewModel.syncCalendarByDefault.collectAsState()
    val googleAccountName by logsViewModel.googleAccountName.collectAsState()
    val measurementUnits by logsViewModel.measurementUnits.collectAsState()
    val activeEquipments by logsViewModel.allEquipments.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(key1 = true) {
        viewModel.uiEvents.collectLatest { event ->
            val message = when(event) {
                is OperationsTypeViewModel.UiEvent.DescriptionInvalid -> context.getString(R.string.description_invalid)
                is OperationsTypeViewModel.UiEvent.AddOperationTypeFailed -> context.getString(R.string.add_operation_type_failed)
                is OperationsTypeViewModel.UiEvent.UpdateOperationTypeFailed -> context.getString(R.string.update_operation_type_failed)
                is OperationsTypeViewModel.UiEvent.UpdateOperationTypesFailed -> context.getString(R.string.update_operation_types_failed)
                is OperationsTypeViewModel.UiEvent.DismissOperationTypeFailed -> context.getString(R.string.dismiss_operation_type_failed)
                is OperationsTypeViewModel.UiEvent.RestoreOperationTypeFailed -> context.getString(R.string.restore_operation_type_failed)
                is OperationsTypeViewModel.UiEvent.AddImageFailed -> context.getString(R.string.add_image_failed)
                is OperationsTypeViewModel.UiEvent.RemoveImageFailed -> context.getString(R.string.remove_image_failed)
                is OperationsTypeViewModel.UiEvent.UpdateImageOrderFailed -> context.getString(R.string.update_image_order_failed)
                is OperationsTypeViewModel.UiEvent.ToggleImageVisibilityFailed -> context.getString(R.string.toggle_image_visibility_failed)
                is OperationsTypeViewModel.UiEvent.DatabaseCheckFailed -> context.getString(R.string.database_check_failed)
                is OperationsTypeViewModel.UiEvent.PhotoUriInvalid -> context.getString(R.string.photo_uri_invalid)
                is OperationsTypeViewModel.UiEvent.SetDefaultFailed -> context.getString(R.string.error_unknown)
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    if (selectedAffectedEquipmentForAdd != null) {
        val (opId, status) = selectedAffectedEquipmentForAdd!!
        MaintenanceLogDialog(
            equipments = activeEquipments.filter { !it.dismissed },
            operationTypes = allOperationTypes.filter { !it.dismissed },
            measurementUnits = measurementUnits,
            allSections = allSections,
            onDismissRequest = { viewModel.onAffectedAction(0, null) },
            onConfirm = { log ->
                val now = System.currentTimeMillis()
                if (log.date > now + 60000) { // Se è nel futuro (> 1 min), diventa un reminder (pianificato)
                    if (status.isPlanned && status.reminderId != null) {
                        logsViewModel.updateReminder(log.equipmentId, log.operationTypeId, log.date, log.value, syncCalendarByDefault, status.reminderId)
                    } else {
                        logsViewModel.addReminder(log.equipmentId, log.operationTypeId, log.date, log.value, syncCalendarByDefault)
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
                viewModel.onAffectedAction(0, null)
            },
            onSchedule = { eqId, opId, date, value, sync ->
                if (status.isPlanned && status.reminderId != null) {
                    logsViewModel.updateReminder(eqId, opId, date, value, sync, status.reminderId)
                } else {
                    logsViewModel.addReminder(eqId, opId, date, value, sync)
                }
                viewModel.onAffectedAction(0, null)
            },
            defaultEquipmentId = status.equipment.id,
            defaultOperationTypeId = opId,
            initialDate = if ((status.nextPresumedDate ?: 0L) > System.currentTimeMillis()) status.nextPresumedDate!! else System.currentTimeMillis(),
            initialValue = if (status.isPlanned) status.plannedValue?.toString() ?: "" else "", 
            isEditMode = status.isPlanned,
            initialTab = 1, // Start on "Planned" for predictions
            equipmentCategoryColor = categoryColorsMap[Category.EQUIPMENT],
            operationCategoryColor = categoryColor,
            syncCalendarByDefault = syncCalendarByDefault,
            googleAccountName = googleAccountName,
            logDraft = logAddDraft,
            reminderDraft = reminderAddDraft,
            onUpdateLogDraft = logsViewModel::updateLogAddDraft,
            onUpdateReminderDraft = logsViewModel::updateReminderAddDraft
        )
    }

    val typesToShow = if (showDismissed) allOperationTypes else activeOperationTypes

    OperationTypeScreenContent(
        operationTypes = typesToShow,
        operationTypeImages = operationTypeImages,
        allCategories = allCategories,
        allSections = allSections,
        selectedSectionId = selectedSectionId,
        onSectionSelected = viewModel::onSectionSelected,
        showDismissedSections = showDismissedSections,
        onToggleShowDismissedSections = viewModel::onToggleShowDismissedSections,
        defaultIcon = categoryDefaultIcon,
        defaultPhotoUri = categoryDefaultPhoto,
        onAddOperationType = viewModel::addOperationType,
        onUpdateOperationTypes = viewModel::updateOperationTypes,
        onUpdateOperationType = viewModel::updateOperationType,
        onDeleteOperationType = viewModel::deleteOperationType,
        onDismissOperationType = viewModel::dismissOperationType,
        onRestoreOperationType = viewModel::restoreOperationType,
        showDismissed = showDismissed,
        onToggleShowDismissed = viewModel::onToggleShowDismissed,
        showAddDialog = showAddDialog,
        cloningOperationType = cloningOperationType,
        onShowAddDialogChange = viewModel::onShowAddDialogChange,
        onCloneOperationType = viewModel::onCloneOperationType,
        onAddImage = viewModel::addImage,
        onToggleImageVisibility = viewModel::toggleImageVisibility,
        operationCategoryColor = categoryColor,
        snackbarHostState = snackbarHostState,
        defaultOperationTypeId = defaultOperationTypeId,
        onToggleDefault = viewModel::toggleDefaultOperationType,
        categoryColors = categoryColorsMap,
        categoryDefaultIcons = categoryDefaultIconsMap,
        categoryDefaultPhotos = categoryDefaultPhotosMap,
        operationStatuses = operationStatuses,
        allDrafts = allDrafts,
        addDraft = addDraft,
        onUpdateAddDraft = viewModel::updateAddDraft,
        onStartEdit = viewModel::startEditing,
        onCancelEdit = viewModel::cancelEditing,
        onToggleDefaultInDraft = viewModel::toggleDefaultInDraft,
        onUpdateDraft = viewModel::updateDraft,
        onSaveEdit = viewModel::saveEditing,
        onAffectedAction = viewModel::onAffectedAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationTypeScreenContent(
    operationTypes: List<OperationType>,
    operationTypeImages: List<Image>,
    allCategories: List<Category>,
    allSections: List<Section>,
    selectedSectionId: Int,
    onSectionSelected: (Int) -> Unit,
    showDismissedSections: Boolean,
    onToggleShowDismissedSections: () -> Unit,
    defaultIcon: String?,
    defaultPhotoUri: String?,
    showDismissed: Boolean,
    onToggleShowDismissed: () -> Unit,
    showAddDialog: Boolean,
    cloningOperationType: OperationType? = null,
    onShowAddDialogChange: (Boolean) -> Unit,
    onCloneOperationType: (OperationType) -> Unit,
    onAddOperationType: (String, ImageIdentifier?, Int, Boolean, Double?, Int?, TimeGranularity?, Int, TimeGranularity, Boolean, Double?) -> Unit,
    onUpdateOperationTypes: (List<OperationType>) -> Unit,
    onUpdateOperationType: (OperationType) -> Unit,
    onDeleteOperationType: (OperationType) -> Unit,
    onDismissOperationType: (OperationType) -> Unit,
    onRestoreOperationType: (OperationType) -> Unit,
    onAddImage: (ImageIdentifier, String) -> Unit,
    onToggleImageVisibility: (Image) -> Unit,
    operationCategoryColor: String,
    snackbarHostState: SnackbarHostState,
    defaultOperationTypeId: Int?,
    onToggleDefault: (Int) -> Unit,
    categoryColors: Map<String, String>,
    categoryDefaultIcons: Map<String, String?>,
    categoryDefaultPhotos: Map<String, String?>,
    operationStatuses: Map<Int, OperationGlobalStatus> = emptyMap(),
    allDrafts: Map<Int, OperationTypeDraft> = emptyMap(),
    addDraft: OperationTypeDraft? = null,
    onUpdateAddDraft: (OperationTypeDraft) -> Unit,
    onStartEdit: (OperationType) -> Unit,
    onCancelEdit: (Int) -> Unit,
    onToggleDefaultInDraft: (Int) -> Unit,
    onUpdateDraft: (OperationTypeDraft) -> Unit,
    onSaveEdit: (OperationTypeDraft) -> Unit,
    onAffectedAction: (Int, EquipmentOperationStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val operationTypesState = remember(operationTypes) { operationTypes.toMutableStateList() }
    var expandAllTrigger by remember { mutableIntStateOf(0) }
    var collapseAllTrigger by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(onClick = { onShowAddDialogChange(true) }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_operation_type))
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
            AddOperationTypeDialog(
                imageLibrary = operationTypeImages,
                categories = allCategories,
                allSections = allSections,
                selectedSectionId = selectedSectionId,
                showDismissedSections = showDismissedSections,
                categoryColors = categoryColors,
                categoryDefaultIcons = categoryDefaultIcons,
                categoryDefaultPhotos = categoryDefaultPhotos,
                defaultIcon = defaultIcon,
                defaultPhotoUri = defaultPhotoUri,
                onDismissRequest = { onShowAddDialogChange(false) },
                onConfirm = { description, identifier, sectionId, isPredictable, interval, timeout, timeoutUnit, horizon, horizonUnit, customHorizon, cost ->
                    onAddOperationType(description, identifier, sectionId, isPredictable, interval, timeout, timeoutUnit, horizon, horizonUnit, customHorizon, cost)
                    onShowAddDialogChange(false)
                },
                onAddImage = onAddImage,
                onToggleImageVisibility = onToggleImageVisibility,
                operationCategoryColor = operationCategoryColor,
                initialOperationType = cloningOperationType,
                draft = addDraft,
                onUpdateDraft = onUpdateAddDraft
            )
        }

        Column(Modifier.padding(paddingValues).fillMaxSize()) {
            SectionChipBar(
                sections = allSections,
                selectedSectionId = selectedSectionId,
                onSectionSelected = onSectionSelected,
                showDismissed = showDismissedSections,
                onToggleShowDismissed = onToggleShowDismissedSections
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(onClick = { collapseAllTrigger++ }) {
                        Icon(Icons.Default.UnfoldLess, contentDescription = "Collapse All", tint = MaterialTheme.colorScheme.primary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = stringResource(R.string.set_as_default_and_expand_instruction), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(text = stringResource(R.string.hold_and_drag_to_reorder), style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { expandAllTrigger++ }) {
                        Icon(Icons.Default.UnfoldMore, contentDescription = "Expand All", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            DraggableLazyColumn(
                items = operationTypesState,
                key = { _, operationType -> operationType.id },
                onMove = { from, to -> operationTypesState.add(to, operationTypesState.removeAt(from)) },
                onDrop = {
                    val reorderedTypes = operationTypesState.mapIndexed { index, type -> type.copy(displayOrder = index) }
                    onUpdateOperationTypes(reorderedTypes)
                },
                modifier = Modifier.fillMaxSize(),
                itemContent = { _, operationType ->
                    val draft = allDrafts[operationType.id]
                    OperationTypeCard(
                        operationType = operationType,
                        allSections = allSections,
                        showDismissedSections = showDismissedSections,
                        onUpdateOperationType = onUpdateOperationType,
                        onDeleteOperationType = onDeleteOperationType,
                        onDismissOperationType = onDismissOperationType,
                        onRestoreOperationType = onRestoreOperationType,
                        operationTypeImages = operationTypeImages,
                        allCategories = allCategories,
                        categoryColors = categoryColors,
                        categoryDefaultIcons = categoryDefaultIcons,
                        categoryDefaultPhotos = categoryDefaultPhotos,
                        onAddImage = onAddImage,
                        onToggleImageVisibility = onToggleImageVisibility,
                        operationCategoryColor = operationCategoryColor,
                        isDefault = draft?.isDefault ?: (operationType.id == defaultOperationTypeId),
                        onToggleDefault = { 
                            if (draft != null) onToggleDefaultInDraft(operationType.id)
                            else onToggleDefault(operationType.id)
                        },
                        onCloneOperationType = onCloneOperationType,
                        draft = draft,
                        onStartEdit = { onStartEdit(operationType) },
                        onCancelEdit = { onCancelEdit(operationType.id) },
                        onUpdateDraft = onUpdateDraft,
                        onSaveEdit = onSaveEdit,
                        status = operationStatuses[operationType.id],
                        onAffectedAction = { onAffectedAction(operationType.id, it) },
                        expandAllTrigger = expandAllTrigger,
                        collapseAllTrigger = collapseAllTrigger
                    )
                }
            )
        }
    }
}
