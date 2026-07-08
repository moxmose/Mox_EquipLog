package com.moxmose.moxequiplog.ui.operations.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.*
import com.moxmose.moxequiplog.ui.components.CommonActionButtons
import com.moxmose.moxequiplog.ui.components.FullImageDialog
import com.moxmose.moxequiplog.ui.components.ImageIcon
import com.moxmose.moxequiplog.ui.components.ImagePickerDialog
import com.moxmose.moxequiplog.ui.components.SectionBadge
import com.moxmose.moxequiplog.ui.components.SectionSelector
import com.moxmose.moxequiplog.ui.components.TimeGranularitySelector
import com.moxmose.moxequiplog.ui.operations.EquipmentOperationStatus
import com.moxmose.moxequiplog.ui.operations.OperationGlobalStatus
import com.moxmose.moxequiplog.ui.options.EquipmentIconProvider
import com.moxmose.moxequiplog.utils.AppConstants
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationTypeCard(
    operationType: OperationType,
    allSections: List<Section>,
    measurementUnits: List<MeasurementUnit>,
    showDismissedSections: Boolean,
    onUpdateOperationType: (OperationType) -> Unit,
    onDeleteOperationType: (OperationType) -> Unit,
    onDismissOperationType: (OperationType) -> Unit,
    onRestoreOperationType: (OperationType) -> Unit,
    onCloneOperationType: (OperationType) -> Unit,
    operationTypeImages: List<Image>,
    allCategories: List<Category>,
    categoryColors: Map<String, String>,
    categoryDefaultIcons: Map<String, String?>,
    categoryDefaultPhotos: Map<String, String?>,
    onAddImage: (ImageIdentifier, String) -> Unit,
    onToggleImageVisibility: (Image) -> Unit,
    operationCategoryColor: String,
    isDefault: Boolean,
    onToggleDefault: () -> Unit,
    showDefault: Boolean = true,
    defaultEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    status: OperationGlobalStatus? = null,
    onAffectedAction: (EquipmentOperationStatus) -> Unit,
    expandAllTrigger: Int = 0,
    collapseAllTrigger: Int = 0,
    draft: OperationTypeDraft? = null,
    sectionsResettableStatus: Map<Int, Boolean> = emptyMap(),
    onStartEdit: () -> Unit = {},
    onCancelEdit: () -> Unit = {},
    onUpdateDraft: (OperationTypeDraft) -> Unit = {},
    onSaveEdit: (OperationTypeDraft) -> Unit = {}
) {
    val isEditing = draft != null
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    
    LaunchedEffect(expandAllTrigger) { if (expandAllTrigger > 0) isExpanded = true }
    LaunchedEffect(collapseAllTrigger) { if (collapseAllTrigger > 0) isExpanded = false }

    val currentOperationType = draft?.operationType ?: operationType

    var editedDescription by remember(currentOperationType.description) { mutableStateOf(currentOperationType.description) }
    var editedIconId by remember(currentOperationType.iconIdentifier) { mutableStateOf(currentOperationType.iconIdentifier) }
    var editedPhotoUri by remember(currentOperationType.photoUri) { mutableStateOf(currentOperationType.photoUri) }
    var editedSectionId by remember(currentOperationType.sectionId) { mutableIntStateOf(currentOperationType.sectionId) }
    var editedUnitId by remember(currentOperationType.unitId) { mutableIntStateOf(currentOperationType.unitId) }
    var editedIsResettable by remember(currentOperationType.isResettable) { mutableStateOf(currentOperationType.isResettable) }
    var editedHasValue by remember(currentOperationType.hasValue) { mutableStateOf(currentOperationType.hasValue) }
    var editedIsPredictable by remember(currentOperationType.isPredictable) { mutableStateOf(currentOperationType.isPredictable) }
    
    var editedUseCustomVisibilityHorizon by remember(currentOperationType.useCustomVisibilityHorizon) { mutableStateOf(currentOperationType.useCustomVisibilityHorizon) }
    var editedIntervalValueStr by remember(currentOperationType.intervalValue) { mutableStateOf(currentOperationType.intervalValue?.toString() ?: "") }
    var editedTimeoutValueStr by remember(currentOperationType.timeoutValue) { mutableStateOf(currentOperationType.timeoutValue?.toString() ?: "") }
    var editedTimeoutUnit by remember(currentOperationType.timeoutUnit) { mutableStateOf(currentOperationType.timeoutUnit ?: TimeGranularity.MONTHS) }
    var editedVisibilityHorizon by remember(currentOperationType.visibilityHorizon) { mutableIntStateOf(currentOperationType.visibilityHorizon) }
    var editedVisibilityHorizonUnit by remember(currentOperationType.visibilityHorizonUnit) { mutableStateOf(currentOperationType.visibilityHorizonUnit) }
    var editedEstimatedCostStr by remember(currentOperationType.estimatedCost) { mutableStateOf(currentOperationType.estimatedCost?.toString() ?: "") }

    // Helper to update draft
    val updateDraft = { updated: OperationTypeDraft -> if (isEditing) onUpdateDraft(updated) }

    LaunchedEffect(editedSectionId, sectionsResettableStatus) {
        val show = editedSectionId == AppConstants.DEFAULT_SECTION_ID || sectionsResettableStatus[editedSectionId] == true
        if (!show && editedIsResettable) {
            editedIsResettable = false
            draft?.let { updateDraft(it.copy(operationType = it.operationType.copy(isResettable = false))) }
        }
    }

    val context = LocalContext.current
    var showFullImageDialog by remember { mutableStateOf<String?>(null) }
    var showNoPictureDialog by remember { mutableStateOf(false) }
    var showImageSelectorDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.delete_operation_type)) },
            text = { Text(stringResource(R.string.delete_operation_type_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteOperationType(operationType)
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.button_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        )
    }

    if (showImageSelectorDialog) {
        ImagePickerDialog(
            onDismissRequest = { showImageSelectorDialog = false },
            photoUri = editedPhotoUri,
            iconIdentifier = editedIconId,
            onImageSelected = { (newIconId, newPhotoUri) ->
                editedIconId = newIconId
                editedPhotoUri = newPhotoUri
                showImageSelectorDialog = false
                draft?.let { updateDraft(it.copy(operationType = it.operationType.copy(iconIdentifier = newIconId, photoUri = newPhotoUri))) }
            },
            imageLibrary = operationTypeImages,
            categories = allCategories,
            categoryColors = categoryColors,
            categoryDefaultIcons = categoryDefaultIcons,
            categoryDefaultPhotos = categoryDefaultPhotos,
            onAddImage = { uri, category -> onAddImage(ImageIdentifier.Photo(uri), category) },
            onRemoveImage = null,
            onUpdateImageOrder = null,
            onToggleImageVisibility = { uri, category -> operationTypeImages.find { it.uri == uri && it.category == category }?.let { onToggleImageVisibility(it) } },
            onSetDefaultInCategory = null,
            isPhotoUsed = null,
            isPrefsMode = false,
            forcedCategory = Category.OPERATION
        )
    }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val operationColor = remember(operationCategoryColor) { try { Color(operationCategoryColor.toColorInt()) } catch (_: Exception) { Color.Gray } }

    Box(contentAlignment = Alignment.TopEnd, modifier = modifier) {
        Card(
            modifier = Modifier.fillMaxWidth().animateContentSize().graphicsLayer(alpha = if (currentOperationType.dismissed) 0.5f else 1f).clickable { if (!isEditing) isExpanded = !isExpanded },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)),
            border = if (isDefault) BorderStroke(4.dp, operationColor) else null
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (isEditing && draft != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .border(2.dp, operationColor, CircleShape)
                                    .clickable { showImageSelectorDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                if (editedPhotoUri != null) AsyncImage(model = ImageRequest.Builder(context).data(editedPhotoUri).crossfade(true).build(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                else Icon(imageVector = EquipmentIconProvider.getIcon(editedIconId, Category.OPERATION), contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            OutlinedTextField(
                                value = editedDescription,
                                onValueChange = { 
                                    if (it.length <= 50) {
                                        editedDescription = it
                                        updateDraft(draft.copy(operationType = draft.operationType.copy(description = it)))
                                    }
                                },
                                label = { Text(stringResource(R.string.operation_type_description)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionSelector(
                                allSections = allSections,
                                selectedSectionId = editedSectionId,
                                onSectionSelected = { 
                                    editedSectionId = it 
                                    updateDraft(draft.copy(operationType = draft.operationType.copy(sectionId = it)))
                                },
                                showDismissed = showDismissedSections,
                                modifier = Modifier.weight(1f)
                            )
                            
                            var unitExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = unitExpanded,
                                onExpandedChange = { unitExpanded = !unitExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = measurementUnits.find { it.id == editedUnitId }?.label ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(R.string.measurement_unit)) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                                ExposedDropdownMenu(
                                    expanded = unitExpanded,
                                    onDismissRequest = { unitExpanded = false }
                                ) {
                                    measurementUnits.filter { !it.isHidden }.forEach { unit ->
                                        DropdownMenuItem(
                                            text = { Text("${unit.label} (${unit.description})") },
                                            onClick = {
                                                editedUnitId = unit.id
                                                updateDraft(draft.copy(operationType = draft.operationType.copy(unitId = unit.id)))
                                                unitExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        OutlinedTextField(
                            value = editedEstimatedCostStr,
                            onValueChange = { input ->
                                val filtered = input.replace(',', '.')
                                if (filtered.isEmpty() || filtered == ".") {
                                    editedEstimatedCostStr = filtered
                                    updateDraft(draft.copy(operationType = draft.operationType.copy(estimatedCost = null)))
                                } else {
                                    val doubleVal = filtered.toDoubleOrNull()
                                    if (doubleVal != null && filtered.length <= 10) {
                                        editedEstimatedCostStr = filtered
                                        updateDraft(draft.copy(operationType = draft.operationType.copy(estimatedCost = doubleVal)))
                                    }
                                }
                            },
                            label = { Text(stringResource(R.string.cost_optional)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodySmall
                        )

                        val showResettableCheckbox = remember(editedSectionId, sectionsResettableStatus) {
                            editedSectionId == AppConstants.DEFAULT_SECTION_ID || sectionsResettableStatus[editedSectionId] == true
                        }

                        if (showResettableCheckbox) {
                            Row(modifier = Modifier.fillMaxWidth().clickable { 
                                editedIsResettable = !editedIsResettable
                                updateDraft(draft.copy(operationType = draft.operationType.copy(isResettable = editedIsResettable)))
                            }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { 
                                Checkbox(checked = editedIsResettable, onCheckedChange = { 
                                    editedIsResettable = it
                                    updateDraft(draft.copy(operationType = draft.operationType.copy(isResettable = it)))
                                })
                                Text(text = stringResource(R.string.operation_is_resettable), style = MaterialTheme.typography.bodyMedium) 
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth().clickable { 
                            editedHasValue = !editedHasValue
                            updateDraft(draft.copy(operationType = draft.operationType.copy(hasValue = editedHasValue)))
                        }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { 
                            Checkbox(checked = editedHasValue, onCheckedChange = { 
                                editedHasValue = it
                                updateDraft(draft.copy(operationType = draft.operationType.copy(hasValue = it)))
                            })
                            Text(text = "Track numeric value (Odometer/Counter)", style = MaterialTheme.typography.bodyMedium)
                        }

                        Row(modifier = Modifier.fillMaxWidth().clickable { 
                            editedIsPredictable = !editedIsPredictable
                            updateDraft(draft.copy(operationType = draft.operationType.copy(isPredictable = editedIsPredictable)))
                        }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { 
                            Checkbox(checked = editedIsPredictable, onCheckedChange = { 
                                editedIsPredictable = it
                                updateDraft(draft.copy(operationType = draft.operationType.copy(isPredictable = it)))
                            })
                            Text(text = "Predictive Maintenance", style = MaterialTheme.typography.bodyMedium) 
                        }
                        
                        if (editedIsPredictable) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Recurrence Intervals", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                val currentUnit = measurementUnits.find { it.id == editedUnitId }?.label ?: ""
                                OutlinedTextField(
                                    value = editedIntervalValueStr, 
                                    onValueChange = { input -> 
                                        val filtered = input.replace(',', '.')
                                        if (filtered.isEmpty() || filtered.toDoubleOrNull() != null) {
                                            editedIntervalValueStr = filtered
                                            updateDraft(draft.copy(operationType = draft.operationType.copy(intervalValue = filtered.toDoubleOrNull())))
                                        }
                                    }, 
                                    label = { Text("Usage Interval") }, 
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                                    modifier = Modifier.fillMaxWidth(), 
                                    textStyle = MaterialTheme.typography.bodySmall,
                                    suffix = { Text(currentUnit) }
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = editedTimeoutValueStr, 
                                        onValueChange = { input -> 
                                            if (input.isEmpty()) {
                                                editedTimeoutValueStr = ""
                                                updateDraft(draft.copy(operationType = draft.operationType.copy(timeoutValue = null)))
                                            } else {
                                                input.toIntOrNull()?.let { 
                                                    editedTimeoutValueStr = input
                                                    updateDraft(draft.copy(operationType = draft.operationType.copy(timeoutValue = it)))
                                                }
                                            }
                                        }, 
                                        label = { Text("Timeout Value") }, 
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                                        modifier = Modifier.weight(1f), 
                                        textStyle = MaterialTheme.typography.bodySmall
                                    )
                                    TimeGranularitySelector(
                                        selected = editedTimeoutUnit, 
                                        onSelected = { 
                                            editedTimeoutUnit = it
                                            updateDraft(draft.copy(operationType = draft.operationType.copy(timeoutUnit = it)))
                                        }, 
                                        label = "Every", 
                                        modifier = Modifier.weight(1.2f)
                                    )
                                }
                                
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth().clickable { 
                                        editedUseCustomVisibilityHorizon = !editedUseCustomVisibilityHorizon
                                        updateDraft(draft.copy(operationType = draft.operationType.copy(useCustomVisibilityHorizon = editedUseCustomVisibilityHorizon)))
                                    }, verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = editedUseCustomVisibilityHorizon, onCheckedChange = { 
                                            editedUseCustomVisibilityHorizon = it
                                            updateDraft(draft.copy(operationType = draft.operationType.copy(useCustomVisibilityHorizon = it)))
                                        })
                                        Text("Use custom visibility horizon", style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (editedUseCustomVisibilityHorizon) {
                                        Row(modifier = Modifier.fillMaxWidth().padding(start = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            OutlinedTextField(
                                                value = editedVisibilityHorizon.toString(), 
                                                onValueChange = { input -> 
                                                    input.toIntOrNull()?.let { 
                                                        editedVisibilityHorizon = it
                                                        updateDraft(draft.copy(operationType = draft.operationType.copy(visibilityHorizon = it)))
                                                    } 
                                                }, 
                                                label = { Text("Event Horizon") }, 
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                                                modifier = Modifier.weight(1f), 
                                                textStyle = MaterialTheme.typography.bodySmall
                                            )
                                            TimeGranularitySelector(
                                                selected = editedVisibilityHorizonUnit, 
                                                onSelected = { 
                                                    editedVisibilityHorizonUnit = it
                                                    updateDraft(draft.copy(operationType = draft.operationType.copy(visibilityHorizonUnit = it)))
                                                }, 
                                                label = "Future span", 
                                                modifier = Modifier.weight(1.2f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val isDirty = draft.operationType != operationType || draft.isDefault != isDefault

                        CommonActionButtons(
                            onConfirm = {
                                onSaveEdit(draft)
                            },
                            onDismiss = { onCancelEdit() },
                            confirmText = stringResource(R.string.save_operation_type),
                            confirmIcon = Icons.Default.Save,
                            showClone = true,
                            onClone = { onCloneOperationType(operationType) },
                            showArchive = true,
                            onArchive = { 
                                updateDraft(draft.copy(operationType = draft.operationType.copy(dismissed = !draft.operationType.dismissed)))
                            },
                            archiveIcon = if (draft.operationType.dismissed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            showDelete = !operationType.isSystem,
                            onDelete = { showDeleteConfirmation = true },
                            showDefault = showDefault,
                            isDefault = draft.isDefault,
                            onToggleDefault = onToggleDefault,
                            defaultEnabled = defaultEnabled,
                            showUndo = isDirty,
                            onUndo = { onUpdateDraft(OperationTypeDraft(operationType, isDefault)) }
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .border(2.dp, operationColor, CircleShape)
                                    .clickable {
                                        if (operationType.photoUri != null) showFullImageDialog = operationType.photoUri
                                        else if (operationType.iconIdentifier == null) showNoPictureDialog = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (operationType.photoUri != null) AsyncImage(model = ImageRequest.Builder(context).data(operationType.photoUri).crossfade(true).build(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                else Icon(imageVector = EquipmentIconProvider.getIcon(operationType.iconIdentifier, Category.OPERATION), contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (operationType.description.isNotBlank()) operationType.description else stringResource(R.string.id_no_description, operationType.id), 
                                    color = if (operationType.description.isNotBlank()) LocalContentColor.current else MaterialTheme.colorScheme.onSurfaceVariant, 
                                    maxLines = 1, 
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                val unitLabel = measurementUnits.find { it.id == operationType.unitId }?.label
                                val section = remember(operationType.sectionId, allSections) { allSections.find { it.id == operationType.sectionId } }
                                val badgeColor = remember(section?.color, operationColor) {
                                    try {
                                        section?.color?.toColorInt()?.let { Color(it) } ?: operationColor
                                    } catch (_: Exception) { operationColor }
                                }

                                if (unitLabel != null) {
                                    com.moxmose.moxequiplog.ui.components.UnitBadge(
                                        label = unitLabel,
                                        color = badgeColor
                                    )
                                }

                                if (section != null && allSections.size > 1) {
                                    SectionBadge(
                                        name = section.name,
                                        colorHex = section.color
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically, 
                                modifier = Modifier.padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!isExpanded && status != null) {
                                    val overdueCount = status.affectedEquipments.count { it.isOverdue }
                                    val upcomingCount = status.affectedEquipments.count { !it.isOverdue }

                                    if (overdueCount > 0) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    if (upcomingCount > 0) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color(0xFFFFB300)
                                        )
                                    }
                                }
                            }
                        }

                        IconButton(
                            onClick = { 
                                onStartEdit()
                                isExpanded = true 
                            },
                            modifier = Modifier.size(40.dp)
                        ) { 
                            Icon(
                                imageVector = Icons.Filled.Edit, 
                                contentDescription = stringResource(R.string.edit_operation_type),
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (!isEditing && isExpanded && status != null && status.affectedEquipments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Upcoming for Equipments", style = MaterialTheme.typography.labelSmall, color = operationColor, fontWeight = FontWeight.Bold)
                    Column(modifier = Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        status.affectedEquipments.sortedBy { it.nextPresumedDate ?: Long.MAX_VALUE }.forEach { eqStatus ->
                            val hasInconsistency = eqStatus.isPlanned && eqStatus.predictedDate != null && 
                                    eqStatus.predictedDate < (eqStatus.nextPresumedDate ?: Long.MAX_VALUE) - 86400000L

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = if (eqStatus.isOverdue) Icons.Default.PriorityHigh else if (hasInconsistency) Icons.Default.Warning else if (eqStatus.isPlanned) Icons.AutoMirrored.Filled.EventNote else Icons.Default.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (eqStatus.isOverdue) MaterialTheme.colorScheme.error 
                                               else if (hasInconsistency) Color(0xFFFF9800)
                                               else if (eqStatus.isPlanned) MaterialTheme.colorScheme.secondary 
                                               else MaterialTheme.colorScheme.primary
                                    )
                                    if (eqStatus.reason != null && !eqStatus.isPlanned) {
                                        Icon(
                                            imageVector = if (eqStatus.reason == com.moxmose.moxequiplog.ui.equipment.PredictionReason.TIME) Icons.Default.AccessTime else Icons.Default.BarChart,
                                            contentDescription = null,
                                            modifier = Modifier.size(10.dp).padding(start = 2.dp),
                                            tint = if (eqStatus.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    
                                    val eqColor = remember(eqStatus.equipment.color, categoryColors) {
                                        try { 
                                            eqStatus.equipment.color?.toColorInt()?.let { Color(it) } 
                                            ?: categoryColors[Category.EQUIPMENT]?.toColorInt()?.let { Color(it) } 
                                            ?: Color.Gray 
                                        } catch (_: Exception) { Color.Gray }
                                    }
                                    
                                    ImageIcon(
                                        photoUri = eqStatus.equipment.photoUri,
                                        iconIdentifier = eqStatus.equipment.iconIdentifier,
                                        modifier = Modifier.size(18.dp),
                                        category = Category.EQUIPMENT,
                                        borderColor = eqColor,
                                        contentPadding = 1.dp
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = eqStatus.equipment.description, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = (if (eqStatus.isOverdue) stringResource(R.string.reminder_overdue) + " - " else "") +
                                               (eqStatus.nextPresumedDate?.let { dateFormat.format(Date(it)) } ?: "Never"),
                                        style = MaterialTheme.typography.labelSmall, 
                                        color = if (eqStatus.isOverdue) MaterialTheme.colorScheme.error 
                                                else if (hasInconsistency) Color(0xFFFF9800)
                                                else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        onClick = { onAffectedAction(eqStatus) },
                                        shape = CircleShape,
                                        color = operationColor.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, operationColor.copy(alpha = 0.5f)),
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (eqStatus.isPlanned) Icons.Default.Edit else Icons.Default.Build,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = operationColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isDefault) {
            Surface(
                shape = CircleShape,
                color = operationColor,
                modifier = Modifier
                    .size(24.dp)
                    .offset(x = (-2).dp, y = 2.dp),
                shadowElevation = 4.dp,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }

    showFullImageDialog?.let { uri ->
        FullImageDialog(photoUri = uri, onDismiss = { showFullImageDialog = null })
    }

    if (showNoPictureDialog) {
        AlertDialog(
            onDismissRequest = { showNoPictureDialog = false },
            title = { Text(stringResource(R.string.no_image_title)) },
            text = { Text(stringResource(R.string.no_image_message)) },
            confirmButton = { TextButton(onClick = { showNoPictureDialog = false }) { Text(stringResource(R.string.button_ok)) } }
        )
    }
}
