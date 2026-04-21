package com.moxmose.moxequiplog.ui.maintenancelog

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.Equipment
import com.moxmose.moxequiplog.data.local.MaintenanceLog
import com.moxmose.moxequiplog.data.local.MaintenanceLogDetails
import com.moxmose.moxequiplog.data.local.MeasurementUnit
import com.moxmose.moxequiplog.data.local.OperationType
import com.moxmose.moxequiplog.utils.AppConstants
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Warning
import com.moxmose.moxequiplog.data.local.MaintenanceReminderDetails
import com.moxmose.moxequiplog.ui.components.ImageIcon
import com.moxmose.moxequiplog.utils.UiConstants
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MaintenanceLogScreen(
    viewModel: MaintenanceLogViewModel = koinViewModel(),
    onNavigateToOptions: () -> Unit = {}
) {
    val logs by viewModel.logs.collectAsState()
    val activeReminders by viewModel.activeReminders.collectAsState()
    val equipments by viewModel.allEquipments.collectAsState()
    val operationTypes by viewModel.allOperationTypes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortProperty by viewModel.sortProperty.collectAsState()
    val sortDirection by viewModel.sortDirection.collectAsState()
    val showDismissed by viewModel.showDismissed.collectAsState()
    val defaultEquipmentId by viewModel.defaultEquipmentId.collectAsState()
    val defaultOperationTypeId by viewModel.defaultOperationTypeId.collectAsState()
    val measurementUnits by viewModel.measurementUnits.collectAsState()
    val syncCalendarByDefault by viewModel.syncCalendarByDefault.collectAsState()
    val googleAccountName by viewModel.googleAccountName.collectAsState()
    
    val equipmentColor by viewModel.getCategoryColor(Category.EQUIPMENT).collectAsState(initial = UiConstants.DEFAULT_FALLBACK_COLOR)
    val operationColor by viewModel.getCategoryColor(Category.OPERATION).collectAsState(initial = UiConstants.DEFAULT_FALLBACK_COLOR)

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(key1 = true) {
        viewModel.uiEvents.collectLatest { event ->
            val message = when (event) {
                is MaintenanceLogViewModel.UiEvent.AddLogFailed -> context.getString(R.string.add_log_failed)
                is MaintenanceLogViewModel.UiEvent.UpdateLogFailed -> context.getString(R.string.update_log_failed)
                is MaintenanceLogViewModel.UiEvent.DismissLogFailed -> context.getString(R.string.dismiss_log_failed)
                is MaintenanceLogViewModel.UiEvent.RestoreLogFailed -> context.getString(R.string.restore_log_failed)
                is MaintenanceLogViewModel.UiEvent.DeleteReminderFailed -> "Failed to delete reminder"
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    val activeEquipments = remember(equipments) { equipments.filter { !it.dismissed }.sortedBy { it.displayOrder } }
    val activeOperationTypes = remember(operationTypes) { operationTypes.filter { !it.dismissed }.sortedBy { it.displayOrder } }

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var expandedCardId by rememberSaveable { mutableStateOf<Int?>(null) }
    var editingCardId by rememberSaveable { mutableStateOf<Int?>(null) }

    val (selectedReminder, setSelectedReminder) = remember { mutableStateOf<MaintenanceReminderDetails?>(null) }

    if (selectedReminder != null) {
        MaintenanceLogDialog(
            equipments = activeEquipments,
            operationTypes = activeOperationTypes,
            measurementUnits = measurementUnits,
            onDismissRequest = { setSelectedReminder(null) },
            onConfirm = { log ->
                viewModel.addLog(log.equipmentId, log.operationTypeId, log.notes, log.value, log.date, log.color)
                setSelectedReminder(null)
            },
            onEstimateDueDate = viewModel::estimateDueDate,
            onEstimateTargetValue = viewModel::estimateTargetValue,
            defaultEquipmentId = selectedReminder.reminder.equipmentId,
            defaultOperationTypeId = selectedReminder.reminder.operationTypeId,
            equipmentCategoryColor = equipmentColor,
            operationCategoryColor = operationColor,
            syncCalendarByDefault = syncCalendarByDefault,
            googleAccountName = googleAccountName,
            onNavigateToOptions = onNavigateToOptions
        )
    }

    MaintenanceLogScreenContent(
        logs = logs,
        equipments = activeEquipments,
        operationTypes = activeOperationTypes,
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
        onShowAddDialogChange = { 
            showAddDialog = it 
        },
        onAddLog = viewModel::addLog,
        onAddReminder = viewModel::addReminder,
        onEstimateDueDate = viewModel::estimateDueDate,
        onEstimateTargetValue = viewModel::estimateTargetValue,
        expandedCardId = expandedCardId,
        onCardExpanded = { id -> expandedCardId = if (expandedCardId == id) null else id },
        editingCardId = editingCardId,
        onEditLog = { log -> editingCardId = log.id },
        onUpdateLog = {
            viewModel.updateLog(it)
            editingCardId = null
        },
        onDismissLog = viewModel::dismissLog,
        onRestoreLog = viewModel::restoreLog,
        activeReminders = activeReminders,
        snackbarHostState = snackbarHostState,
        defaultEquipmentId = defaultEquipmentId,
        defaultOperationTypeId = defaultOperationTypeId,
        equipmentCategoryColor = equipmentColor,
        operationCategoryColor = operationColor,
        onCompleteReminder = { setSelectedReminder(it) },
        onDeleteReminder = { viewModel.deleteReminder(it) },
        syncCalendarByDefault = syncCalendarByDefault,
        googleAccountName = googleAccountName,
        onNavigateToOptions = onNavigateToOptions
    )
}

@Composable
fun RemindersDashboard(
    reminders: List<MaintenanceReminderDetails>,
    measurementUnits: List<MeasurementUnit>,
    equipmentCategoryColor: String?,
    operationCategoryColor: String?,
    onComplete: (MaintenanceReminderDetails) -> Unit,
    onDelete: (MaintenanceReminderDetails) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    
    if (reminders.isEmpty()) return

    val eColor = remember(equipmentCategoryColor) {
        try { equipmentCategoryColor?.toColorInt()?.let { Color(it) } ?: Color.Gray } catch (_: Exception) { Color.Gray }
    }
    val oColor = remember(operationCategoryColor) {
        try { operationCategoryColor?.toColorInt()?.let { Color(it) } ?: Color.Gray } catch (_: Exception) { Color.Gray }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .clickable { expanded = !expanded }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.reminders_dashboard_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        text = reminders.size.toString(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }

            if (expanded) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    reminders.forEach { reminderDetails ->
                        ReminderItem(
                            details = reminderDetails,
                            measurementUnits = measurementUnits,
                            eColor = eColor,
                            oColor = oColor,
                            onComplete = { onComplete(reminderDetails) },
                            onDelete = { onDelete(reminderDetails) }
                        )
                    }
                    Spacer(modifier = Modifier.padding(bottom = 4.dp))
                }
            }
        }
    }
}

