package com.moxmose.moxequiplog.ui.equipment.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
    onDismissEquipment: (Equipment) -> Unit,
    onRestoreEquipment: (Equipment) -> Unit,
    onCloneEquipment: (Equipment) -> Unit,
    onAddImage: (ImageIdentifier, String) -> Unit,
    onToggleImageVisibility: (Image) -> Unit,
    equipmentCategoryColor: String?,
    isDefault: Boolean,
    onToggleDefault: () -> Unit,
    modifier: Modifier = Modifier,
    status: EquipmentStatus? = null,
    onPredictionAction: (OperationStatus) -> Unit,
    onPlannedAction: (OperationStatus) -> Unit,
    categoryColors: Map<String, String>,
    categoryDefaultIcons: Map<String, String?>,
    categoryDefaultPhotos: Map<String, String?>,
    expandAllTrigger: Int = 0,
    collapseAllTrigger: Int = 0
) {
    var isEditing by remember { mutableStateOf(false) }
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    
    LaunchedEffect(expandAllTrigger) { if (expandAllTrigger > 0) isExpanded = true }
    LaunchedEffect(collapseAllTrigger) { if (collapseAllTrigger > 0) isExpanded = false }

    var editedDescription by remember(equipment.description) { mutableStateOf(equipment.description) }
    var editedUnitId by remember(equipment.unitId) { mutableIntStateOf(equipment.unitId) }
    var editedSectionId by remember(equipment.sectionId) { mutableIntStateOf(equipment.sectionId) }
    var editedIconId by remember(equipment.iconIdentifier) { mutableStateOf(equipment.iconIdentifier) }
    var editedPhotoUri by remember(equipment.photoUri) { mutableStateOf(equipment.photoUri) }
    var editedIsResettable by remember(equipment.isResettable) { mutableStateOf(equipment.isResettable) }
    
    // Predictive Settings
    var editedUseCustomUsageWindow by remember(equipment.useCustomUsageWindow) { mutableStateOf(equipment.useCustomUsageWindow) }
    var editedUsageWindow by remember(equipment.usageWindow) { mutableIntStateOf(equipment.usageWindow) }
    var editedUsageWindowUnit by remember(equipment.usageWindowUnit) { mutableStateOf(equipment.usageWindowUnit) }
    
    var editedManualAverageValue by remember(equipment.manualAverageValue) { mutableStateOf(equipment.manualAverageValue) }
    var editedManualAverageValueStr by remember(equipment.manualAverageValue) { mutableStateOf(equipment.manualAverageValue?.toString() ?: "") }
    var editedManualAverageUnit by remember(equipment.manualAverageUnit) { mutableStateOf(equipment.manualAverageUnit) }
    
    var editedUseCustomVisibilityHorizon by remember(equipment.useCustomVisibilityHorizon) { mutableStateOf(equipment.useCustomVisibilityHorizon) }
    var editedVisibilityHorizon by remember(equipment.visibilityHorizon) { mutableIntStateOf(equipment.visibilityHorizon) }
    var editedVisibilityHorizonUnit by remember(equipment.visibilityHorizonUnit) { mutableStateOf(equipment.visibilityHorizonUnit) }
    
    var showFullImageDialog by remember { mutableStateOf<String?>(null) }
    var showNoPictureDialog by remember { mutableStateOf(false) }
    var showImageSelectorDialog by remember { mutableStateOf(false) }

    if (showImageSelectorDialog) {
        ImagePickerDialog(
            onDismissRequest = { showImageSelectorDialog = false },
            photoUri = editedPhotoUri,
            iconIdentifier = editedIconId,
            onImageSelected = { (newIconId, newPhotoUri) ->
                editedIconId = newIconId
                editedPhotoUri = newPhotoUri
                showImageSelectorDialog = false
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

    val unit = measurementUnits.find { it.id == editedUnitId }
    val unitLabel = unit?.label ?: ""
    val decimalPlaces = unit?.decimalPlaces ?: 0
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val equipmentColor = remember(equipmentCategoryColor, primaryColor) {
        try { equipmentCategoryColor?.toColorInt()?.let { Color(it) } ?: primaryColor } catch (_: Exception) { primaryColor }
    }

    Box(contentAlignment = Alignment.BottomEnd) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize()
                .graphicsLayer(alpha = if (equipment.dismissed) 0.5f else 1f)
                .then(if (isDefault) Modifier.border(3.dp, equipmentColor, MaterialTheme.shapes.medium) else Modifier)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { if (!isEditing) isExpanded = !isExpanded }),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                if (isEditing) {
                    // Layout in MODALITÀ EDIT: Descrizione sopra, icone sotto
                    Column {
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
                                onValueChange = { if (it.length <= 50) editedDescription = it },
                                label = { Text(stringResource(R.string.equipment_description)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { if (equipment.dismissed) onRestoreEquipment(equipment) else onDismissEquipment(equipment) }) {
                                Icon(imageVector = if (equipment.dismissed) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                            }
                            IconButton(onClick = { onCloneEquipment(equipment) }) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = stringResource(R.string.button_clone), tint = MaterialTheme.colorScheme.secondary)
                            }
                            IconButton(onClick = {
                                onUpdateEquipment(
                                    equipment.copy(
                                        description = editedDescription, 
                                        unitId = editedUnitId,
                                        sectionId = editedSectionId,
                                        iconIdentifier = editedIconId,
                                        photoUri = editedPhotoUri,
                                        isResettable = editedIsResettable,
                                        usageWindow = editedUsageWindow,
                                        usageWindowUnit = editedUsageWindowUnit,
                                        manualAverageValue = editedManualAverageValue,
                                        manualAverageUnit = editedManualAverageUnit,
                                        visibilityHorizon = editedVisibilityHorizon,
                                        visibilityHorizonUnit = editedVisibilityHorizonUnit,
                                        useCustomUsageWindow = editedUseCustomUsageWindow,
                                        useCustomVisibilityHorizon = editedUseCustomVisibilityHorizon
                                    )
                                )
                                isEditing = false
                            }) {
                                Icon(imageVector = Icons.Filled.Done, contentDescription = null)
                            }
                            IconButton(onClick = { onToggleDefault() }) {
                                Icon(imageVector = if (isDefault) Icons.Filled.Star else Icons.Filled.StarBorder, contentDescription = null, tint = if (isDefault) Color(0xFFFFB300) else LocalContentColor.current)
                            }
                            IconButton(onClick = {}) { Icon(imageVector = Icons.Filled.DragHandle, contentDescription = null) }
                        }
                    }
                } else {
                    // Layout in MODALITÀ VISUALIZZAZIONE: Tutto su una riga (compatto)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .border(2.dp, equipmentColor, CircleShape)
                                    .clickable {
                                        if (editedPhotoUri != null) showFullImageDialog = editedPhotoUri
                                        else if (editedIconId == null) showNoPictureDialog = true
                                    },
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
                            
                            // HEALTH TAGGER (Badge)
                            if (!isExpanded && status != null && status.operationStatuses.isNotEmpty()) {
                                val overdueCount = status.operationStatuses.count { it.isOverdue }
                                val upcomingCount = status.operationStatuses.count { !it.isOverdue && (it.isPlanned || it.nextPresumedDate != null) }

                                if (overdueCount > 0 || upcomingCount > 0) {
                                    val badgeColor = if (overdueCount > 0) MaterialTheme.colorScheme.error else Color(0xFFFFB300)
                                    val badgeIcon = if (overdueCount > 0) Icons.Default.Warning else Icons.Default.Schedule
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .offset(x = 4.dp, y = 4.dp)
                                            .clip(CircleShape)
                                            .background(badgeColor)
                                            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = badgeIcon,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = if (editedDescription.isNotBlank()) editedDescription else stringResource(R.string.id_no_description, equipment.id),
                                    modifier = Modifier.weight(1f, fill = false),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                // Section Badge
                                val section = remember(equipment.sectionId, allSections) { allSections.find { it.id == equipment.sectionId } }
                                if (section != null && section.id != AppConstants.DEFAULT_SECTION_ID) {
                                    val sectionColor = remember(section.color) {
                                        try { section.color?.toColorInt()?.let { Color(it) } ?: Color.Gray } catch (_: Exception) { Color.Gray }
                                    }
                                    Surface(
                                        shape = CircleShape,
                                        color = sectionColor.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, sectionColor.copy(alpha = 0.5f)),
                                        modifier = Modifier.padding(start = 4.dp)
                                    ) {
                                        Text(
                                            text = section.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = sectionColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            if (status != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val displayValue = status.health.currentSessionValue ?: status.health.lastRecordedValue
                                    displayValue?.let { valStr ->
                                        Text(text = "Last: ${String.format(Locale.US, "%.${decimalPlaces}f", valStr)} $unitLabel", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    
                                    val displayEstimated = status.health.currentSessionEstimated ?: status.health.estimatedCurrentValue
                                    displayEstimated?.let { estStr ->
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = "Now (est): ${String.format(Locale.US, "%.${decimalPlaces}f", estStr)} $unitLabel", style = MaterialTheme.typography.labelSmall, color = equipmentColor, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                isEditing = true
                                isExpanded = true
                            }) {
                                Icon(imageVector = Icons.Filled.Edit, contentDescription = null)
                            }
                            IconButton(onClick = { onToggleDefault() }) {
                                Icon(imageVector = if (isDefault) Icons.Filled.Star else Icons.Filled.StarBorder, contentDescription = null, tint = if (isDefault) Color(0xFFFFB300) else LocalContentColor.current)
                            }
                            IconButton(onClick = {}) { Icon(imageVector = Icons.Filled.DragHandle, contentDescription = null) }
                        }
                    }
                }
                
                if (isEditing) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionSelector(
                        allSections = allSections,
                        selectedSectionId = editedSectionId,
                        onSectionSelected = { editedSectionId = it },
                        showDismissed = showDismissedSections,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    UnitSelector(
                        measurementUnits = measurementUnits,
                        selectedUnitId = editedUnitId,
                        onUnitSelected = { editedUnitId = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { editedIsResettable = !editedIsResettable }.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(checked = editedIsResettable, onCheckedChange = { editedIsResettable = it })
                        Text(text = stringResource(R.string.equipment_is_resettable), style = MaterialTheme.typography.bodyMedium)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = stringResource(R.string.predictive_maintenance_settings),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Trend Window Section
                    Column(modifier = Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().clickable { editedUseCustomUsageWindow = !editedUseCustomUsageWindow }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = editedUseCustomUsageWindow, onCheckedChange = { editedUseCustomUsageWindow = it })
                            Text("Use custom trend window", style = MaterialTheme.typography.bodySmall)
                        }
                        if (editedUseCustomUsageWindow) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = editedUsageWindow.toString(),
                                    onValueChange = { input ->
                                        input.toIntOrNull()?.let { if (it in 1..999) editedUsageWindow = it }
                                    },
                                    label = { Text("Window Value") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                                
                                TimeGranularitySelector(
                                    selected = editedUsageWindowUnit,
                                    onSelected = { editedUsageWindowUnit = it },
                                    label = "Of last",
                                    modifier = Modifier.weight(1.2f)
                                )
                            }
                        } else {
                            Text(text = "Using global default (set in Options)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 32.dp))
                        }
                    }

                    // Manual Average Section
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = editedManualAverageValueStr,
                                onValueChange = { input ->
                                    val filtered = input.replace(',', '.')
                                    if (filtered.isEmpty() || filtered == "." || filtered == "-") {
                                        editedManualAverageValueStr = filtered
                                        editedManualAverageValue = null
                                    } else {
                                        val doubleVal = filtered.toDoubleOrNull()
                                        if (doubleVal != null) {
                                            editedManualAverageValueStr = filtered
                                            editedManualAverageValue = doubleVal
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
                                onSelected = { editedManualAverageUnit = it },
                                label = "Every",
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                        Text(
                            text = "Optional: expected usage when history is missing (fallback)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    // Visibility Horizon Section
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().clickable { editedUseCustomVisibilityHorizon = !editedUseCustomVisibilityHorizon }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = editedUseCustomVisibilityHorizon, onCheckedChange = { editedUseCustomVisibilityHorizon = it })
                            Text("Use custom visibility horizon", style = MaterialTheme.typography.bodySmall)
                        }
                        if (editedUseCustomVisibilityHorizon) {
                            Row(modifier = Modifier.fillMaxWidth().padding(start = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = editedVisibilityHorizon.toString(),
                                    onValueChange = { input -> input.toIntOrNull()?.let { editedVisibilityHorizon = it } },
                                    label = { Text("Event Horizon") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                                TimeGranularitySelector(selected = editedVisibilityHorizonUnit, onSelected = { editedVisibilityHorizonUnit = it }, label = "Future span", modifier = Modifier.weight(1.2f))
                            }
                        } else {
                            Text(text = "Using global default (set in Options)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 32.dp))
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
        if (isDefault) {
            Box(
                modifier = Modifier.padding(end = 4.dp, bottom = 4.dp).size(24.dp).clip(CircleShape).background(equipmentColor).border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White) }
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
