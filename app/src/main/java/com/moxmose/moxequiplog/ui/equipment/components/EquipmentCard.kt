package com.moxmose.moxequiplog.ui.equipment.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.*
import com.moxmose.moxequiplog.ui.components.CommonActionButtons
import com.moxmose.moxequiplog.ui.components.FullImageDialog
import com.moxmose.moxequiplog.ui.components.ImagePickerDialog
import com.moxmose.moxequiplog.ui.components.SectionSelector
import com.moxmose.moxequiplog.ui.components.TimeGranularitySelector
import com.moxmose.moxequiplog.ui.components.UnitSelector
import com.moxmose.moxequiplog.ui.equipment.EquipmentStatus
import com.moxmose.moxequiplog.ui.equipment.OperationStatus
import com.moxmose.moxequiplog.ui.options.EquipmentIconProvider
import com.moxmose.moxequiplog.utils.AppConstants
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EquipmentCard(
    equipment: Equipment,
    equipmentImages: List<Image>,
    allCategories: List<Category>,
    measurementUnits: List<MeasurementUnit>,
    allSections: List<Section>,
    showDismissedSections: Boolean,
    onUpdateEquipment: (Equipment) -> Unit,
    onDeleteEquipment: (Equipment) -> Unit,
    onDismissEquipment: (Equipment) -> Unit,
    onRestoreEquipment: (Equipment) -> Unit,
    onCloneEquipment: (Equipment) -> Unit,
    onAddImage: (ImageIdentifier, String) -> Unit,
    onToggleImageVisibility: (Image) -> Unit,
    equipmentCategoryColor: String?,
    isDefault: Boolean,
    onToggleDefault: () -> Unit,
    showDefault: Boolean = true,
    defaultEnabled: Boolean = true,
    originalIsDefault: Boolean = false,
    modifier: Modifier = Modifier,
    status: EquipmentStatus? = null,
    onPredictionAction: (OperationStatus) -> Unit,
    onPlannedAction: (OperationStatus) -> Unit,
    categoryColors: Map<String, String>,
    categoryDefaultIcons: Map<String, String?>,
    categoryDefaultPhotos: Map<String, String?>,
    expandAllTrigger: Int = 0,
    collapseAllTrigger: Int = 0,
    draft: EquipmentDraft? = null,
    onStartEdit: () -> Unit = {},
    onCancelEdit: () -> Unit = {},
    onUpdateDraft: (EquipmentDraft) -> Unit = {},
    onSaveEdit: (EquipmentDraft) -> Unit = {}
) {
    val isEditing = draft != null
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    
    LaunchedEffect(expandAllTrigger) { if (expandAllTrigger > 0) isExpanded = true }
    LaunchedEffect(collapseAllTrigger) { if (collapseAllTrigger > 0) isExpanded = false }

    // Use draft values if editing, otherwise original equipment values
    val currentEquipment = draft?.equipment ?: equipment

    var editedDescription by remember(currentEquipment.description) { mutableStateOf(currentEquipment.description) }
    var editedUnitId by remember(currentEquipment.unitId) { mutableIntStateOf(currentEquipment.unitId) }
    var editedSectionId by remember(currentEquipment.sectionId) { mutableIntStateOf(currentEquipment.sectionId) }
    var editedIconId by remember(currentEquipment.iconIdentifier) { mutableStateOf(currentEquipment.iconIdentifier) }
    var editedPhotoUri by remember(currentEquipment.photoUri) { mutableStateOf(currentEquipment.photoUri) }
    var editedIsResettable by remember(currentEquipment.isResettable) { mutableStateOf(currentEquipment.isResettable) }
    
    // Predictive Settings
    var editedUseCustomUsageWindow by remember(currentEquipment.useCustomUsageWindow) { mutableStateOf(currentEquipment.useCustomUsageWindow) }
    var editedUsageWindow by remember(currentEquipment.usageWindow) { mutableIntStateOf(currentEquipment.usageWindow) }
    var editedUsageWindowUnit by remember(currentEquipment.usageWindowUnit) { mutableStateOf(currentEquipment.usageWindowUnit) }
    
    var editedManualAverageValue by remember(currentEquipment.manualAverageValue) { mutableStateOf(currentEquipment.manualAverageValue) }
    var editedManualAverageValueStr by remember(currentEquipment.manualAverageValue) { mutableStateOf(currentEquipment.manualAverageValue?.toString() ?: "") }
    var editedManualAverageUnit by remember(currentEquipment.manualAverageUnit) { mutableStateOf(currentEquipment.manualAverageUnit) }
    
    var editedUseCustomVisibilityHorizon by remember(currentEquipment.useCustomVisibilityHorizon) { mutableStateOf(currentEquipment.useCustomVisibilityHorizon) }
    var editedVisibilityHorizon by remember(currentEquipment.visibilityHorizon) { mutableIntStateOf(currentEquipment.visibilityHorizon) }
    var editedVisibilityHorizonUnit by remember(currentEquipment.visibilityHorizonUnit) { mutableStateOf(currentEquipment.visibilityHorizonUnit) }
    
    var showFullImageDialog by remember { mutableStateOf<String?>(null) }
    var showNoPictureDialog by remember { mutableStateOf(false) }
    var showImageSelectorDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Helper to update draft
    val updateDraft = { updated: EquipmentDraft -> if (isEditing) onUpdateDraft(updated) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.delete_equipment)) },
            text = { Text(stringResource(R.string.delete_equipment_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteEquipment(equipment)
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
                draft?.let { updateDraft(it.copy(equipment = it.equipment.copy(iconIdentifier = newIconId, photoUri = newPhotoUri))) }
            },
            imageLibrary = equipmentImages,
            categories = allCategories,
            categoryColors = categoryColors,
            categoryDefaultIcons = categoryDefaultIcons,
            categoryDefaultPhotos = categoryDefaultPhotos,
            onAddImage = { uri, category -> onAddImage(ImageIdentifier.Photo(uri), category) },
            onRemoveImage = null,
            onUpdateImageOrder = null,
            onToggleImageVisibility = { uri, category -> equipmentImages.find { it.uri == uri && it.category == category }?.let { onToggleImageVisibility(it) } },
            onSetDefaultInCategory = null,
            isPhotoUsed = null,
            isPrefsMode = false,
            forcedCategory = Category.EQUIPMENT
        )
    }

    val unit = measurementUnits.find { it.id == currentEquipment.unitId }
    val unitLabel = unit?.label ?: ""
    val decimalPlaces = unit?.decimalPlaces ?: 0
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val equipmentColor = remember(equipmentCategoryColor, primaryColor) {
        try { equipmentCategoryColor?.toColorInt()?.let { Color(it) } ?: primaryColor } catch (_: Exception) { primaryColor }
    }

    Box(contentAlignment = Alignment.TopEnd, modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .graphicsLayer(alpha = if (currentEquipment.dismissed) 0.5f else 1f)
                .clickable { if (!isEditing) isExpanded = !isExpanded },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)),
            border = if (isDefault) BorderStroke(4.dp, equipmentColor) else null
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                if (isEditing && draft != null) {
                    // Layout in MODALITÀ EDIT: Descrizione e campi sopra
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .border(2.dp, equipmentColor, CircleShape)
                                    .clickable { showImageSelectorDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                if (editedPhotoUri != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current).data(editedPhotoUri).crossfade(true).build(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = EquipmentIconProvider.getIcon(editedIconId),
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            OutlinedTextField(
                                value = editedDescription,
                                onValueChange = { 
                                    if (it.length <= 50) {
                                        editedDescription = it
                                        updateDraft(draft.copy(equipment = draft.equipment.copy(description = it)))
                                    }
                                },
                                label = { Text(stringResource(R.string.equipment_description)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        SectionSelector(
                            allSections = allSections,
                            selectedSectionId = editedSectionId,
                            onSectionSelected = { 
                                editedSectionId = it
                                updateDraft(draft.copy(equipment = draft.equipment.copy(sectionId = it)))
                            },
                            showDismissed = showDismissedSections,
                            modifier = Modifier.fillMaxWidth()
                        )

                        UnitSelector(
                            measurementUnits = measurementUnits,
                            selectedUnitId = editedUnitId,
                            onUnitSelected = { 
                                editedUnitId = it
                                updateDraft(draft.copy(equipment = draft.equipment.copy(unitId = it)))
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { 
                                editedIsResettable = !editedIsResettable
                                updateDraft(draft.copy(equipment = draft.equipment.copy(isResettable = editedIsResettable)))
                            },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(checked = editedIsResettable, onCheckedChange = { 
                                editedIsResettable = it
                                updateDraft(draft.copy(equipment = draft.equipment.copy(isResettable = it)))
                            })
                            Text(text = stringResource(R.string.equipment_is_resettable), style = MaterialTheme.typography.bodyMedium)
                        }

                        HorizontalDivider()

                        Text(
                            text = stringResource(R.string.predictive_maintenance_settings),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Trend Window
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth().clickable { 
                                    editedUseCustomUsageWindow = !editedUseCustomUsageWindow
                                    updateDraft(draft.copy(equipment = draft.equipment.copy(useCustomUsageWindow = editedUseCustomUsageWindow)))
                                }, verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = editedUseCustomUsageWindow, onCheckedChange = { 
                                        editedUseCustomUsageWindow = it
                                        updateDraft(draft.copy(equipment = draft.equipment.copy(useCustomUsageWindow = it)))
                                    })
                                    Text("Use custom trend window", style = MaterialTheme.typography.bodySmall)
                                }
                                if (editedUseCustomUsageWindow) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(start = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = editedUsageWindow.toString(),
                                            onValueChange = { input -> 
                                                input.toIntOrNull()?.let { 
                                                    if (it in 1..999) {
                                                        editedUsageWindow = it
                                                        updateDraft(draft.copy(equipment = draft.equipment.copy(usageWindow = it)))
                                                    }
                                                } 
                                            },
                                            label = { Text("Window Value") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            textStyle = MaterialTheme.typography.bodySmall
                                        )
                                        TimeGranularitySelector(
                                            selected = editedUsageWindowUnit, 
                                            onSelected = { 
                                                editedUsageWindowUnit = it
                                                updateDraft(draft.copy(equipment = draft.equipment.copy(usageWindowUnit = it)))
                                            }, 
                                            label = "Of last", 
                                            modifier = Modifier.weight(1.2f)
                                        )
                                    }
                                }
                            }

                            // Manual Average
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = editedManualAverageValueStr,
                                        onValueChange = { input ->
                                            val filtered = input.replace(',', '.')
                                            if (filtered.isEmpty() || filtered == "." || filtered == "-") {
                                                editedManualAverageValueStr = filtered
                                                editedManualAverageValue = null
                                                updateDraft(draft.copy(equipment = draft.equipment.copy(manualAverageValue = null)))
                                            } else {
                                                val doubleVal = filtered.toDoubleOrNull()
                                                if (doubleVal != null) {
                                                    editedManualAverageValueStr = filtered
                                                    editedManualAverageValue = doubleVal
                                                    updateDraft(draft.copy(equipment = draft.equipment.copy(manualAverageValue = doubleVal)))
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
                                        onSelected = { 
                                            editedManualAverageUnit = it
                                            updateDraft(draft.copy(equipment = draft.equipment.copy(manualAverageUnit = it)))
                                        }, 
                                        label = "Every", 
                                        modifier = Modifier.weight(1.2f)
                                    )
                                }
                            }

                            // Visibility Horizon
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth().clickable { 
                                    editedUseCustomVisibilityHorizon = !editedUseCustomVisibilityHorizon
                                    updateDraft(draft.copy(equipment = draft.equipment.copy(useCustomVisibilityHorizon = editedUseCustomVisibilityHorizon)))
                                }, verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = editedUseCustomVisibilityHorizon, onCheckedChange = { 
                                        editedUseCustomVisibilityHorizon = it
                                        updateDraft(draft.copy(equipment = draft.equipment.copy(useCustomVisibilityHorizon = it)))
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
                                                    updateDraft(draft.copy(equipment = draft.equipment.copy(visibilityHorizon = it)))
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
                                                updateDraft(draft.copy(equipment = draft.equipment.copy(visibilityHorizonUnit = it)))
                                            }, 
                                            label = "Future span", 
                                            modifier = Modifier.weight(1.2f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val isDirty = draft.equipment != equipment || draft.isDefault != originalIsDefault

                        CommonActionButtons(
                            onConfirm = {
                                onSaveEdit(draft)
                            },
                            onDismiss = { onCancelEdit() },
                            confirmText = stringResource(R.string.save_equipment),
                            confirmIcon = Icons.Default.Save,
                            showClone = true,
                            onClone = { onCloneEquipment(equipment) },
                            showArchive = true,
                            onArchive = { 
                                updateDraft(draft.copy(equipment = draft.equipment.copy(dismissed = !draft.equipment.dismissed)))
                            },
                            archiveIcon = if (currentEquipment.dismissed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            showDelete = true,
                            onDelete = { showDeleteConfirmation = true },
                            showDefault = showDefault,
                            isDefault = draft.isDefault,
                            onToggleDefault = onToggleDefault,
                            defaultEnabled = defaultEnabled,
                            showUndo = isDirty,
                            onUndo = { onUpdateDraft(EquipmentDraft(equipment, originalIsDefault)) }
                        )
                    }
                } else {
                    // Layout in MODALITÀ VISUALIZZAZIONE: Compatto
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .border(2.dp, equipmentColor, CircleShape)
                                    .clickable {
                                        if (equipment.photoUri != null) showFullImageDialog = equipment.photoUri
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

                            // HEALTH TAGGER (Badge) moved below
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = if (equipment.description.isNotBlank()) equipment.description else stringResource(R.string.id_no_description, equipment.id),
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            // Section Badge, Status Icons & Usage Info in a second row
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!isExpanded && status != null) {
                                    val overdueCount = status.operationStatuses.count { it.isOverdue }
                                    val upcomingCount = status.operationStatuses.count { !it.isOverdue && (it.isPlanned || it.nextPresumedDate != null) }

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

                                val section = remember(equipment.sectionId, allSections) { allSections.find { it.id == equipment.sectionId } }
                                if (section != null && section.id != AppConstants.DEFAULT_SECTION_ID) {
                                    val sectionColor = remember(section.color) {
                                        try { section.color?.toColorInt()?.let { Color(it) } ?: Color.Gray } catch (_: Exception) { Color.Gray }
                                    }
                                    Surface(
                                        shape = CircleShape,
                                        color = sectionColor.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, sectionColor.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = section.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = sectionColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (status != null) {
                                    val displayValue = status.health.currentSessionValue ?: status.health.lastRecordedValue
                                    displayValue?.let { valStr ->
                                        Text(
                                            text = "Last: ${String.format(Locale.US, "%.${decimalPlaces}f", valStr)} $unitLabel",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    val displayEstimated = status.health.currentSessionEstimated ?: status.health.estimatedCurrentValue
                                    displayEstimated?.let { estStr ->
                                        Text(
                                            text = "Now (est): ${String.format(Locale.US, "%.${decimalPlaces}f", estStr)} $unitLabel",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = equipmentColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // SINGLE ACTION ICON (Edit)
                        IconButton(
                            onClick = {
                                onStartEdit()
                                isExpanded = true
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.edit_equipment),
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
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
                            val hasInconsistency = opStatus.isPlanned && opStatus.predictedDate != null && 
                                    opStatus.predictedDate < (opStatus.nextPresumedDate ?: Long.MAX_VALUE) - 86400000L // 1 day buffer

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = if (opStatus.isOverdue) Icons.Default.PriorityHigh else if (hasInconsistency) Icons.Default.Warning else if (opStatus.isPlanned) Icons.AutoMirrored.Filled.EventNote else Icons.Default.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (opStatus.isOverdue) MaterialTheme.colorScheme.error 
                                               else if (hasInconsistency) Color(0xFFFF9800) 
                                               else if (opStatus.isPlanned) MaterialTheme.colorScheme.secondary 
                                               else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    
                                    val opColor = remember(opStatus.operation.color, categoryColors) {
                                        try { 
                                            opStatus.operation.color?.toColorInt()?.let { Color(it) } 
                                            ?: categoryColors[Category.OPERATION]?.toColorInt()?.let { Color(it) } 
                                            ?: Color.Gray 
                                        } catch (_: Exception) { Color.Gray }
                                    }
                                    
                                    com.moxmose.moxequiplog.ui.components.ImageIcon(
                                        photoUri = opStatus.operation.photoUri,
                                        iconIdentifier = opStatus.operation.iconIdentifier,
                                        modifier = Modifier.size(18.dp),
                                        category = Category.OPERATION,
                                        borderColor = opColor,
                                        contentPadding = 1.dp
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
                                        text = (if (opStatus.isOverdue) stringResource(R.string.reminder_overdue) + " - " else "") +
                                               (opStatus.nextPresumedDate?.let { dateFormat.format(Date(it)) } ?: "Never"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (opStatus.isOverdue) MaterialTheme.colorScheme.error 
                                                else if (hasInconsistency) Color(0xFFFF9800)
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        onClick = { if (opStatus.isPlanned) onPlannedAction(opStatus) else onPredictionAction(opStatus) },
                                        shape = CircleShape,
                                        color = equipmentColor.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, equipmentColor.copy(alpha = 0.5f)),
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
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
        }
        
        // DEFAULT CHECKMARK BADGE (Corner of the Card)
        if (isDefault) {
            Surface(
                shape = CircleShape,
                color = equipmentColor,
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

    if (showNoPictureDialog) {
        AlertDialog(
            onDismissRequest = { showNoPictureDialog = false },
            title = { Text(stringResource(R.string.no_image_title)) },
            text = { Text(stringResource(R.string.no_image_message)) },
            confirmButton = { TextButton(onClick = { showNoPictureDialog = false }) { Text(stringResource(R.string.button_ok)) } }
        )
    }

    showFullImageDialog?.let { uri ->
        FullImageDialog(photoUri = uri, onDismiss = { showFullImageDialog = null })
    }
}
