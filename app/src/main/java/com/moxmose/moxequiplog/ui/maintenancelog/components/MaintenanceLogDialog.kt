package com.moxmose.moxequiplog.ui.maintenancelog.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.*
import com.moxmose.moxequiplog.ui.components.CommonActionButtons
import com.moxmose.moxequiplog.ui.components.ImageIcon
import com.moxmose.moxequiplog.utils.AppConstants
import com.moxmose.moxequiplog.utils.UiConstants
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceLogDialog(
    equipments: List<Equipment>,
    operationTypes: List<OperationType>,
    measurementUnits: List<MeasurementUnit>,
    allSections: List<Section>,
    onDismissRequest: () -> Unit,
    onConfirm: (MaintenanceLog) -> Unit,
    onSchedule: ((Int, Int, Long?, Double?, Boolean) -> Unit)? = null,
    onDeleteReminder: (() -> Unit)? = null,
    onDeleteLog: (() -> Unit)? = null,
    onArchive: (() -> Unit)? = null,
    isDismissed: Boolean = false,
    onEstimateDueDate: (suspend (Int, Double) -> Long?)? = null,
    onEstimateTargetValue: (suspend (Int, Long) -> Double?)? = null,
    onGetOperationCostStats: (suspend (Int) -> Pair<Double?, Double?>)? = null,
    defaultEquipmentId: Int?,
    defaultOperationTypeId: Int?,
    initialDate: Long = System.currentTimeMillis(),
    initialValue: String = "",
    initialCost: String = "",
    initialIsUnplanned: Boolean = false,
    initialSyncToCalendar: Boolean? = null,
    initialHasFixedDate: Boolean = true,
    isEditMode: Boolean = false,
    equipmentCategoryColor: String?,
    operationCategoryColor: String?,
    syncCalendarByDefault: Boolean = false,
    googleAccountName: String? = null,
    costTrendThreshold: Float = UiConstants.DEFAULT_COST_TREND_THRESHOLD,
    initialTab: Int? = null,
    onNavigateToOptions: () -> Unit = {},
    logDraft: MaintenanceLog? = null,
    reminderDraft: MaintenanceReminder? = null,
    onUpdateLogDraft: (MaintenanceLog) -> Unit = {},
    onUpdateReminderDraft: (MaintenanceReminder) -> Unit = {}
) {
    val dayFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    var selectedTab by remember(isEditMode, initialTab) { 
        mutableIntStateOf(initialTab ?: if (isEditMode) 1 else 0) 
    } // 0: Completed, 1: Planned

    // Use drafts if present
    val currentLog = logDraft ?: MaintenanceLog(
        equipmentId = defaultEquipmentId ?: 0,
        operationTypeId = defaultOperationTypeId ?: 0,
        date = initialDate,
        value = initialValue.toDoubleOrNull(),
        cost = initialCost.toDoubleOrNull(),
        isUnplanned = initialIsUnplanned
    )
    val currentReminder = reminderDraft ?: MaintenanceReminder(
        equipmentId = defaultEquipmentId ?: 0,
        operationTypeId = defaultOperationTypeId ?: 0,
        dueDate = if (initialHasFixedDate) initialDate else null,
        dueValue = initialValue.toDoubleOrNull()
    )

    var notes by remember(currentLog.notes) { mutableStateOf(currentLog.notes ?: "") }
    var valueStr by remember(currentLog.value, currentReminder.dueValue) { 
        mutableStateOf(if (selectedTab == 0) currentLog.value?.toString() ?: initialValue else currentReminder.dueValue?.toString() ?: initialValue) 
    }
    var costStr by remember(currentLog.cost) { mutableStateOf(currentLog.cost?.toString() ?: initialCost) }
    var isUnplanned by remember(currentLog.isUnplanned) { mutableStateOf(currentLog.isUnplanned) }
    var syncToCalendar by remember(syncCalendarByDefault, initialSyncToCalendar) { 
        mutableStateOf(initialSyncToCalendar ?: syncCalendarByDefault) 
    }
    
    var selectedEquipment by remember(currentLog.equipmentId, currentReminder.equipmentId, equipments) { 
        val id = if (selectedTab == 0) currentLog.equipmentId else currentReminder.equipmentId
        mutableStateOf(equipments.find { it.id == (if (id == 0) defaultEquipmentId else id) } ?: equipments.find { it.id == defaultEquipmentId }) 
    }
    var selectedOperationType by remember(currentLog.operationTypeId, currentReminder.operationTypeId, operationTypes) { 
        val id = if (selectedTab == 0) currentLog.operationTypeId else currentReminder.operationTypeId
        mutableStateOf(operationTypes.find { it.id == (if (id == 0) defaultOperationTypeId else id) } ?: operationTypes.find { it.id == defaultOperationTypeId }) 
    }

    val filteredEquipments = remember(selectedOperationType, equipments) {
        if (selectedOperationType == null) equipments
        else equipments.filter { it.sectionId == selectedOperationType?.sectionId || it.sectionId == AppConstants.DEFAULT_SECTION_ID || selectedOperationType?.sectionId == AppConstants.DEFAULT_SECTION_ID }
    }

    val filteredOperationTypes = remember(selectedEquipment, operationTypes) {
        if (selectedEquipment == null) operationTypes
        else operationTypes.filter { it.sectionId == selectedEquipment?.sectionId || it.sectionId == AppConstants.DEFAULT_SECTION_ID || selectedEquipment?.sectionId == AppConstants.DEFAULT_SECTION_ID }
    }

    val unit = remember(selectedEquipment, measurementUnits) {
        measurementUnits.find { it.id == selectedEquipment?.unitId }
    }
    val unitLabel = unit?.label ?: "Km"
    val decimalPlaces = unit?.decimalPlaces ?: 0
    
    var isEquipmentDropdownExpanded by remember { mutableStateOf(false) }
    var isOperationDropdownExpanded by remember { mutableStateOf(false) }
    var selectedDate by remember(currentLog.date, currentReminder.dueDate) { 
        mutableLongStateOf(if (selectedTab == 0) currentLog.date else currentReminder.dueDate ?: initialDate) 
    }
    var hasFixedDate by remember(currentReminder.dueDate, initialHasFixedDate) { 
        mutableStateOf(if (selectedTab == 1) currentReminder.dueDate != null else initialHasFixedDate) 
    }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteReminderConfirmation by remember { mutableStateOf(false) }
    var resetAfter by remember(currentLog.resetAfter) { mutableStateOf(currentLog.resetAfter) }

    var lastCost by remember { mutableStateOf<Double?>(null) }
    var avgCost by remember { mutableStateOf<Double?>(null) }

    // Helper to update drafts
    val updateLogDraft = {
        onUpdateLogDraft(currentLog.copy(
            equipmentId = selectedEquipment?.id ?: 0,
            operationTypeId = selectedOperationType?.id ?: 0,
            notes = notes,
            value = valueStr.toDoubleOrNull(),
            cost = costStr.toDoubleOrNull(),
            isUnplanned = isUnplanned,
            date = selectedDate,
            resetAfter = resetAfter
        ))
    }
    val updateReminderDraft = {
        onUpdateReminderDraft(currentReminder.copy(
            equipmentId = selectedEquipment?.id ?: 0,
            operationTypeId = selectedOperationType?.id ?: 0,
            dueDate = if (hasFixedDate) selectedDate else null,
            dueValue = valueStr.toDoubleOrNull()
        ))
    }

    LaunchedEffect(selectedOperationType) {
        selectedOperationType?.id?.let { opId ->
            onGetOperationCostStats?.invoke(opId)?.let { (last, avg) ->
                lastCost = last
                avgCost = avg
            }
        } ?: run {
            lastCost = null
            avgCost = null
        }
    }

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
                            if (selectedTab == 0) updateLogDraft() else updateReminderDraft()
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
                        if (selectedTab == 0) updateLogDraft() else updateReminderDraft()
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

    if (showDeleteReminderConfirmation) {
        val isLog = onDeleteLog != null
        AlertDialog(
            onDismissRequest = { showDeleteReminderConfirmation = false },
            title = { Text(stringResource(if (isLog) R.string.delete_log else R.string.delete_reminder)) },
            text = { Text(stringResource(if (isLog) R.string.delete_log_confirm else R.string.delete_reminder_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteReminder?.invoke()
                        onDeleteLog?.invoke()
                        showDeleteReminderConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.button_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteReminderConfirmation = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { 
            Text(
                if (isEditMode && onSchedule != null) stringResource(R.string.edit_log)
                else if (isEditMode) stringResource(R.string.edit_log)
                else if (onSchedule != null) stringResource(R.string.add_new_maintenance_log) 
                else stringResource(R.string.reminder_complete_log)
            ) 
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onSchedule != null && !isEditMode) {
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
                        label = { Text(stringResource(R.string.navigation_equipment)) },
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
                        filteredEquipments.forEach { equipment ->
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = equipment.description.takeIf { it.isNotBlank() } ?: stringResource(R.string.id_no_description, equipment.id),
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        allSections.find { it.id == equipment.sectionId }?.let { section ->
                                            val sectionName = if (section.id == AppConstants.DEFAULT_SECTION_ID) stringResource(R.string.section_common) else section.name
                                            val sColor = try { section.color?.toColorInt()?.let { Color(it) } ?: Color.Gray } catch (_: Exception) { Color.Gray }
                                            Surface(
                                                shape = CircleShape,
                                                color = sColor.copy(alpha = 0.15f),
                                                border = BorderStroke(1.dp, sColor.copy(alpha = 0.5f)),
                                                modifier = Modifier.padding(start = 8.dp)
                                            ) {
                                                Text(
                                                    text = sectionName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = sColor,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                },
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
                                    if (selectedOperationType?.isSystem == true && !equipment.isResettable) {
                                        selectedOperationType = null
                                    }
                                    if (!equipment.isResettable) {
                                        resetAfter = false
                                    }
                                    if (selectedTab == 0) updateLogDraft() else updateReminderDraft()
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
                        filteredOperationTypes.filter { !it.isSystem || (selectedEquipment?.isResettable == true && it.id == AppConstants.SYSTEM_OPERATION_RESET_ID) }.forEach { operation ->
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = operation.description.takeIf { it.isNotBlank() } ?: stringResource(R.string.id_no_description, operation.id),
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        allSections.find { it.id == operation.sectionId }?.let { section ->
                                            val sectionName = if (section.id == AppConstants.DEFAULT_SECTION_ID) stringResource(R.string.section_common) else section.name
                                            val sColor = try { section.color?.toColorInt()?.let { Color(it) } ?: Color.Gray } catch (_: Exception) { Color.Gray }
                                            Surface(
                                                shape = CircleShape,
                                                color = sColor.copy(alpha = 0.15f),
                                                border = BorderStroke(1.dp, sColor.copy(alpha = 0.5f)),
                                                modifier = Modifier.padding(start = 8.dp)
                                            ) {
                                                Text(
                                                    text = sectionName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = sColor,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                },
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
                                    if (operation.id == AppConstants.SYSTEM_OPERATION_RESET_ID) {
                                        resetAfter = true
                                        valueStr = "0"
                                    }
                                    if (selectedTab == 0) updateLogDraft() else updateReminderDraft()
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = valueStr,
                    onValueChange = { input ->
                        val filtered = input.replace(',', '.')
                        if (filtered.isEmpty() || filtered == "." || filtered == "-") {
                            valueStr = filtered
                        } else {
                            val doubleVal = filtered.toDoubleOrNull()
                            if (doubleVal != null && filtered.length <= 10) {
                                val dotIndex = filtered.indexOf('.')
                                if (dotIndex == -1 || filtered.length - dotIndex - 1 <= decimalPlaces) {
                                    valueStr = filtered
                                }
                            }
                        }
                        if (selectedTab == 0) updateLogDraft() else updateReminderDraft()
                    },
                    label = { Text(if (selectedTab == 0) stringResource(R.string.value_optional, unitLabel) else stringResource(R.string.target_value, unitLabel)) },
                    readOnly = selectedOperationType?.id == AppConstants.SYSTEM_OPERATION_RESET_ID,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    trailingIcon = {
                        if (selectedTab == 1 && valueStr.isNotBlank() && onEstimateDueDate != null) {
                            IconButton(onClick = {
                                selectedEquipment?.id?.let { eqId ->
                                    valueStr.toDoubleOrNull()?.let { target ->
                                        scope.launch {
                                            isEstimating = true
                                            onEstimateDueDate(eqId, target)?.let { estimated ->
                                                selectedDate = estimated
                                                updateReminderDraft()
                                            }
                                            isEstimating = false
                                        }
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Recalculate Date")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedTab == 0) {
                    val isResettable = selectedEquipment?.isResettable == true
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isResettable && selectedOperationType?.id != AppConstants.SYSTEM_OPERATION_RESET_ID) { 
                                resetAfter = !resetAfter
                                if (resetAfter) valueStr = "0"
                                updateLogDraft()
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = resetAfter && isResettable,
                            onCheckedChange = { 
                                resetAfter = it 
                                if (it) valueStr = "0"
                                updateLogDraft()
                            },
                            enabled = isResettable && selectedOperationType?.id != AppConstants.SYSTEM_OPERATION_RESET_ID
                        )
                        Text(
                            text = stringResource(R.string.reset_counter_after),
                            color = if (isResettable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }

                    OutlinedTextField(
                        value = costStr,
                        onValueChange = { input ->
                            val filtered = input.replace(',', '.')
                            if (filtered.isEmpty() || filtered == ".") {
                                costStr = filtered
                            } else {
                                val doubleVal = filtered.toDoubleOrNull()
                                if (doubleVal != null && filtered.length <= 10) {
                                    costStr = filtered
                                }
                            }
                            updateLogDraft()
                        },
                        label = { Text(stringResource(R.string.cost_optional)) },
                        supportingText = {
                            if (lastCost != null && avgCost != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(R.string.last_cost_label, String.format(Locale.US, "%.2f €", lastCost)),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    val (icon, color) = when {
                                        lastCost!! > avgCost!! * (1 + costTrendThreshold) -> Icons.AutoMirrored.Filled.TrendingUp to Color.Red
                                        lastCost!! < avgCost!! * (1 - costTrendThreshold) -> Icons.AutoMirrored.Filled.TrendingDown to Color.Green
                                        else -> Icons.AutoMirrored.Filled.TrendingFlat to Color.Gray
                                    }
                                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { 
                            isUnplanned = !isUnplanned 
                            updateLogDraft()
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isUnplanned,
                            onCheckedChange = { 
                                isUnplanned = it 
                                updateLogDraft()
                            }
                        )
                        Text(stringResource(R.string.unplanned_intervention))
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { 
                            if (it.length <= 200) {
                                notes = it 
                                updateLogDraft()
                            }
                        },
                        label = { Text(stringResource(R.string.notes_optional)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (selectedTab == 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            hasFixedDate = !hasFixedDate
                            updateReminderDraft()
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = hasFixedDate,
                            onCheckedChange = { 
                                hasFixedDate = it 
                                updateReminderDraft()
                            }
                        )
                        Text(stringResource(R.string.set_fixed_due_date))
                    }
                }

                if (selectedTab == 0 || hasFixedDate) {
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
                }

                LaunchedEffect(selectedDate, selectedTab) {
                    if (selectedTab == 1 && valueStr.isEmpty() && onEstimateTargetValue != null) {
                        selectedEquipment?.id?.let { eqId ->
                            scope.launch {
                                isEstimating = true
                                onEstimateTargetValue(eqId, selectedDate)?.let { estimated ->
                                    valueStr = String.format(Locale.US, "%.${decimalPlaces}f", estimated)
                                    updateReminderDraft()
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
            CommonActionButtons(
                onConfirm = {
                    val equipment = selectedEquipment
                    val op = selectedOperationType
                    val targetValue = valueStr.toDoubleOrNull()
                    val fixedDate = if (hasFixedDate) selectedDate else null
                    
                    if (equipment != null && op != null && (selectedTab == 0 || fixedDate != null || targetValue != null)) {
                        if (selectedTab == 0) {
                            onConfirm(
                                MaintenanceLog(
                                    equipmentId = equipment.id,
                                    operationTypeId = op.id,
                                    notes = notes.takeIf { it.isNotBlank() },
                                    value = valueStr.toDoubleOrNull(),
                                    date = selectedDate,
                                    resetAfter = resetAfter,
                                    cost = costStr.toDoubleOrNull(),
                                    isUnplanned = isUnplanned
                                )
                            )
                        } else {
                            onSchedule?.invoke(
                                equipment.id,
                                op.id,
                                fixedDate,
                                targetValue,
                                syncToCalendar
                            )
                        }
                    }
                },
                onDismiss = onDismissRequest,
                confirmText = if (isEditMode) stringResource(R.string.save_operation_type)
                             else if (selectedTab == 0) stringResource(R.string.button_add) 
                             else stringResource(R.string.schedule_maintenance),
                confirmIcon = if (isEditMode) Icons.Default.Save else Icons.Default.Add,
                confirmEnabled = selectedEquipment != null && selectedOperationType != null && 
                                (selectedTab == 0 || hasFixedDate || valueStr.isNotBlank()),
                showDelete = isEditMode && (onDeleteReminder != null || onDeleteLog != null),
                onDelete = { showDeleteReminderConfirmation = true },
                showArchive = isEditMode && onArchive != null,
                onArchive = onArchive,
                archiveIcon = if (isDismissed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                compactMode = true
            )
        },
        dismissButton = null
    )
}
