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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.*
import com.moxmose.moxequiplog.ui.components.CommonActionButtons
import com.moxmose.moxequiplog.ui.components.ImageIcon
import com.moxmose.moxequiplog.ui.components.SectionBadge
import com.moxmose.moxequiplog.ui.components.SelectionDropdown
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
    allSections: List<Section>,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onSave: (MaintenanceLog) -> Unit,
    onDelete: (MaintenanceLog) -> Unit,
    onGetOperationCostStats: suspend (Int) -> Pair<Double?, Double?>,
    modifier: Modifier = Modifier,
    equipmentCategoryColor: String?,
    operationCategoryColor: String?,
    costTrendThreshold: Float,
    draft: MaintenanceLog? = null,
    onStartEdit: () -> Unit = {},
    onCancelEdit: () -> Unit = {},
    onUpdateDraft: (MaintenanceLog) -> Unit = {},
    onSaveEdit: (MaintenanceLog) -> Unit = {}
) {
    val currentLog = draft ?: logDetail.log
    val actualIsEditing = draft != null

    var editedNotes by remember(currentLog.notes) { mutableStateOf(currentLog.notes ?: "") }
    var editedValueStr by remember(currentLog.value) { mutableStateOf(currentLog.value?.toString() ?: "") }
    var editedCostStr by remember(currentLog.cost) { mutableStateOf(currentLog.cost?.toString() ?: "") }
    var editedIsUnplanned by remember(currentLog.isUnplanned) { mutableStateOf(currentLog.isUnplanned) }
    var editedResetAfter by remember(currentLog.resetAfter) { mutableStateOf(currentLog.resetAfter) }
    var editedDismissed by remember(currentLog.dismissed) { mutableStateOf(currentLog.dismissed) }
    var editedDate by remember(currentLog.date) { mutableLongStateOf(currentLog.date) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedEquipment by remember(currentLog.equipmentId) { mutableStateOf(equipments.find { it.id == currentLog.equipmentId }) }
    var selectedOperationType by remember(currentLog.operationTypeId) { mutableStateOf(operationTypes.find { it.id == currentLog.operationTypeId }) }
    
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    var lastCost by remember { mutableStateOf<Double?>(null) }
    var avgCost by remember { mutableStateOf<Double?>(null) }

    // Helper to update draft
    val updateDraft = { updated: MaintenanceLog -> if (actualIsEditing) onUpdateDraft(updated) }

    LaunchedEffect(selectedOperationType) {
        if (actualIsEditing) {
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

    val unit = remember(actualIsEditing, selectedEquipment, selectedOperationType, logDetail, measurementUnits) {
        val unitId = if (actualIsEditing) {
            if (selectedOperationType?.id == AppConstants.SYSTEM_OPERATION_RESET_ID) {
                selectedEquipment?.unitId
            } else {
                selectedOperationType?.unitId ?: selectedEquipment?.unitId
            }
        } else {
            if (logDetail.log.operationTypeId == AppConstants.SYSTEM_OPERATION_RESET_ID) {
                logDetail.equipmentUnitId
            } else {
                logDetail.operationTypeUnitId
            }
        }
        measurementUnits.find { it.id == unitId } ?: measurementUnits.find { it.id == (if (actualIsEditing) selectedEquipment?.unitId else logDetail.equipmentUnitId) }
    }
    val unitLabel = unit?.label ?: "Km"
    val decimalPlaces = unit?.decimalPlaces ?: 0

    val cardAlpha = if (currentLog.dismissed) 0.5f else 1f
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
                            draft?.let { updateDraft(it.copy(date = editedDate)) }
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
                        draft?.let { updateDraft(it.copy(date = editedDate)) }
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
                if (actualIsEditing && draft != null) {
                    SelectionDropdown(
                        label = stringResource(R.string.navigation_equipment),
                        selectedItem = selectedEquipment,
                        items = equipments,
                        allSections = allSections,
                        onItemSelected = { equipment ->
                            selectedEquipment = equipment
                            if (selectedOperationType?.isSystem == true && selectedOperationType?.id == AppConstants.SYSTEM_OPERATION_RESET_ID) {
                                // Reset system operation is always allowed if selectedEquipment is not null
                                updateDraft(draft.copy(equipmentId = equipment.id))
                            } else {
                                updateDraft(draft.copy(equipmentId = equipment.id))
                            }
                            // Reset is allowed only if operation type allows it
                            if (selectedOperationType?.isResettable != true) {
                                editedResetAfter = false
                                updateDraft(draft.copy(resetAfter = false))
                            }
                        },
                        itemDescription = { it.description.takeIf { d -> d.isNotBlank() } ?: stringResource(R.string.id_no_description, it.id) },
                        itemPhotoUri = { it.photoUri },
                        itemIconIdentifier = { it.iconIdentifier },
                        itemSectionId = { it.sectionId },
                        category = Category.EQUIPMENT,
                        categoryColor = eColor,
                        placeholder = stringResource(R.string.select_an_equipment)
                    )

                    SelectionDropdown(
                        label = stringResource(R.string.navigation_operations),
                        selectedItem = selectedOperationType,
                        items = operationTypes,
                        allSections = allSections,
                        onItemSelected = { operation ->
                            selectedOperationType = operation
                            updateDraft(draft.copy(operationTypeId = operation.id))
                            if (operation.id == AppConstants.SYSTEM_OPERATION_RESET_ID || operation.isResettable) {
                                editedResetAfter = true
                                updateDraft(draft.copy(resetAfter = true))
                            } else {
                                editedResetAfter = false
                                updateDraft(draft.copy(resetAfter = false))
                            }
                        },
                        itemDescription = { it.description.takeIf { d -> d.isNotBlank() } ?: stringResource(R.string.id_no_description, it.id) },
                        itemPhotoUri = { it.photoUri },
                        itemIconIdentifier = { it.iconIdentifier },
                        itemSectionId = { it.sectionId },
                        category = Category.OPERATION,
                        categoryColor = oColor,
                        placeholder = stringResource(R.string.select_an_operation)
                    )
                    if (selectedOperationType?.hasValue != false || selectedOperationType?.id == AppConstants.SYSTEM_OPERATION_RESET_ID) {
                        OutlinedTextField(
                            value = editedValueStr,
                            onValueChange = { input ->
                                val filtered = input.replace(',', '.')
                                if (filtered.isEmpty() || filtered == "." || filtered == "-") {
                                    editedValueStr = filtered
                                    updateDraft(draft.copy(value = null))
                                } else {
                                    val doubleVal = filtered.toDoubleOrNull()
                                    if (doubleVal != null && filtered.length <= 10) {
                                        val dotIndex = filtered.indexOf('.')
                                        if (dotIndex == -1 || filtered.length - dotIndex - 1 <= decimalPlaces) {
                                            editedValueStr = filtered
                                            updateDraft(draft.copy(value = doubleVal))
                                        }
                                    }
                                }
                            },
                            label = { Text(stringResource(R.string.value_optional, unitLabel)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            trailingIcon = {
                                if (editedValueStr.isNotEmpty() && selectedOperationType?.id != AppConstants.SYSTEM_OPERATION_RESET_ID) {
                                    IconButton(onClick = { 
                                        editedValueStr = ""
                                        updateDraft(draft.copy(value = null))
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    val isResettable = selectedOperationType?.isResettable == true || selectedOperationType?.id == AppConstants.SYSTEM_OPERATION_RESET_ID
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isResettable) { 
                                editedResetAfter = !editedResetAfter 
                                updateDraft(draft.copy(resetAfter = editedResetAfter))
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = editedResetAfter && isResettable,
                            onCheckedChange = { 
                                editedResetAfter = it
                                updateDraft(draft.copy(resetAfter = it))
                            },
                            enabled = isResettable
                        )
                        Text(
                            text = stringResource(R.string.reset_counter_after),
                            color = if (isResettable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }

                    OutlinedTextField(
                        value = editedNotes,
                        onValueChange = { 
                            if (it.length <= 200) {
                                editedNotes = it
                                updateDraft(draft.copy(notes = it))
                            }
                        },
                        label = { Text(stringResource(R.string.notes_optional)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editedCostStr,
                        onValueChange = { input ->
                            val filtered = input.replace(',', '.')
                            if (filtered.isEmpty() || filtered == ".") {
                                editedCostStr = filtered
                                updateDraft(draft.copy(cost = null))
                            } else {
                                val doubleVal = filtered.toDoubleOrNull()
                                if (doubleVal != null && filtered.length <= 10) {
                                    editedCostStr = filtered
                                    updateDraft(draft.copy(cost = doubleVal))
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
                        modifier = Modifier.fillMaxWidth().clickable { 
                            editedIsUnplanned = !editedIsUnplanned 
                            updateDraft(draft.copy(isUnplanned = editedIsUnplanned))
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = editedIsUnplanned,
                            onCheckedChange = { 
                                editedIsUnplanned = it
                                updateDraft(draft.copy(isUnplanned = it))
                            }
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

                    Spacer(modifier = Modifier.height(12.dp))

                    val isDirty = currentLog != logDetail.log

                    CommonActionButtons(
                        onConfirm = {
                            onSaveEdit(draft)
                        },
                        onDismiss = { onCancelEdit() },
                        confirmText = stringResource(R.string.save_log),
                        confirmIcon = Icons.Default.Save,
                        showDelete = true,
                        onDelete = { showDeleteConfirmation = true },
                        showArchive = true,
                        archiveIcon = if (currentLog.dismissed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        onArchive = { 
                            updateDraft(currentLog.copy(dismissed = !currentLog.dismissed))
                        },
                        showUndo = isDirty,
                        onUndo = { onUpdateDraft(logDetail.log) }
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
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
                            modifier = Modifier.graphicsLayer(alpha = equipmentTextAlpha).weight(1f)
                        )
                        if (logDetail.equipmentSectionId != null && allSections.size > 1) {
                            SectionBadge(
                                name = logDetail.equipmentSectionName ?: "",
                                colorHex = logDetail.equipmentSectionColor,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
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
                        if (logDetail.operationSectionId != null && allSections.size > 1) {
                            SectionBadge(
                                name = logDetail.operationSectionName ?: "",
                                colorHex = logDetail.operationSectionColor,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }

                    if (logDetail.operationTypeHasValue || logDetail.log.operationTypeId == AppConstants.SYSTEM_OPERATION_RESET_ID) {
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
                    } else {
                        // Se non ha valore, mostriamo solo il costo e la data
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            logDetail.log.cost?.let {
                                Text(
                                    text = String.format(Locale.US, "%.2f €", it),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } ?: Spacer(Modifier.weight(1f))
                            
                            Text(
                                text = dateFormat.format(Date(logDetail.log.date)),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
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
            if (!actualIsEditing) {
                IconButton(onClick = { onStartEdit() }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.edit_log),
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
