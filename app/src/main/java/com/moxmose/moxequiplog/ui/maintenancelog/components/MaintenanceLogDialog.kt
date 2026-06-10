package com.moxmose.moxequiplog.ui.maintenancelog.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
    onNavigateToOptions: () -> Unit = {}
) {
    val dayFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    var selectedTab by remember(isEditMode, initialTab) { 
        mutableIntStateOf(initialTab ?: if (isEditMode) 1 else 0) 
    } // 0: Completed, 1: Planned
    var notes by remember { mutableStateOf("") }
    var valueStr by remember { mutableStateOf(initialValue) }
    var costStr by remember { mutableStateOf(initialCost) }
    var isUnplanned by remember { mutableStateOf(initialIsUnplanned) }
    var syncToCalendar by remember(syncCalendarByDefault, initialSyncToCalendar) { 
        mutableStateOf(initialSyncToCalendar ?: syncCalendarByDefault) 
    }
    
    var selectedEquipment by remember(defaultEquipmentId, equipments) { 
        mutableStateOf(equipments.find { it.id == defaultEquipmentId }) 
    }
    var selectedOperationType by remember(defaultOperationTypeId, operationTypes) { 
        mutableStateOf(operationTypes.find { it.id == defaultOperationTypeId }) 
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
    var selectedDate by remember { mutableLongStateOf(initialDate) }
    var hasFixedDate by remember(isEditMode, initialHasFixedDate) { 
        mutableStateOf(if (isEditMode) initialHasFixedDate else true) 
    }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteReminderConfirmation by remember { mutableStateOf(false) }
    var resetAfter by remember { mutableStateOf(false) }

    var lastCost by remember { mutableStateOf<Double?>(null) }
    var avgCost by remember { mutableStateOf<Double?>(null) }

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

    if (showDeleteReminderConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteReminderConfirmation = false },
            title = { Text(stringResource(R.string.delete_reminder)) },
            text = { Text("Are you sure you want to permanently delete this reminder?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteReminder?.invoke()
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
                                            Text(
                                                text = " ($sectionName)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.padding(start = 4.dp)
                                            )
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
                                            Text(
                                                text = " ($sectionName)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.padding(start = 4.dp)
                                            )
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
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = resetAfter && isResettable,
                            onCheckedChange = { 
                                resetAfter = it 
                                if (it) valueStr = "0"
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
                        modifier = Modifier.fillMaxWidth().clickable { isUnplanned = !isUnplanned },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isUnplanned,
                            onCheckedChange = { isUnplanned = it }
                        )
                        Text(stringResource(R.string.unplanned_intervention))
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { if (it.length <= 200) notes = it },
                        label = { Text(stringResource(R.string.notes_optional)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (selectedTab == 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = hasFixedDate,
                            onCheckedChange = { hasFixedDate = it }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isEditMode && onDeleteReminder != null) Arrangement.SpaceBetween else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEditMode && onDeleteReminder != null) {
                    TextButton(
                        onClick = { showDeleteReminderConfirmation = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.delete_reminder))
                    }
                }
                
                Button(
                    onClick = {
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
                    enabled = selectedEquipment != null && selectedOperationType != null && 
                            (selectedTab == 0 || hasFixedDate || valueStr.isNotBlank())
                ) {
                    Text(
                        if (isEditMode) stringResource(R.string.save_operation_type)
                        else if (selectedTab == 0) stringResource(R.string.button_add) 
                        else stringResource(R.string.schedule_maintenance)
                    )
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.button_cancel))
            }
        }
    )
}
