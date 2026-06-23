package com.moxmose.moxequiplog.ui.operations.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.*
import com.moxmose.moxequiplog.ui.components.CommonActionButtons
import com.moxmose.moxequiplog.ui.components.ImagePickerDialog
import com.moxmose.moxequiplog.ui.components.SectionSelector
import com.moxmose.moxequiplog.ui.components.TimeGranularitySelector
import com.moxmose.moxequiplog.ui.options.EquipmentIconProvider
import com.moxmose.moxequiplog.utils.AppConstants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOperationTypeDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String, ImageIdentifier?, Int, Int, Boolean, Boolean, Double?, Int?, TimeGranularity?, Int, TimeGranularity, Boolean, Double?) -> Unit,
    imageLibrary: List<Image>,
    categories: List<Category>,
    allSections: List<Section>,
    measurementUnits: List<MeasurementUnit>,
    selectedSectionId: Int,
    showDismissedSections: Boolean,
    categoryColors: Map<String, String>,
    categoryDefaultIcons: Map<String, String?>,
    categoryDefaultPhotos: Map<String, String?>,
    defaultIcon: String?,
    defaultPhotoUri: String?,
    onAddImage: (ImageIdentifier, String) -> Unit,
    onToggleImageVisibility: (Image) -> Unit,
    operationCategoryColor: String,
    initialOperationType: OperationType? = null,
    draft: OperationTypeDraft? = null,
    onUpdateDraft: (OperationTypeDraft) -> Unit = {}
) {
    val cloneSuffix = stringResource(R.string.clone_suffix)
    val currentOperationType = draft?.operationType ?: initialOperationType ?: OperationType(
        description = "",
        sectionId = if (selectedSectionId == AppConstants.ALL_SECTIONS_ID) AppConstants.DEFAULT_SECTION_ID else selectedSectionId,
        iconIdentifier = defaultIcon,
        photoUri = defaultPhotoUri,
        unitId = allSections.find { it.id == selectedSectionId }?.defaultUnitId ?: 1
    )

    var description by remember(currentOperationType.description) { 
        mutableStateOf(currentOperationType.description) 
    }
    var photoUri by remember(currentOperationType.photoUri) { mutableStateOf(currentOperationType.photoUri) }
    var iconId by remember(currentOperationType.iconIdentifier) { mutableStateOf(currentOperationType.iconIdentifier) }
    var sectionId by remember(currentOperationType.sectionId) { 
        mutableIntStateOf(currentOperationType.sectionId) 
    }
    var unitId by remember(currentOperationType.unitId) {
        mutableIntStateOf(currentOperationType.unitId)
    }
    var isResettable by remember(currentOperationType.isResettable) {
        mutableStateOf(currentOperationType.isResettable)
    }
    var estimatedCostStr by remember(currentOperationType.estimatedCost) { mutableStateOf(currentOperationType.estimatedCost?.toString() ?: "") }
    var estimatedCost by remember(currentOperationType.estimatedCost) { mutableStateOf(currentOperationType.estimatedCost) }
    var isPredictable by remember(currentOperationType.isPredictable) { mutableStateOf(currentOperationType.isPredictable) }
    var intervalValue by remember(currentOperationType.intervalValue) { mutableStateOf(currentOperationType.intervalValue) }
    var intervalValueStr by remember(currentOperationType.intervalValue) { mutableStateOf(currentOperationType.intervalValue?.toString() ?: "") }
    var timeoutValue by remember(currentOperationType.timeoutValue) { mutableStateOf(currentOperationType.timeoutValue) }
    var timeoutValueStr by remember(currentOperationType.timeoutValue) { mutableStateOf(currentOperationType.timeoutValue?.toString() ?: "") }
    var timeoutUnit by remember(currentOperationType.timeoutUnit) { mutableStateOf(currentOperationType.timeoutUnit ?: TimeGranularity.MONTHS) }
    
    var useCustomVisibilityHorizon by remember(currentOperationType.useCustomVisibilityHorizon) { mutableStateOf(currentOperationType.useCustomVisibilityHorizon) }
    var visibilityHorizon by remember(currentOperationType.visibilityHorizon) { mutableIntStateOf(currentOperationType.visibilityHorizon) }
    var visibilityHorizonUnit by remember(currentOperationType.visibilityHorizonUnit) { mutableStateOf(currentOperationType.visibilityHorizonUnit) }

    var showImageSelectorDialog by remember { mutableStateOf(false) }
    var showAdvancedSettings by rememberSaveable { mutableStateOf(false) }

    // Helper to update draft
    val updateDraft = { updated: OperationType -> onUpdateDraft(OperationTypeDraft(operationType = updated, isDefault = draft?.isDefault ?: false)) }

    if (showImageSelectorDialog) {
        ImagePickerDialog(
            onDismissRequest = { showImageSelectorDialog = false },
            photoUri = photoUri,
            iconIdentifier = iconId,
            onImageSelected = { (newIconId, newPhotoUri) ->
                iconId = newIconId
                photoUri = newPhotoUri
                showImageSelectorDialog = false
                updateDraft(currentOperationType.copy(iconIdentifier = newIconId, photoUri = newPhotoUri))
            },
            imageLibrary = imageLibrary,
            categories = categories,
            categoryColors = categoryColors,
            categoryDefaultIcons = categoryDefaultIcons,
            categoryDefaultPhotos = categoryDefaultPhotos,
            onAddImage = { uri, category -> onAddImage(ImageIdentifier.Photo(uri), category) },
            onRemoveImage = null,
            onUpdateImageOrder = null,
            onToggleImageVisibility = { uri, category -> imageLibrary.find { it.uri == uri && it.category == category }?.let { onToggleImageVisibility(it) } },
            onSetDefaultInCategory = null,
            isPhotoUsed = null,
            isPrefsMode = false,
            forcedCategory = Category.OPERATION
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(R.string.add_a_new_operation_type), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val borderColor = remember(operationCategoryColor) { try { Color(operationCategoryColor.toColorInt()) } catch (_: Exception) { Color.Gray } }
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer).border(2.dp, borderColor, CircleShape).clickable { showImageSelectorDialog = true }, contentAlignment = Alignment.Center) {
                        if (photoUri != null) AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(photoUri).crossfade(true).build(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Icon(imageVector = EquipmentIconProvider.getIcon(iconId, Category.OPERATION), contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    OutlinedTextField(
                        value = description, 
                        onValueChange = { 
                            if (it.length <= 50) {
                                description = it
                                updateDraft(currentOperationType.copy(description = it))
                            }
                        }, 
                        label = { Text(stringResource(R.string.operation_type_description)) }, 
                        modifier = Modifier.weight(1f), 
                        singleLine = true
                    )
                }

                SectionSelector(
                    allSections = allSections,
                    selectedSectionId = sectionId,
                    onSectionSelected = { 
                        sectionId = it 
                        updateDraft(currentOperationType.copy(sectionId = it))
                    },
                    showDismissed = showDismissedSections,
                    modifier = Modifier.fillMaxWidth()
                )

                // Unit Selector
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = measurementUnits.find { it.id == unitId }?.let { "${it.label} (${it.description})" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.measurement_unit)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        measurementUnits.filter { !it.isHidden }.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text("${unit.label} (${unit.description})") },
                                onClick = {
                                    unitId = unit.id
                                    updateDraft(currentOperationType.copy(unitId = unit.id))
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().clickable { 
                    isResettable = !isResettable
                    updateDraft(currentOperationType.copy(isResettable = isResettable))
                }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { 
                    Checkbox(checked = isResettable, onCheckedChange = { 
                        isResettable = it 
                        updateDraft(currentOperationType.copy(isResettable = it))
                    })
                    Text(text = stringResource(R.string.operation_is_resettable), style = MaterialTheme.typography.bodyMedium)
                }

                OutlinedTextField(
                    value = estimatedCostStr,
                    onValueChange = { input ->
                        val filtered = input.replace(',', '.')
                        if (filtered.isEmpty() || filtered == ".") {
                            estimatedCostStr = filtered
                            estimatedCost = null
                            updateDraft(currentOperationType.copy(estimatedCost = null))
                        } else {
                            val doubleVal = filtered.toDoubleOrNull()
                            if (doubleVal != null && filtered.length <= 10) {
                                estimatedCostStr = filtered
                                estimatedCost = doubleVal
                                updateDraft(currentOperationType.copy(estimatedCost = doubleVal))
                            }
                        }
                    },
                    label = { Text(stringResource(R.string.cost_optional)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth().clickable { 
                    isPredictable = !isPredictable 
                    updateDraft(currentOperationType.copy(isPredictable = isPredictable))
                }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { 
                    Checkbox(checked = isPredictable, onCheckedChange = { 
                        isPredictable = it 
                        updateDraft(currentOperationType.copy(isPredictable = it))
                    })
                    Text(text = "Automatic Maintenance Prediction", style = MaterialTheme.typography.bodyMedium) 
                }

                if (isPredictable) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Default Recurrence Intervals", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            val currentUnit = measurementUnits.find { it.id == unitId }?.label ?: ""
                            OutlinedTextField(
                                value = intervalValueStr, 
                                onValueChange = { input -> 
                                    val filtered = input.replace(',', '.')
                                    if (filtered.isEmpty() || filtered.toDoubleOrNull() != null) { 
                                        intervalValueStr = filtered
                                        intervalValue = filtered.toDoubleOrNull()
                                        updateDraft(currentOperationType.copy(intervalValue = intervalValue))
                                    } 
                                }, 
                                label = { Text("Usage Interval") }, 
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                                modifier = Modifier.fillMaxWidth(),
                                suffix = { Text(currentUnit) }
                            )
                            Text("Recurrence by usage ($currentUnit)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = timeoutValueStr, 
                                    onValueChange = { input -> 
                                        if (input.isEmpty()) { 
                                            timeoutValueStr = ""
                                            timeoutValue = null 
                                            updateDraft(currentOperationType.copy(timeoutValue = null))
                                        } else { 
                                            input.toIntOrNull()?.let { 
                                                timeoutValueStr = input
                                                timeoutValue = it 
                                                updateDraft(currentOperationType.copy(timeoutValue = it))
                                            } 
                                        } 
                                    }, 
                                    label = { Text("Timeout Value") }, 
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                                    modifier = Modifier.weight(1f)
                                )
                                TimeGranularitySelector(
                                    selected = timeoutUnit, 
                                    onSelected = { 
                                        timeoutUnit = it 
                                        updateDraft(currentOperationType.copy(timeoutUnit = it))
                                    }, 
                                    label = "Every", 
                                    modifier = Modifier.weight(1.2f)
                                )
                            }
                            Text("Maximum time allowed between maintenances", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { showAdvancedSettings = !showAdvancedSettings },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Advanced Visibility",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = if (showAdvancedSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (showAdvancedSettings) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth().clickable { 
                                    useCustomVisibilityHorizon = !useCustomVisibilityHorizon 
                                    updateDraft(currentOperationType.copy(useCustomVisibilityHorizon = useCustomVisibilityHorizon))
                                }, verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = useCustomVisibilityHorizon, onCheckedChange = { 
                                        useCustomVisibilityHorizon = it 
                                        updateDraft(currentOperationType.copy(useCustomVisibilityHorizon = it))
                                    })
                                    Text("Use custom visibility horizon", style = MaterialTheme.typography.bodySmall)
                                }
                                if (useCustomVisibilityHorizon) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(start = 32.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = visibilityHorizon.toString(), 
                                            onValueChange = { input -> 
                                                input.toIntOrNull()?.let { 
                                                    if (it in 1..999) {
                                                        visibilityHorizon = it 
                                                        updateDraft(currentOperationType.copy(visibilityHorizon = it))
                                                    }
                                                } 
                                            }, 
                                            label = { Text("Event Horizon") }, 
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                                            modifier = Modifier.weight(1f) 
                                        )
                                        TimeGranularitySelector(
                                            selected = visibilityHorizonUnit, 
                                            onSelected = { 
                                                visibilityHorizonUnit = it 
                                                updateDraft(currentOperationType.copy(visibilityHorizonUnit = it))
                                            }, 
                                            label = "Future span", 
                                            modifier = Modifier.weight(1.2f)
                                        )
                                    }
                                } else {
                                    Text(text = "Using global default (set in Options)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 32.dp))
                                }
                            }
                        }
                        Text("App will pick the earliest of the triggers.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            CommonActionButtons(
                onConfirm = {
                    val identifier = when {
                        photoUri != null -> ImageIdentifier.Photo(photoUri!!)
                        iconId != null -> ImageIdentifier.Icon(iconId!!)
                        else -> null
                    }
                    onConfirm(description, identifier, sectionId, unitId, isResettable, isPredictable,
                            intervalValue, timeoutValue, timeoutUnit, visibilityHorizon, visibilityHorizonUnit, useCustomVisibilityHorizon, estimatedCost)
                },
                onDismiss = onDismissRequest,
                confirmText = stringResource(R.string.button_add),
                confirmIcon = Icons.Default.Add,
                compactMode = true
            )
        },
        dismissButton = null
    )
}
