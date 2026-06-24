package com.moxmose.moxequiplog.ui.maintenancelog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.Equipment
import com.moxmose.moxequiplog.data.local.MaintenanceLog
import com.moxmose.moxequiplog.data.local.MaintenanceLogDetails
import com.moxmose.moxequiplog.data.local.MaintenanceReminder
import com.moxmose.moxequiplog.data.local.MaintenanceReminderDetails
import com.moxmose.moxequiplog.data.local.MeasurementUnit
import com.moxmose.moxequiplog.data.local.OperationType
import com.moxmose.moxequiplog.data.local.Section
import com.moxmose.moxequiplog.ui.components.SectionChipBar
import com.moxmose.moxequiplog.ui.components.UnifiedSectionSelector
import com.moxmose.moxequiplog.ui.maintenancelog.components.MaintenanceLogCard
import com.moxmose.moxequiplog.ui.maintenancelog.components.MaintenanceLogDialog
import com.moxmose.moxequiplog.ui.maintenancelog.components.RemindersDashboard
import com.moxmose.moxequiplog.ui.maintenancelog.components.PredictionsDashboard
import com.moxmose.moxequiplog.ui.equipment.OperationStatus
import com.moxmose.moxequiplog.utils.UiConstants
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun MaintenanceLogScreen(
    viewModel: MaintenanceLogViewModel = koinViewModel(),
    onNavigateToOptions: () -> Unit = {}
) {
    val logs by viewModel.logs.collectAsState()
    val activeReminders by viewModel.activeReminders.collectAsState()
    val automaticPredictions by viewModel.automaticPredictions.collectAsState()
    val allSections by viewModel.allSections.collectAsState()
    val selectedSectionId by viewModel.selectedSectionId.collectAsState()
    val showDismissedSections by viewModel.showDismissedSections.collectAsState()
    val allEquipments by viewModel.allEquipments.collectAsState()
    val allOperationTypes by viewModel.allOperationTypes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortProperty by viewModel.sortProperty.collectAsState()
    val sortDirection by viewModel.sortDirection.collectAsState()
    val showDismissed by viewModel.showDismissed.collectAsState()
    val defaultEquipmentId by viewModel.defaultEquipmentId.collectAsState()
    val defaultOperationTypeId by viewModel.defaultOperationTypeId.collectAsState()
    val measurementUnits by viewModel.measurementUnits.collectAsState()
    val syncCalendarByDefault by viewModel.syncCalendarByDefault.collectAsState()
    val googleAccountName by viewModel.googleAccountName.collectAsState()
    val costTrendThreshold by viewModel.costTrendThreshold.collectAsState()
    
    val equipmentColor by viewModel.getCategoryColor(Category.EQUIPMENT).collectAsState(initial = UiConstants.DEFAULT_FALLBACK_COLOR)
    val operationColor by viewModel.getCategoryColor(Category.OPERATION).collectAsState(initial = UiConstants.DEFAULT_FALLBACK_COLOR)

    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val expandedCardId by viewModel.expandedCardId.collectAsState()
    val editingCardId by viewModel.editingCardId.collectAsState()
    val allDrafts by viewModel.allDrafts.collectAsState()
    val selectedReminderForComplete by viewModel.selectedReminderForComplete.collectAsState()
    val selectedReminderForEdit by viewModel.selectedReminderForEdit.collectAsState()
    val selectedPredictionForAdd by viewModel.selectedPredictionForAdd.collectAsState()
    val logAddDraft by viewModel.logAddDraft.collectAsState()
    val reminderAddDraft by viewModel.reminderAddDraft.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(key1 = true) {
        viewModel.uiEvents.collectLatest { event ->
            val message = when (event) {
                is MaintenanceLogViewModel.UiEvent.AddLogFailed -> context.getString(R.string.add_log_failed)
                is MaintenanceLogViewModel.UiEvent.UpdateLogFailed -> context.getString(R.string.update_log_failed)
                is MaintenanceLogViewModel.UiEvent.DismissLogFailed -> context.getString(R.string.dismiss_log_failed)
                is MaintenanceLogViewModel.UiEvent.RestoreLogFailed -> context.getString(R.string.restore_log_failed)
                is MaintenanceLogViewModel.UiEvent.DeleteLogFailed -> context.getString(R.string.delete_log_failed)
                is MaintenanceLogViewModel.UiEvent.DeleteReminderFailed -> context.getString(R.string.delete_reminder_failed)
                is MaintenanceLogViewModel.UiEvent.UpdateReminderFailed -> context.getString(R.string.update_reminder_failed)
                is MaintenanceLogViewModel.UiEvent.RecalculateRemindersFailed -> context.getString(R.string.recalculate_reminders_failed)
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    val equipmentsToShow = remember(allEquipments, showDismissed, showDismissedSections, allSections) {
        val dismissedSectionIds = allSections.filter { it.dismissed }.map { it.id }.toSet()
        allEquipments.filter { 
            (showDismissed || !it.dismissed) && 
            (showDismissedSections || it.sectionId !in dismissedSectionIds)
        }.sortedBy { it.displayOrder }
    }
    val operationTypesToShow = remember(allOperationTypes, showDismissed, showDismissedSections, allSections) {
        val dismissedSectionIds = allSections.filter { it.dismissed }.map { it.id }.toSet()
        allOperationTypes.filter { 
            (showDismissed || !it.dismissed) && 
            (showDismissedSections || it.sectionId !in dismissedSectionIds)
        }.sortedBy { it.displayOrder }
    }

    if (selectedReminderForComplete != null) {
        val details = selectedReminderForComplete!!
        MaintenanceLogDialog(
            equipments = equipmentsToShow,
            operationTypes = operationTypesToShow,
            measurementUnits = measurementUnits,
            allSections = allSections,
            onDismissRequest = { viewModel.onCompleteReminder(null) },
            onConfirm = { log ->
                viewModel.addLog(
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
                viewModel.onCompleteReminder(null)
            },
            onEstimateDueDate = viewModel::estimateDueDate,
            onEstimateTargetValue = viewModel::estimateTargetValue,
            defaultEquipmentId = details.reminder.equipmentId,
            defaultOperationTypeId = details.reminder.operationTypeId,
            initialCost = (details.lastLogCost ?: details.operationTypeEstimatedCost)?.toString() ?: "",
            equipmentCategoryColor = equipmentColor,
            operationCategoryColor = operationColor,
            syncCalendarByDefault = syncCalendarByDefault,
            googleAccountName = googleAccountName,
            costTrendThreshold = costTrendThreshold,
            onNavigateToOptions = onNavigateToOptions,
            logDraft = logAddDraft,
            reminderDraft = reminderAddDraft,
            onUpdateLogDraft = viewModel::updateLogAddDraft,
            onUpdateReminderDraft = viewModel::updateReminderAddDraft
        )
    }

    if (selectedReminderForEdit != null) {
        val reminderDetails = selectedReminderForEdit!!
        val reminder = reminderDetails.reminder
        MaintenanceLogDialog(
            equipments = equipmentsToShow,
            operationTypes = operationTypesToShow,
            measurementUnits = measurementUnits,
            allSections = allSections,
            onDismissRequest = { viewModel.onEditReminder(null) },
            onConfirm = { /* Not used in edit mode */ },
            onSchedule = { eqId, opId, date, value, sync ->
                viewModel.updateReminder(eqId, opId, date, value, sync, reminder.id)
                viewModel.onEditReminder(null)
            },
            onDeleteReminder = {
                viewModel.deleteReminder(reminderDetails)
                viewModel.onEditReminder(null)
            },
            onEstimateDueDate = viewModel::estimateDueDate,
            onEstimateTargetValue = viewModel::estimateTargetValue,
            defaultEquipmentId = reminder.equipmentId,
            defaultOperationTypeId = reminder.operationTypeId,
            initialDate = reminder.dueDate ?: System.currentTimeMillis(),
            initialValue = reminder.dueValue?.toString() ?: "",
            initialSyncToCalendar = reminder.calendarEventId != null,
            initialHasFixedDate = reminder.dueDate != null,
            isEditMode = true,
            equipmentCategoryColor = equipmentColor,
            operationCategoryColor = operationColor,
            syncCalendarByDefault = syncCalendarByDefault,
            googleAccountName = googleAccountName,
            costTrendThreshold = costTrendThreshold,
            onNavigateToOptions = onNavigateToOptions,
            logDraft = logAddDraft,
            reminderDraft = reminderAddDraft,
            onUpdateLogDraft = viewModel::updateLogAddDraft,
            onUpdateReminderDraft = viewModel::updateReminderAddDraft
        )
    }

    if (selectedPredictionForAdd != null) {
        val (eqId, status) = selectedPredictionForAdd!!
        MaintenanceLogDialog(
            equipments = equipmentsToShow,
            operationTypes = operationTypesToShow,
            measurementUnits = measurementUnits,
            allSections = allSections,
            onDismissRequest = { viewModel.onPredictionAction(0, null) },
            onConfirm = { log ->
                viewModel.addLog(
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
                viewModel.onPredictionAction(0, null)
            },
            onSchedule = { equipmentId, opTypeId, date, value, sync ->
                viewModel.addReminder(equipmentId, opTypeId, date, value, sync)
                viewModel.onPredictionAction(0, null)
            },
            onEstimateDueDate = viewModel::estimateDueDate,
            onEstimateTargetValue = viewModel::estimateTargetValue,
            onGetOperationCostStats = viewModel::getOperationCostStats,
            defaultEquipmentId = eqId,
            defaultOperationTypeId = status.operation.id,
            initialDate = if (status.nextPresumedDate != null && status.nextPresumedDate!! > System.currentTimeMillis()) status.nextPresumedDate!! else System.currentTimeMillis(),
            initialTab = 1,
            equipmentCategoryColor = equipmentColor,
            operationCategoryColor = operationColor,
            syncCalendarByDefault = syncCalendarByDefault,
            googleAccountName = googleAccountName,
            costTrendThreshold = costTrendThreshold,
            onNavigateToOptions = onNavigateToOptions,
            logDraft = logAddDraft,
            reminderDraft = reminderAddDraft,
            onUpdateLogDraft = viewModel::updateLogAddDraft,
            onUpdateReminderDraft = viewModel::updateReminderAddDraft
        )
    }

    MaintenanceLogScreenContent(
        logs = logs,
        allSections = allSections,
        selectedSectionId = selectedSectionId,
        onSectionSelected = viewModel::onSectionSelected,
        showDismissedSections = showDismissedSections,
        onToggleShowDismissedSections = viewModel::onToggleShowDismissedSections,
        equipments = equipmentsToShow,
        operationTypes = operationTypesToShow,
        measurementUnits = measurementUnits,
        searchQuery = searchQuery,
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        sortProperty = sortProperty,
        onSortPropertyChange = viewModel::onSortPropertyChanged,
        sortDirection = sortDirection,
        onSortDirectionChange = viewModel::onSortDirectionChanged,
        showDismissed = showDismissed,
        onShowDismissedToggle = viewModel::onShowDismissedToggled,
        showAddDialog = showAddDialog,
        onShowAddDialogChange = viewModel::onShowAddDialogChange,
        onAddLog = viewModel::addLog,
        onAddReminder = viewModel::addReminder,
        onRefreshReminders = viewModel::recalculateAllReminders,
        onEstimateDueDate = viewModel::estimateDueDate,
        onEstimateTargetValue = viewModel::estimateTargetValue,
        onGetOperationCostStats = viewModel::getOperationCostStats,
        expandedCardId = expandedCardId,
        onCardExpanded = viewModel::onCardExpanded,
        editingCardId = editingCardId,
        allDrafts = allDrafts,
        onStartEdit = viewModel::startEditing,
        onCancelEdit = viewModel::cancelEditing,
        onUpdateDraft = viewModel::updateDraft,
        onSaveEdit = viewModel::saveEditing,
        onUpdateLog = viewModel::updateLog,
        onDeleteLog = viewModel::deleteLog,
        onDismissLog = viewModel::dismissLog,
        onRestoreLog = viewModel::restoreLog,
        activeReminders = activeReminders,
        automaticPredictions = automaticPredictions,
        snackbarHostState = snackbarHostState,
        defaultEquipmentId = defaultEquipmentId,
        defaultOperationTypeId = defaultOperationTypeId,
        equipmentCategoryColor = equipmentColor,
        operationCategoryColor = operationColor,
        onCompleteReminder = viewModel::onCompleteReminder,
        onEditReminder = viewModel::onEditReminder,
        onPredictionAction = viewModel::onPredictionAction,
        syncCalendarByDefault = syncCalendarByDefault,
        googleAccountName = googleAccountName,
        costTrendThreshold = costTrendThreshold,
        onNavigateToOptions = onNavigateToOptions,
        logAddDraft = logAddDraft,
        reminderAddDraft = reminderAddDraft,
        onUpdateLogDraft = viewModel::updateLogAddDraft,
        onUpdateReminderDraft = viewModel::updateReminderAddDraft
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceLogScreenContent(
    logs: List<MaintenanceLogDetails>,
    allSections: List<Section>,
    selectedSectionId: Int,
    onSectionSelected: (Int) -> Unit,
    showDismissedSections: Boolean,
    onToggleShowDismissedSections: () -> Unit,
    equipments: List<Equipment>,
    operationTypes: List<OperationType>,
    measurementUnits: List<MeasurementUnit>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortProperty: SortProperty,
    onSortPropertyChange: (SortProperty) -> Unit,
    sortDirection: SortDirection,
    onSortDirectionChange: () -> Unit,
    showDismissed: Boolean,
    onShowDismissedToggle: () -> Unit,
    showAddDialog: Boolean,
    onShowAddDialogChange: (Boolean) -> Unit,
    onAddLog: (Int, Int, String?, Double?, Long, String?, Boolean, Double?, Boolean) -> Unit,
    onAddReminder: (Int, Int, Long?, Double?, Boolean) -> Unit,
    onRefreshReminders: () -> Unit,
    onEstimateDueDate: suspend (Int, Double) -> Long?,
    onEstimateTargetValue: suspend (Int, Long) -> Double?,
    onGetOperationCostStats: suspend (Int) -> Pair<Double?, Double?>,
    expandedCardId: Int?,
    onCardExpanded: (Int) -> Unit,
    editingCardId: Int?,
    allDrafts: Map<Int, MaintenanceLog> = emptyMap(),
    onStartEdit: (MaintenanceLog) -> Unit,
    onCancelEdit: (Int) -> Unit,
    onUpdateDraft: (MaintenanceLog) -> Unit,
    onSaveEdit: (MaintenanceLog) -> Unit,
    onUpdateLog: (MaintenanceLog) -> Unit,
    onDeleteLog: (MaintenanceLog) -> Unit,
    onDismissLog: (MaintenanceLog) -> Unit,
    onRestoreLog: (MaintenanceLog) -> Unit,
    modifier: Modifier = Modifier,
    activeReminders: List<MaintenanceReminderDetails> = emptyList(),
    automaticPredictions: List<Pair<Equipment, OperationStatus>> = emptyList(),
    snackbarHostState: SnackbarHostState,
    defaultEquipmentId: Int?,
    defaultOperationTypeId: Int?,
    equipmentCategoryColor: String?,
    operationCategoryColor: String?,
    onCompleteReminder: (MaintenanceReminderDetails) -> Unit,
    onEditReminder: (MaintenanceReminderDetails) -> Unit,
    onPredictionAction: (Int, OperationStatus) -> Unit,
    syncCalendarByDefault: Boolean,
    googleAccountName: String?,
    costTrendThreshold: Float,
    logAddDraft: MaintenanceLog?,
    reminderAddDraft: MaintenanceReminder?,
    onUpdateLogDraft: (MaintenanceLog) -> Unit,
    onUpdateReminderDraft: (MaintenanceReminder) -> Unit,
    onNavigateToOptions: () -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(onClick = { onShowAddDialogChange(true) }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_log))
                }
                Spacer(modifier = Modifier.padding(8.dp))
                FloatingActionButton(
                    onClick = onShowDismissedToggle,
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
        Column(Modifier.padding(paddingValues).fillMaxSize()) {
            UnifiedSectionSelector(
                allSections = allSections,
                selectedSectionId = selectedSectionId,
                onSectionSelected = onSectionSelected,
                showDismissedSections = showDismissedSections,
                onToggleShowDismissedSections = onToggleShowDismissedSections
            )
            
            if (showAddDialog) {
                MaintenanceLogDialog(
                    equipments = equipments,
                    operationTypes = operationTypes,
                    measurementUnits = measurementUnits,
                    allSections = allSections,
                    onDismissRequest = { onShowAddDialogChange(false) },
                    onConfirm = { log ->
                        onAddLog(
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
                        onShowAddDialogChange(false)
                    },
                    onSchedule = { equipmentId, opTypeId, date, value, sync ->
                        onAddReminder(equipmentId, opTypeId, date, value, sync)
                        onShowAddDialogChange(false)
                    },
                    onEstimateDueDate = onEstimateDueDate,
                    onEstimateTargetValue = onEstimateTargetValue,
                    onGetOperationCostStats = onGetOperationCostStats,
                    defaultEquipmentId = defaultEquipmentId,
                    defaultOperationTypeId = defaultOperationTypeId,
                    equipmentCategoryColor = equipmentCategoryColor,
                    operationCategoryColor = operationCategoryColor,
                    syncCalendarByDefault = syncCalendarByDefault,
                    googleAccountName = googleAccountName,
                    costTrendThreshold = costTrendThreshold,
                    onNavigateToOptions = onNavigateToOptions,
                    logDraft = logAddDraft,
                    reminderDraft = reminderAddDraft,
                    onUpdateLogDraft = onUpdateLogDraft,
                    onUpdateReminderDraft = onUpdateReminderDraft
                )
            }

            RemindersDashboard(
                reminders = activeReminders,
                measurementUnits = measurementUnits,
                equipmentCategoryColor = equipmentCategoryColor,
                operationCategoryColor = operationCategoryColor,
                onComplete = onCompleteReminder,
                onEdit = onEditReminder,
                onRefresh = onRefreshReminders,
                costTrendThreshold = costTrendThreshold
            )

            PredictionsDashboard(
                predictions = automaticPredictions,
                equipmentCategoryColor = equipmentCategoryColor,
                operationCategoryColor = operationCategoryColor,
                onPredictionClick = { equipment, status -> onPredictionAction(equipment.id, status) }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text(stringResource(R.string.search_logs)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_logs)) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                )

                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.sort_by))
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortProperty.entries.forEach { prop ->
                            DropdownMenuItem(
                                text = { 
                                    val label = when (prop) {
                                        SortProperty.VALUE -> stringResource(R.string.measurement_unit)
                                        else -> prop.name.lowercase().replaceFirstChar { it.titlecase() }
                                    }
                                    Text(label) 
                                },
                                onClick = {
                                    onSortPropertyChange(prop)
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (sortProperty == prop) {
                                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.selected_content_desc))
                                    }
                                }
                            )
                        }
                    }
                }

                IconButton(onClick = onSortDirectionChange) {
                    Icon(
                        imageVector = if (sortDirection == SortDirection.DESCENDING) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = stringResource(R.string.sort_direction)
                    )
                }
            }
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
                items(logs, key = { it.log.id }) { logDetail ->
                    MaintenanceLogCard(
                        logDetail = logDetail,
                        equipments = equipments,
                        operationTypes = operationTypes,
                        measurementUnits = measurementUnits,
                        allSections = allSections,
                        isExpanded = logDetail.log.id == expandedCardId,
                        draft = allDrafts[logDetail.log.id],
                        onStartEdit = { onStartEdit(logDetail.log) },
                        onCancelEdit = { onCancelEdit(logDetail.log.id) },
                        onUpdateDraft = onUpdateDraft,
                        onSaveEdit = onSaveEdit,
                        onExpand = { onCardExpanded(logDetail.log.id) },
                        onSave = onUpdateLog,
                        onDelete = onDeleteLog,
                        onGetOperationCostStats = onGetOperationCostStats,
                        equipmentCategoryColor = equipmentCategoryColor,
                        operationCategoryColor = operationCategoryColor,
                        costTrendThreshold = costTrendThreshold
                    )
                }
            }
        }
    }
}