@Composable
fun ReminderItem(
    details: MaintenanceReminderDetails,
    measurementUnits: List<MeasurementUnit>,
    eColor: Color,
    oColor: Color,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val unit = measurementUnits.find { it.id == details.unitId }
    val unitLabel = unit?.label ?: "Km"
    val decimalPlaces = unit?.decimalPlaces ?: 0

    val isOverdue = remember(details.reminder.dueDate) {
        details.reminder.dueDate?.let { it < System.currentTimeMillis() } ?: false
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ImageIcon(
                        photoUri = details.equipmentPhotoUri,
                        iconIdentifier = details.equipmentIconIdentifier,
                        modifier = Modifier.size(20.dp),
                        category = Category.EQUIPMENT,
                        borderColor = eColor,
                        contentPadding = 1.dp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = details.equipmentDescription.takeIf { it.isNotBlank() } ?: stringResource(R.string.id_no_description, details.reminder.equipmentId),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ImageIcon(
                        photoUri = details.operationTypePhotoUri,
                        iconIdentifier = details.operationTypeIconIdentifier,
                        modifier = Modifier.size(20.dp),
                        category = Category.OPERATION,
                        borderColor = oColor,
                        contentPadding = 1.dp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = details.operationTypeDescription.takeIf { it.isNotBlank() } ?: stringResource(R.string.id_no_description, details.reminder.operationTypeId),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                if (details.reminder.dueDate != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isOverdue) Icons.Default.PriorityHigh else Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.due_date_label, dateFormat.format(Date(details.reminder.dueDate))),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOverdue) MaterialTheme.colorScheme.error else Color.Unspecified
                        )
                    }
                }
                
                if (details.reminder.dueValue != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.due_value_label, String.format(Locale.US, "%.${decimalPlaces}f", details.reminder.dueValue), unitLabel),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            
            Row {
                IconButton(onClick = onComplete) {
                    Icon(Icons.Default.Done, contentDescription = stringResource(R.string.reminder_complete_log), tint = Color(0xFF4CAF50))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_reminder), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceLogScreenContent(
    logs: List<MaintenanceLogDetails>,
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
    onAddLog: (Int, Int, String?, Double?, Long, String?) -> Unit,
    onAddReminder: (Int, Int, Long?, Double?, Boolean) -> Unit,
    onEstimateDueDate: suspend (Int, Double) -> Long?,
    onEstimateTargetValue: suspend (Int, Long) -> Double?,
    expandedCardId: Int?,
    onCardExpanded: (Int) -> Unit,
    editingCardId: Int?,
    onEditLog: (MaintenanceLog) -> Unit,
    onUpdateLog: (MaintenanceLog) -> Unit,
    onDismissLog: (MaintenanceLog) -> Unit,
    onRestoreLog: (MaintenanceLog) -> Unit,
    activeReminders: List<MaintenanceReminderDetails> = emptyList(),
    snackbarHostState: SnackbarHostState,
    defaultEquipmentId: Int?,
    defaultOperationTypeId: Int?,
    equipmentCategoryColor: String?,
    operationCategoryColor: String?,
    onCompleteReminder: (MaintenanceReminderDetails) -> Unit,
    onDeleteReminder: (MaintenanceReminderDetails) -> Unit,
    syncCalendarByDefault: Boolean,
    googleAccountName: String?,
    onNavigateToOptions: () -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent, // RENDI TRASPARENTE
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
        if (showAddDialog) {
            MaintenanceLogDialog(
                equipments = equipments,
                operationTypes = operationTypes,
                measurementUnits = measurementUnits,
                onDismissRequest = { onShowAddDialogChange(false) },
                onConfirm = { log ->
                    onAddLog(log.equipmentId, log.operationTypeId, log.notes, log.value, log.date, log.color)
                    onShowAddDialogChange(false)
                },
                onSchedule = { equipmentId, opTypeId, date, value, sync ->
                    onAddReminder(equipmentId, opTypeId, date, value, sync)
                    onShowAddDialogChange(false)
                },
                onEstimateDueDate = onEstimateDueDate,
                onEstimateTargetValue = onEstimateTargetValue,
                defaultEquipmentId = defaultEquipmentId,
                defaultOperationTypeId = defaultOperationTypeId,
                equipmentCategoryColor = equipmentCategoryColor,
                operationCategoryColor = operationCategoryColor,
                syncCalendarByDefault = syncCalendarByDefault,
                googleAccountName = googleAccountName,
                onNavigateToOptions = onNavigateToOptions
            )
        }

        Column(Modifier.padding(paddingValues)) {
            RemindersDashboard(
                reminders = activeReminders,
                measurementUnits = measurementUnits,
                equipmentCategoryColor = equipmentCategoryColor,
                operationCategoryColor = operationCategoryColor,
                onComplete = onCompleteReminder,
                onDelete = onDeleteReminder
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
                        isExpanded = logDetail.log.id == expandedCardId,
                        isEditing = logDetail.log.id == editingCardId,
                        onExpand = { onCardExpanded(logDetail.log.id) },
                        onEdit = { onEditLog(logDetail.log) },
                        onSave = onUpdateLog,
                        onDismiss = { onDismissLog(logDetail.log) },
                        onRestore = { onRestoreLog(logDetail.log) },
                        equipmentCategoryColor = equipmentCategoryColor,
                        operationCategoryColor = operationCategoryColor
                    )
                }
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceLogDialog(
    equipments: List<Equipment>,
    operationTypes: List<OperationType>,
    measurementUnits: List<MeasurementUnit>,
    onDismissRequest: () -> Unit,
    onConfirm: (MaintenanceLog) -> Unit,
    onSchedule: ((Int, Int, Long?, Double?, Boolean) -> Unit)? = null,
    onEstimateDueDate: (suspend (Int, Double) -> Long?)? = null,
    onEstimateTargetValue: (suspend (Int, Long) -> Double?)? = null,
    defaultEquipmentId: Int?,
    defaultOperationTypeId: Int?,
    equipmentCategoryColor: String?,
    operationCategoryColor: String?,
    syncCalendarByDefault: Boolean = false,
    googleAccountName: String? = null,
    onNavigateToOptions: () -> Unit = {}
) {
    val dayFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Completed, 1: Planned
    var notes by remember { mutableStateOf("") }
    var valueStr by remember { mutableStateOf("") }
    var syncToCalendar by remember(syncCalendarByDefault) { mutableStateOf(syncCalendarByDefault) }
    
    var selectedEquipment by remember(defaultEquipmentId, equipments) { 
        mutableStateOf(equipments.find { it.id == defaultEquipmentId }) 
    }
    var selectedOperationType by remember(defaultOperationTypeId, operationTypes) { 
        mutableStateOf(operationTypes.find { it.id == defaultOperationTypeId }) 
    }

    val unit = remember(selectedEquipment, measurementUnits) {
        measurementUnits.find { it.id == selectedEquipment?.unitId }
    }
    val unitLabel = unit?.label ?: "Km"
    val decimalPlaces = unit?.decimalPlaces ?: 0
    
    var isEquipmentDropdownExpanded by remember { mutableStateOf(false) }
    var isOperationDropdownExpanded by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var isEstimating by remember { mutableStateOf(false) }

    val eColor = remember(equipmentCategoryColor) {
        try {
            equipmentCategoryColor?.toColorInt()?.let { Color(it) } ?: Color.Gray
        } catch (_: Exception) {
            Color.Gray
        }
    }
    val oColor = remember(operationCategoryColor) {
        try {
            operationCategoryColor?.toColorInt()?.let { Color(it) } ?: Color.Gray
        } catch (_: Exception) {
            Color.Gray
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dateMillis ->
                            val calendar = Calendar.getInstance()
                            val currentCalendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
                            calendar.timeInMillis = dateMillis
                            calendar.set(Calendar.HOUR_OF_DAY, currentCalendar.get(Calendar.HOUR_OF_DAY))
                            calendar.set(Calendar.MINUTE, currentCalendar.get(Calendar.MINUTE))
                            selectedDate = calendar.timeInMillis
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.button_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
        val timePickerState = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newCalendar = Calendar.getInstance().apply {
                            timeInMillis = selectedDate
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                        }
                        selectedDate = newCalendar.timeInMillis
                        showTimePicker = false
                    }
                ) {
                    Text(stringResource(R.string.button_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(if (onSchedule != null) stringResource(R.string.add_new_maintenance_log) else stringResource(R.string.reminder_complete_log)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onSchedule != null) {
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text(stringResource(R.string.tab_log)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text(stringResource(R.string.tab_reminder)) }
                        )
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = isEquipmentDropdownExpanded,
                    onExpandedChange = { isEquipmentDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedEquipment?.description?.takeIf { it.isNotBlank() } ?: selectedEquipment?.let { stringResource(R.string.id_no_description, it.id) } ?: stringResource(R.string.select_an_equipment),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.navigation_equipments)) },
                        leadingIcon = {
                            ImageIcon(
                                photoUri = selectedEquipment?.photoUri,
                                iconIdentifier = selectedEquipment?.iconIdentifier,
                                modifier = Modifier.size(24.dp),
                                category = Category.EQUIPMENT,
                                borderColor = eColor,
                                contentPadding = 2.dp
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isEquipmentDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isEquipmentDropdownExpanded,
                        onDismissRequest = { isEquipmentDropdownExpanded = false }
                    ) {
                        equipments.forEach { equipment ->
                            DropdownMenuItem(
                                text = { Text(equipment.description.takeIf { it.isNotBlank() } ?: stringResource(R.string.id_no_description, equipment.id)) },
                                leadingIcon = {
                                    ImageIcon(
                                        photoUri = equipment.photoUri,
                                        iconIdentifier = equipment.iconIdentifier,
                                        modifier = Modifier.size(24.dp),
                                        category = Category.EQUIPMENT,
                                        borderColor = eColor,
                                        contentPadding = 2.dp
                                    )
                                },
                                onClick = {
                                    selectedEquipment = equipment
                                    isEquipmentDropdownExpanded = false
                                    // Se l'operazione selezionata è di sistema (Reset) e l'equipment non è resettabile, resetta l'operazione
                                    if (selectedOperationType?.isSystem == true && !equipment.isResettable) {
                                        selectedOperationType = null
                                    }
                                }
                            )
                        }
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = isOperationDropdownExpanded,
                    onExpandedChange = { isOperationDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedOperationType?.description?.takeIf { it.isNotBlank() } ?: selectedOperationType?.let { stringResource(R.string.id_no_description, it.id) } ?: stringResource(R.string.select_an_operation),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.navigation_operations)) },
                        leadingIcon = {
                            ImageIcon(
                                photoUri = selectedOperationType?.photoUri,
                                iconIdentifier = selectedOperationType?.iconIdentifier,
                                modifier = Modifier.size(24.dp),
                                category = Category.OPERATION,
                                borderColor = oColor,
                                contentPadding = 2.dp
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isOperationDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isOperationDropdownExpanded,
                        onDismissRequest = { isOperationDropdownExpanded = false }
                    ) {
                        operationTypes.filter { !it.isSystem || (selectedEquipment?.isResettable == true && it.id == AppConstants.SYSTEM_OPERATION_RESET_ID) }.forEach { operation ->
                            DropdownMenuItem(
                                text = { Text(operation.description.takeIf { it.isNotBlank() } ?: stringResource(R.string.id_no_description, operation.id)) },
                                leadingIcon = {
                                    ImageIcon(
                                        photoUri = operation.photoUri,
                                        iconIdentifier = operation.iconIdentifier,
                                        modifier = Modifier.size(24.dp),
                                        category = Category.OPERATION,
                                        borderColor = oColor,
                                        contentPadding = 2.dp
                                    )
                                },
                                onClick = {
                                    selectedOperationType = operation
                                    isOperationDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = valueStr,
                    onValueChange = { input ->
                        if (input.length <= 10) {
                            val filtered = input.replace(',', '.')
                            if (filtered.isEmpty() || filtered.toDoubleOrNull() != null) {
                                val dotIndex = filtered.indexOf('.')
                                if (dotIndex == -1 || filtered.length - dotIndex - 1 <= decimalPlaces) {
                                    valueStr = filtered
                                    if (selectedTab == 1 && filtered.isNotEmpty() && onEstimateDueDate != null) {
                                        selectedEquipment?.id?.let { eqId ->
                                            filtered.toDoubleOrNull()?.let { target ->
                                                scope.launch {
                                                    isEstimating = true
                                                    onEstimateDueDate(eqId, target)?.let { estimated ->
                                                        selectedDate = estimated
                                                    }
                                                    isEstimating = false
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    label = { Text(if (selectedTab == 0) stringResource(R.string.value_optional, unitLabel) else stringResource(R.string.target_value, unitLabel)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedTab == 0) {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { if (it.length <= 200) notes = it },
                        label = { Text(stringResource(R.string.notes_optional)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dayFormat.format(Date(selectedDate)),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (selectedTab == 0) stringResource(R.string.date) else stringResource(R.string.due_date)) },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.select_date))
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = if (isEstimating) OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.primary
                        ) else OutlinedTextFieldDefaults.colors()
                    )

                    OutlinedTextField(
                        value = timeFormat.format(Date(selectedDate)),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.time)) },
                        trailingIcon = {
                            IconButton(onClick = { showTimePicker = true }) {
                                Icon(Icons.Default.AccessTime, contentDescription = stringResource(R.string.select_time))
                            }
                        },
                        modifier = Modifier.weight(1f).clickable { showTimePicker = true }
                    )
                }

                LaunchedEffect(selectedDate, selectedTab) {
                    if (selectedTab == 1 && valueStr.isEmpty() && onEstimateTargetValue != null) {
                        selectedEquipment?.id?.let { eqId ->
                            scope.launch {
                                isEstimating = true
                                onEstimateTargetValue(eqId, selectedDate)?.let { estimated ->
                                    valueStr = String.format(Locale.US, "%.${decimalPlaces}f", estimated)
                                }
                                isEstimating = false
                            }
                        }
                    }
                }

                if (selectedTab == 1) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = syncToCalendar,
                                onCheckedChange = { syncToCalendar = it }
                            )
                            Text(stringResource(R.string.sync_to_calendar))
                        }
                        if (syncToCalendar && googleAccountName == null) {
                            Text(
                                text = stringResource(R.string.calendar_no_account),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .clickable { onNavigateToOptions() }
                            )
                        }
                    }
                }
            }
        },

        confirmButton = {
            Button(
                onClick = {
                    val equipment = selectedEquipment
                    val op = selectedOperationType
                    if (equipment != null && op != null) {
                        if (selectedTab == 0) {
                            onConfirm(
                                MaintenanceLog(
                                    equipmentId = equipment.id,
                                    operationTypeId = op.id,
                                    notes = notes.takeIf { it.isNotBlank() },
                                    value = valueStr.toDoubleOrNull(),
                                    date = selectedDate
                                )
                            )
                        } else {
                            onSchedule?.invoke(
                                equipment.id,
                                op.id,
                                selectedDate,
                                valueStr.toDoubleOrNull(),
                                syncToCalendar
                            )
                        }
                    }
                }
            ) {
                Text(if (selectedTab == 0) stringResource(R.string.button_add) else stringResource(R.string.schedule_maintenance))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.button_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceLogCard(
    logDetail: MaintenanceLogDetails,
    equipments: List<Equipment>,
    operationTypes: List<OperationType>,
    measurementUnits: List<MeasurementUnit>,
    isExpanded: Boolean,
    isEditing: Boolean,
    onExpand: () -> Unit,
    onEdit: () -> Unit,
    onSave: (MaintenanceLog) -> Unit,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
    equipmentCategoryColor: String?,
    operationCategoryColor: String?
) {
    var editedNotes by remember(logDetail, isEditing) { mutableStateOf(logDetail.log.notes ?: "") }
    var editedValueStr by remember(logDetail, isEditing) { mutableStateOf(logDetail.log.value?.toString() ?: "") }
    var editedDate by remember(logDetail, isEditing) { mutableLongStateOf(logDetail.log.date) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedEquipment by remember(logDetail, isEditing) { mutableStateOf(equipments.find { it.id == logDetail.log.equipmentId }) }
    var selectedOperationType by remember(logDetail, isEditing) { mutableStateOf(operationTypes.find { it.id == logDetail.log.operationTypeId }) }
    var isEquipmentDropdownExpanded by remember { mutableStateOf(false) }
    var isOperationDropdownExpanded by remember { mutableStateOf(false) }

    val unit = remember(selectedEquipment, measurementUnits) {
        measurementUnits.find { it.id == selectedEquipment?.unitId }
    }
    val unitLabel = unit?.label ?: "Km"
    val decimalPlaces = unit?.decimalPlaces ?: 0

    val cardAlpha = if (logDetail.log.dismissed) 0.5f else 1f
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val eColor = remember(equipmentCategoryColor) {
        try {
            equipmentCategoryColor?.toColorInt()?.let { Color(it) } ?: Color.Gray
        } catch (_: Exception) {
            Color.Gray
        }
    }
    val oColor = remember(operationCategoryColor) {
        try {
            operationCategoryColor?.toColorInt()?.let { Color(it) } ?: Color.Gray
        } catch (_: Exception) {
            Color.Gray
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = editedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dateMillis ->
                            val calendar = Calendar.getInstance()
                            val currentCalendar = Calendar.getInstance().apply { timeInMillis = editedDate }
                            calendar.timeInMillis = dateMillis
                            calendar.set(Calendar.HOUR_OF_DAY, currentCalendar.get(Calendar.HOUR_OF_DAY))
                            calendar.set(Calendar.MINUTE, currentCalendar.get(Calendar.MINUTE))
                            editedDate = calendar.timeInMillis
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.button_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val calendar = Calendar.getInstance().apply { timeInMillis = editedDate }
        val timePickerState = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newCalendar = Calendar.getInstance().apply {
                            timeInMillis = editedDate
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                        }
                        editedDate = newCalendar.timeInMillis
                        showTimePicker = false
                    }
                ) {
                    Text(stringResource(R.string.button_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .graphicsLayer(alpha = cardAlpha)
            .clickable { onExpand() }
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) // Leggera trasparenza
        )
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isEditing) {
                    ExposedDropdownMenuBox(
                        expanded = isEquipmentDropdownExpanded,
                        onExpandedChange = { isEquipmentDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedEquipment?.description?.takeIf { it.isNotBlank() } ?: selectedEquipment?.let { stringResource(R.string.id_no_description, it.id) } ?: stringResource(id = R.string.select_an_equipment),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.navigation_equipments)) },
                            leadingIcon = {
                                ImageIcon(
                                    photoUri = selectedEquipment?.photoUri,
                                    iconIdentifier = selectedEquipment?.iconIdentifier,
                                    modifier = Modifier.size(24.dp),
                                    category = Category.EQUIPMENT,
                                    borderColor = eColor,
                                    contentPadding = 2.dp
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isEquipmentDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = isEquipmentDropdownExpanded,
                            onDismissRequest = { isEquipmentDropdownExpanded = false }
                        ) {
                            equipments.forEach { equipment ->
                                DropdownMenuItem(
                                    text = { Text(equipment.description.takeIf { it.isNotBlank() } ?: stringResource(R.string.id_no_description, equipment.id)) },
                                    leadingIcon = {
                                        ImageIcon(
                                            photoUri = equipment.photoUri,
                                            iconIdentifier = equipment.iconIdentifier,
                                            modifier = Modifier.size(24.dp),
                                            category = Category.EQUIPMENT,
                                            borderColor = eColor,
                                            contentPadding = 2.dp
                                        )
                                    },
                                    onClick = {
                                        selectedEquipment = equipment
                                        isEquipmentDropdownExpanded = false
                                        // Se l'operazione selezionata è di sistema (Reset) e l'equipment non è resettabile, resetta l'operazione
                                        if (selectedOperationType?.isSystem == true && !equipment.isResettable) {
                                            selectedOperationType = null
                                        }
                                    }
                                )
                            }
                        }
                    }
                    ExposedDropdownMenuBox(
                        expanded = isOperationDropdownExpanded,
                        onExpandedChange = { isOperationDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedOperationType?.description?.takeIf { it.isNotBlank() } ?: selectedOperationType?.let { stringResource(R.string.id_no_description, it.id) } ?: stringResource(id = R.string.select_an_operation),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.navigation_operations)) },
                            leadingIcon = {
                                ImageIcon(
                                    photoUri = selectedOperationType?.photoUri,
                                    iconIdentifier = selectedOperationType?.iconIdentifier,
                                    modifier = Modifier.size(24.dp),
                                    category = Category.OPERATION,
                                    borderColor = oColor,
                                    contentPadding = 2.dp
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isOperationDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = isOperationDropdownExpanded,
                            onDismissRequest = { isOperationDropdownExpanded = false }
                        ) {
                            operationTypes.filter { !it.isSystem || (selectedEquipment?.isResettable == true && it.id == AppConstants.SYSTEM_OPERATION_RESET_ID) }.forEach { operation ->
                                DropdownMenuItem(
                                    text = { Text(operation.description.takeIf { it.isNotBlank() } ?: stringResource(R.string.id_no_description, operation.id)) },
                                    leadingIcon = {
                                        ImageIcon(
                                            photoUri = operation.photoUri,
                                            iconIdentifier = operation.iconIdentifier,
                                            modifier = Modifier.size(24.dp),
                                            category = Category.OPERATION,
                                            borderColor = oColor,
                                            contentPadding = 2.dp
                                        )
                                    },
                                    onClick = {
                                        selectedOperationType = operation
                                        isOperationDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = editedValueStr,
                        onValueChange = { input ->
                            if (input.length <= 10) {
                                val filtered = input.replace(',', '.')
                                if (filtered.isEmpty() || filtered.toDoubleOrNull() != null) {
                                    val dotIndex = filtered.indexOf('.')
                                    if (dotIndex == -1 || filtered.length - dotIndex - 1 <= decimalPlaces) {
                                        editedValueStr = filtered
                                    }
                                }
                            }
                        },
                        label = { Text("$unitLabel (optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editedNotes,
                        onValueChange = { if (it.length <= 200) editedNotes = it },
                        label = { Text(stringResource(R.string.notes_optional)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                            Text(text = dayFormat.format(Date(editedDate)))
                        }
                        Button(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f)) {
                            Text(text = timeFormat.format(Date(editedDate)))
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val equipmentTextAlpha = if (logDetail.equipmentDismissed) 0.5f else 1f
                        ImageIcon(
                            photoUri = logDetail.equipmentPhotoUri,
                            iconIdentifier = logDetail.equipmentIconIdentifier,
                            modifier = Modifier.size(24.dp).graphicsLayer(alpha = equipmentTextAlpha),
                            category = Category.EQUIPMENT,
                            borderColor = eColor,
                            contentPadding = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = (logDetail.equipmentDescription.takeIf { it.isNotBlank() } ?: stringResource(R.string.id_no_description, logDetail.log.equipmentId)) + if (logDetail.equipmentDismissed) " " + stringResource(R.string.dismissed_suffix) else "",
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.graphicsLayer(alpha = equipmentTextAlpha)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val operationTypeAlpha = if (logDetail.operationTypeDismissed) 0.5f else 1f
                        ImageIcon(
                            photoUri = logDetail.operationTypePhotoUri,
                            iconIdentifier = logDetail.operationTypeIconIdentifier,
                            modifier = Modifier.size(24.dp).graphicsLayer(alpha = operationTypeAlpha),
                            category = Category.OPERATION,
                            borderColor = oColor,
                            contentPadding = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = (logDetail.operationTypeDescription.takeIf { it.isNotBlank() } ?: stringResource(R.string.id_no_description, logDetail.log.operationTypeId)) + if (logDetail.operationTypeDismissed) " " + stringResource(R.string.dismissed_suffix) else "",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.graphicsLayer(alpha = operationTypeAlpha)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isValueIncongruent = remember(logDetail) {
                            val currentVal = logDetail.log.value ?: 0.0
                            val prevVal = logDetail.previousLogValue ?: 0.0
                            !logDetail.operationTypeIsSystem && !logDetail.previousLogIsSystem && currentVal < prevVal
                        }
                        Text(
                            text = logDetail.log.value?.let { String.format(Locale.US, "%.${decimalPlaces}f %s", it, unitLabel) } ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isValueIncongruent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = dateFormat.format(Date(logDetail.log.date)),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    logDetail.log.notes?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Column {
                IconButton(onClick = {
                    if (isEditing) {
                        val updatedLog = logDetail.log.copy(
                            notes = editedNotes,
                            value = editedValueStr.toDoubleOrNull(),
                            date = editedDate,
                            equipmentId = selectedEquipment?.id ?: logDetail.log.equipmentId,
                            operationTypeId = selectedOperationType?.id ?: logDetail.log.operationTypeId
                        )
                        onSave(updatedLog)
                    } else {
                        onEdit()
                    }
                }) {
                    Icon(
                        imageVector = if (isEditing) Icons.Filled.Done else Icons.Filled.Edit,
                        contentDescription = if (isEditing) stringResource(R.string.save_log) else stringResource(R.string.edit_log)
                    )
                }
                if (isEditing) {
                    IconButton(
                        onClick = {
                            if (logDetail.log.dismissed) {
                                onRestore()
                            } else {
                                onDismiss()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (logDetail.log.dismissed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (logDetail.log.dismissed) stringResource(R.string.restore_log) else stringResource(R.string.dismiss_log)
                        )
                    }
                }
            }
        }
    }
}
