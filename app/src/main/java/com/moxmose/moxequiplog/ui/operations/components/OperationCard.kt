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
import com.moxmose.moxequiplog.ui.components.SectionSelector
import com.moxmose.moxequiplog.ui.components.TimeGranularitySelector
import com.moxmose.moxequiplog.ui.operations.EquipmentOperationStatus
import com.moxmose.moxequiplog.ui.operations.OperationGlobalStatus
import com.moxmose.moxequiplog.ui.options.EquipmentIconProvider
import com.moxmose.moxequiplog.utils.AppConstants
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OperationTypeCard(
    operationType: OperationType,
    allSections: List<Section>,
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
    modifier: Modifier = Modifier,
    status: OperationGlobalStatus? = null,
    onAffectedAction: (EquipmentOperationStatus) -> Unit,
    expandAllTrigger: Int = 0,
    collapseAllTrigger: Int = 0
) {
    var isEditing by remember { mutableStateOf(false) }
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    
    LaunchedEffect(expandAllTrigger) { if (expandAllTrigger > 0) isExpanded = true }
    LaunchedEffect(collapseAllTrigger) { if (collapseAllTrigger > 0) isExpanded = false }

    var editedDescription by remember(operationType.description) { mutableStateOf(operationType.description) }
    var editedIconId by remember(operationType.iconIdentifier) { mutableStateOf(operationType.iconIdentifier) }
    var editedPhotoUri by remember(operationType.photoUri) { mutableStateOf(operationType.photoUri) }
    var editedSectionId by remember(operationType.sectionId) { mutableIntStateOf(operationType.sectionId) }
    var editedIsPredictable by remember(operationType.isPredictable) { mutableStateOf(operationType.isPredictable) }
    
    var editedUseCustomVisibilityHorizon by remember(operationType.useCustomVisibilityHorizon) { mutableStateOf(operationType.useCustomVisibilityHorizon) }
    var editedIntervalValueStr by remember(operationType.intervalValue) { mutableStateOf(operationType.intervalValue?.toString() ?: "") }
    var editedTimeoutValueStr by remember(operationType.timeoutValue) { mutableStateOf(operationType.timeoutValue?.toString() ?: "") }
    var editedTimeoutUnit by remember(operationType.timeoutUnit) { mutableStateOf(operationType.timeoutUnit ?: TimeGranularity.MONTHS) }
    var editedVisibilityHorizon by remember(operationType.visibilityHorizon) { mutableIntStateOf(operationType.visibilityHorizon) }
    var editedVisibilityHorizonUnit by remember(operationType.visibilityHorizonUnit) { mutableStateOf(operationType.visibilityHorizonUnit) }
    var editedEstimatedCostStr by remember(operationType.estimatedCost) { mutableStateOf(operationType.estimatedCost?.toString() ?: "") }

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
            modifier = Modifier.fillMaxWidth().animateContentSize().graphicsLayer(alpha = if (operationType.dismissed) 0.5f else 1f).clickable { if (!isEditing) isExpanded = !isExpanded },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)),
            border = if (isDefault) BorderStroke(4.dp, operationColor) else null
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (isEditing) {
                    // Layout in MODALITÀ EDIT: Campi e pulsanti in basso
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
                                onValueChange = { if (it.length <= 50) editedDescription = it },
                                label = { Text(stringResource(R.string.operation_type_description)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        SectionSelector(
                            allSections = allSections,
                            selectedSectionId = editedSectionId,
                            onSectionSelected = { editedSectionId = it },
                            showDismissed = showDismissedSections,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = editedEstimatedCostStr,
                            onValueChange = { input ->
                                val filtered = input.replace(',', '.')
                                if (filtered.isEmpty() || filtered == ".") {
                                    editedEstimatedCostStr = filtered
                                } else {
                                    val doubleVal = filtered.toDoubleOrNull()
                                    if (doubleVal != null && filtered.length <= 10) {
                                        editedEstimatedCostStr = filtered
                                    }
                                }
                            },
                            label = { Text(stringResource(R.string.cost_optional)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodySmall
                        )

                        Row(modifier = Modifier.fillMaxWidth().clickable { editedIsPredictable = !editedIsPredictable }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Checkbox(checked = editedIsPredictable, onCheckedChange = { editedIsPredictable = it }); Text(text = "Predictive Maintenance", style = MaterialTheme.typography.bodyMedium) }
                        
                        if (editedIsPredictable) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Recurrence Intervals", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                OutlinedTextField(value = editedIntervalValueStr, onValueChange = { input -> val filtered = input.replace(',', '.'); if (filtered.isEmpty() || filtered.toDoubleOrNull() != null) editedIntervalValueStr = filtered }, label = { Text("Usage Interval") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), textStyle = MaterialTheme.typography.bodySmall)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(value = editedTimeoutValueStr, onValueChange = { input -> if (input.isEmpty()) editedTimeoutValueStr = "" else input.toIntOrNull()?.let { editedTimeoutValueStr = input } }, label = { Text("Timeout Value") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), textStyle = MaterialTheme.typography.bodySmall)
                                    TimeGranularitySelector(selected = editedTimeoutUnit, onSelected = { editedTimeoutUnit = it }, label = "Every", modifier = Modifier.weight(1.2f))
                                }
                                
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth().clickable { editedUseCustomVisibilityHorizon = !editedUseCustomVisibilityHorizon }, verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = editedUseCustomVisibilityHorizon, onCheckedChange = { editedUseCustomVisibilityHorizon = it })
                                        Text("Use custom visibility horizon", style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (editedUseCustomVisibilityHorizon) {
                                        Row(modifier = Modifier.fillMaxWidth().padding(start = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            OutlinedTextField(value = editedVisibilityHorizon.toString(), onValueChange = { input -> input.toIntOrNull()?.let { editedVisibilityHorizon = it } }, label = { Text("Event Horizon") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), textStyle = MaterialTheme.typography.bodySmall)
                                            TimeGranularitySelector(selected = editedVisibilityHorizonUnit, onSelected = { editedVisibilityHorizonUnit = it }, label = "Future span", modifier = Modifier.weight(1.2f))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        CommonActionButtons(
                            onConfirm = {
                                onUpdateOperationType(operationType.copy(
                                    description = editedDescription, 
                                    iconIdentifier = editedIconId, 
                                    photoUri = editedPhotoUri, 
                                    sectionId = editedSectionId,
                                    isPredictable = editedIsPredictable, 
                                    intervalValue = editedIntervalValueStr.toDoubleOrNull(), 
                                    timeoutValue = editedTimeoutValueStr.toIntOrNull(), 
                                    timeoutUnit = editedTimeoutUnit, 
                                    visibilityHorizon = editedVisibilityHorizon, 
                                    visibilityHorizonUnit = editedVisibilityHorizonUnit, 
                                    useCustomVisibilityHorizon = editedUseCustomVisibilityHorizon,
                                    estimatedCost = editedEstimatedCostStr.toDoubleOrNull()
                                )) 
                                isEditing = false 
                            },
                            onDismiss = { isEditing = false },
                            confirmText = stringResource(R.string.save_operation_type),
                            confirmIcon = Icons.Default.Save,
                            showClone = true,
                            onClone = { onCloneOperationType(operationType) },
                            showArchive = true,
                            onArchive = { if (operationType.dismissed) onRestoreOperationType(operationType) else onDismissOperationType(operationType) },
                            archiveIcon = if (operationType.dismissed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            showDelete = !operationType.isSystem,
                            onDelete = { showDeleteConfirmation = true },
                            showDefault = true,
                            isDefault = isDefault,
                            onToggleDefault = onToggleDefault
                        )
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
                                    .border(2.dp, operationColor, CircleShape)
                                    .clickable {
                                        if (editedPhotoUri != null) showFullImageDialog = editedPhotoUri
                                        else if (editedIconId == null) showNoPictureDialog = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (editedPhotoUri != null) AsyncImage(model = ImageRequest.Builder(context).data(editedPhotoUri).crossfade(true).build(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                else Icon(imageVector = EquipmentIconProvider.getIcon(editedIconId, Category.OPERATION), contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }

                            // HEALTH TAGGER (Badge) moved below
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = if (editedDescription.isNotBlank()) editedDescription else stringResource(R.string.id_no_description, operationType.id), 
                                    color = if (editedDescription.isNotBlank()) LocalContentColor.current else MaterialTheme.colorScheme.onSurfaceVariant, 
                                    maxLines = 1, 
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Section Badge & Status Icons row
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
                                
                                val section = remember(operationType.sectionId, allSections) { allSections.find { it.id == operationType.sectionId } }
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
                            }
                        }

                        // SINGLE ACTION ICON (Edit)
                        IconButton(
                            onClick = { 
                                isEditing = true
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

                if (isEditing) {
                    // Already handled above
                } else if (isExpanded && status != null && status.affectedEquipments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Upcoming for Equipments", style = MaterialTheme.typography.labelSmall, color = operationColor, fontWeight = FontWeight.Bold)
                    Column(modifier = Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        status.affectedEquipments.sortedBy { it.nextPresumedDate ?: Long.MAX_VALUE }.forEach { eqStatus ->
                            val hasInconsistency = eqStatus.isPlanned && eqStatus.predictedDate != null && 
                                    eqStatus.predictedDate < (eqStatus.nextPresumedDate ?: Long.MAX_VALUE) - 86400000L // 1 day buffer

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
                                    Spacer(modifier = Modifier.width(4.dp))
                                    
                                    // Equipment Icon
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

        // DEFAULT CHECKMARK BADGE (Corner of the Card)
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
