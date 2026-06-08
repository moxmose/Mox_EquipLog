package com.moxmose.moxequiplog.ui.maintenancelog.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.*
import com.moxmose.moxequiplog.ui.components.ImageIcon
import com.moxmose.moxequiplog.utils.AppConstants
import java.text.SimpleDateFormat
import java.util.*

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
    onDelete: (MaintenanceLog) -> Unit,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
    onGetOperationCostStats: suspend (Int) -> Pair<Double?, Double?>,
    modifier: Modifier = Modifier,
    equipmentCategoryColor: String?,
    operationCategoryColor: String?,
    costTrendThreshold: Float
) {
    var editedNotes by remember(logDetail, isEditing) { mutableStateOf(logDetail.log.notes ?: "") }
    var editedValueStr by remember(logDetail, isEditing) { mutableStateOf(logDetail.log.value?.toString() ?: "") }
    var editedCostStr by remember(logDetail, isEditing) { mutableStateOf(logDetail.log.cost?.toString() ?: "") }
    var editedIsUnplanned by remember(logDetail, isEditing) { mutableStateOf(logDetail.log.isUnplanned) }
    var editedResetAfter by remember(logDetail, isEditing) { mutableStateOf(logDetail.log.resetAfter) }
    var editedDate by remember(logDetail, isEditing) { mutableLongStateOf(logDetail.log.date) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedEquipment by remember(logDetail, isEditing) { mutableStateOf(equipments.find { it.id == logDetail.log.equipmentId }) }
    var selectedOperationType by remember(logDetail, isEditing) { mutableStateOf(operationTypes.find { it.id == logDetail.log.operationTypeId }) }
    var isEquipmentDropdownExpanded by remember { mutableStateOf(false) }
    var isOperationDropdownExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    var lastCost by remember { mutableStateOf<Double?>(null) }
    var avgCost by remember { mutableStateOf<Double?>(null) }

    LaunchedEffect(selectedOperationType) {
        if (isEditing) {
            selectedOperationType?.id?.let { opId ->
                onGetOperationCostStats(opId).let { (last, avg) ->
                    lastCost = last
                    avgCost = avg
                }
            } ?: run {
                lastCost = null
                avgCost = null
            }
        }
    }

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

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.delete_log)) },
            text = { Text(stringResource(R.string.delete_log_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(logDetail.log)
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.button_delete).ifEmpty { "Delete" })
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
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
                                        if (selectedOperationType?.isSystem == true && !equipment.isResettable) {
                                            selectedOperationType = null
                                        }
                                        if (!equipment.isResettable) {
                                            editedResetAfter = false
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
                                        if (operation.id == AppConstants.SYSTEM_OPERATION_RESET_ID) {
                                            editedResetAfter = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = editedValueStr,
                        onValueChange = { input ->
                            val filtered = input.replace(',', '.')
                            if (filtered.isEmpty() || filtered == "." || filtered == "-") {
                                editedValueStr = filtered
                            } else {
                                val doubleVal = filtered.toDoubleOrNull()
                                if (doubleVal != null && filtered.length <= 10) {
                                    val dotIndex = filtered.indexOf('.')
                                    if (dotIndex == -1 || filtered.length - dotIndex - 1 <= decimalPlaces) {
                                        editedValueStr = filtered
                                    }
                                }
                            }
                        },
                        label = { Text(stringResource(R.string.value_optional, unitLabel)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    val isResettable = selectedEquipment?.isResettable == true
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isResettable) { editedResetAfter = !editedResetAfter },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = editedResetAfter && isResettable,
                            onCheckedChange = { editedResetAfter = it },
                            enabled = isResettable
                        )
                        Text(
                            text = stringResource(R.string.reset_counter_after),
                            color = if (isResettable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }

                    OutlinedTextField(
                        value = editedNotes,
                        onValueChange = { if (it.length <= 200) editedNotes = it },
                        label = { Text(stringResource(R.string.notes_optional)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editedCostStr,
                        onValueChange = { input ->
                            val filtered = input.replace(',', '.')
                            if (filtered.isEmpty() || filtered == ".") {
                                editedCostStr = filtered
                            } else {
                                val doubleVal = filtered.toDoubleOrNull()
                                if (doubleVal != null && filtered.length <= 10) {
                                    editedCostStr = filtered
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
                        modifier = Modifier.fillMaxWidth().clickable { editedIsUnplanned = !editedIsUnplanned },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = editedIsUnplanned,
                            onCheckedChange = { editedIsUnplanned = it }
                        )
                        Text(stringResource(R.string.unplanned_intervention))
                    }
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
                            modifier = Modifier.graphicsLayer(alpha = operationTypeAlpha).weight(1f)
                        )
                        if (logDetail.log.isUnplanned) {
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.unplanned_intervention),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = logDetail.log.value?.let { String.format(Locale.US, "%.${decimalPlaces}f %s", it, unitLabel) } ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isValueIncongruent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            logDetail.log.cost?.let {
                                Text(
                                    text = String.format(Locale.US, "%.2f €", it),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
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
                            cost = editedCostStr.toDoubleOrNull(),
                            isUnplanned = editedIsUnplanned,
                            resetAfter = editedResetAfter,
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
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete_log),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
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
