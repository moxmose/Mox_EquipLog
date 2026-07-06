package com.moxmose.moxequiplog.ui.equipment.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import com.moxmose.moxequiplog.ui.components.UnitSelector
import com.moxmose.moxequiplog.ui.options.EquipmentIconProvider
import com.moxmose.moxequiplog.utils.AppConstants
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEquipmentDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String, ImageIdentifier?, Int, Int, Int, TimeGranularity, Double?, TimeGranularity, Int, TimeGranularity, Boolean, Boolean, Boolean) -> Unit,
    defaultIcon: String?,
    defaultPhotoUri: String?,
    imageLibrary: List<Image>,
    categories: List<Category>,
    measurementUnits: List<MeasurementUnit>,
    allSections: List<Section>,
    selectedSectionId: Int,
    showDismissedSections: Boolean,
    defaultUnitId: Int?,
    equipmentCategoryColor: String?,
    categoryColors: Map<String, String>,
    categoryDefaultIcons: Map<String, String?>,
    categoryDefaultPhotos: Map<String, String?>,
    onAddImage: (ImageIdentifier, String) -> Unit,
    onToggleImageVisibility: (Image) -> Unit,
    initialEquipment: Equipment? = null,
    draft: EquipmentDraft? = null,
    onUpdateDraft: (EquipmentDraft) -> Unit = {}
) {
    val currentEquipment = draft?.equipment ?: initialEquipment ?: Equipment(
        description = "",
        unitId = defaultUnitId ?: 1,
        sectionId = if (selectedSectionId == AppConstants.ALL_SECTIONS_ID) AppConstants.DEFAULT_SECTION_ID else selectedSectionId,
        iconIdentifier = defaultIcon,
        photoUri = defaultPhotoUri
    )

    var description by remember(currentEquipment.description) { 
        mutableStateOf(currentEquipment.description) 
    }
    var photoUri by remember(currentEquipment.photoUri) { mutableStateOf(currentEquipment.photoUri) }
    var iconId by remember(currentEquipment.iconIdentifier) { mutableStateOf(currentEquipment.iconIdentifier) }
    var unitId by remember(currentEquipment.unitId) { 
        mutableIntStateOf(currentEquipment.unitId) 
    }
    var sectionId by remember(currentEquipment.sectionId) { 
        mutableIntStateOf(currentEquipment.sectionId) 
    }
    
    // Predictive Settings
    var useCustomUsageWindow by remember(currentEquipment.useCustomUsageWindow) { mutableStateOf(currentEquipment.useCustomUsageWindow) }
    var usageWindow by remember(currentEquipment.usageWindow) { mutableIntStateOf(currentEquipment.usageWindow) }
    var usageWindowUnit by remember(currentEquipment.usageWindowUnit) { mutableStateOf(currentEquipment.usageWindowUnit) }
    
    var manualAverageValue by remember(currentEquipment.manualAverageValue) { mutableStateOf(currentEquipment.manualAverageValue) }
    var manualAverageValueStr by remember(currentEquipment.manualAverageValue) { mutableStateOf(currentEquipment.manualAverageValue?.toString() ?: "") }
    var manualAverageUnit by remember(currentEquipment.manualAverageUnit) { mutableStateOf(currentEquipment.manualAverageUnit) }
    
    var useCustomVisibilityHorizon by remember(currentEquipment.useCustomVisibilityHorizon) { mutableStateOf(currentEquipment.useCustomVisibilityHorizon) }
    var visibilityHorizon by remember(currentEquipment.visibilityHorizon) { mutableIntStateOf(currentEquipment.visibilityHorizon) }
    var visibilityHorizonUnit by remember(currentEquipment.visibilityHorizonUnit) { mutableStateOf(currentEquipment.visibilityHorizonUnit) }
    var isResettable by remember(currentEquipment.isResettable) { mutableStateOf(currentEquipment.isResettable) }

    var showImageSelectorDialog by remember { mutableStateOf(false) }
    var showAdvancedSettings by rememberSaveable { mutableStateOf(false) }

    val selectedUnit = measurementUnits.find { it.id == unitId }
    val unitLabel = selectedUnit?.label ?: ""

    // Helper to update draft
    val updateDraft = { updated: Equipment -> onUpdateDraft(EquipmentDraft(equipment = updated, isDefault = draft?.isDefault ?: false)) }

    if (showImageSelectorDialog) {
        ImagePickerDialog(
            onDismissRequest = { showImageSelectorDialog = false },
            photoUri = photoUri,
            iconIdentifier = iconId,
            onImageSelected = { (newIconId, newPhotoUri) ->
                iconId = newIconId
                photoUri = newPhotoUri
                showImageSelectorDialog = false
                updateDraft(currentEquipment.copy(iconIdentifier = newIconId, photoUri = newPhotoUri))
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
            forcedCategory = Category.EQUIPMENT
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(R.string.add_a_new_equipment), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val borderColor = remember(equipmentCategoryColor, primaryColor) {
                        try {
                            equipmentCategoryColor?.toColorInt()?.let { Color(it) } ?: primaryColor
                        } catch (_: Exception) { primaryColor }
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .border(2.dp, borderColor, CircleShape)
                            .clickable { showImageSelectorDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUri != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(photoUri).crossfade(true).build(),
                                contentDescription = stringResource(R.string.equipment_photo),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            val icon = EquipmentIconProvider.getIcon(iconId)
                            Icon(
                                imageVector = icon,
                                contentDescription = stringResource(R.string.equipment_photo),
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    OutlinedTextField(
                        value = description,
                        onValueChange = { 
                            if (it.length <= 50) {
                                description = it
                                updateDraft(currentEquipment.copy(description = it))
                            }
                        },
                        label = { Text(stringResource(R.string.equipment_description)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                SectionSelector(
                    allSections = allSections,
                    selectedSectionId = sectionId,
                    onSectionSelected = { 
                        sectionId = it 
                        updateDraft(currentEquipment.copy(sectionId = it))
                    },
                    showDismissed = showDismissedSections,
                    modifier = Modifier.fillMaxWidth()
                )

                UnitSelector(
                    measurementUnits = measurementUnits,
                    selectedUnitId = unitId,
                    onUnitSelected = { 
                        unitId = it 
                        updateDraft(currentEquipment.copy(unitId = it))
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth().clickable { 
                    isResettable = !isResettable
                    updateDraft(currentEquipment.copy(isResettable = isResettable))
                }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { 
                    Checkbox(checked = isResettable, onCheckedChange = { 
                        isResettable = it 
                        updateDraft(currentEquipment.copy(isResettable = it))
                    })
                    Text(text = stringResource(R.string.equipment_is_resettable), style = MaterialTheme.typography.bodyMedium) 
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showAdvancedSettings = !showAdvancedSettings },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.predictive_maintenance_settings),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = if (showAdvancedSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                if (showAdvancedSettings) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Trend Window Section
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().clickable { 
                                useCustomUsageWindow = !useCustomUsageWindow 
                                updateDraft(currentEquipment.copy(useCustomUsageWindow = useCustomUsageWindow))
                            }, verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = useCustomUsageWindow, onCheckedChange = { 
                                    useCustomUsageWindow = it 
                                    updateDraft(currentEquipment.copy(useCustomUsageWindow = it))
                                })
                                Text("Use custom trend window", style = MaterialTheme.typography.bodySmall)
                            }
                            if (useCustomUsageWindow) {
                                Row(modifier = Modifier.fillMaxWidth().padding(start = 32.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = usageWindow.toString(),
                                        onValueChange = { input -> 
                                            input.toIntOrNull()?.let { 
                                                if (it in 1..999) {
                                                    usageWindow = it 
                                                    updateDraft(currentEquipment.copy(usageWindow = it))
                                                }
                                            } 
                                        },
                                        label = { Text("Window Value") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    TimeGranularitySelector(
                                        selected = usageWindowUnit, 
                                        onSelected = { 
                                            usageWindowUnit = it 
                                            updateDraft(currentEquipment.copy(usageWindowUnit = it))
                                        }, 
                                        label = "Of last", 
                                        modifier = Modifier.weight(1.2f)
                                    )
                                }
                            } else {
                                Text(text = "Using global default (set in Options)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 32.dp))
                            }
                        }

                        // Manual Average Section
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = manualAverageValueStr,
                                    onValueChange = { input ->
                                        val filtered = input.replace(',', '.')
                                        if (filtered.isEmpty() || filtered == "." || filtered == "-") {
                                            manualAverageValueStr = filtered
                                            manualAverageValue = null
                                            updateDraft(currentEquipment.copy(manualAverageValue = null))
                                        } else {
                                            val doubleVal = filtered.toDoubleOrNull()
                                            if (doubleVal != null) {
                                                manualAverageValueStr = filtered
                                                manualAverageValue = doubleVal
                                                updateDraft(currentEquipment.copy(manualAverageValue = doubleVal))
                                            }
                                        }
                                    },
                                    label = { Text(if (unitLabel.isNotBlank()) "Usage ($unitLabel)" else "Usage") },
                                    placeholder = { Text("Fallback") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                TimeGranularitySelector(
                                    selected = manualAverageUnit, 
                                    onSelected = { 
                                        manualAverageUnit = it 
                                        updateDraft(currentEquipment.copy(manualAverageUnit = it))
                                    }, 
                                    label = "Every", 
                                    modifier = Modifier.weight(1.2f)
                                )
                            }
                            Text(text = "Optional: expected usage when history is missing (fallback)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Visibility Horizon Row
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().clickable { 
                                useCustomVisibilityHorizon = !useCustomVisibilityHorizon 
                                updateDraft(currentEquipment.copy(useCustomVisibilityHorizon = useCustomVisibilityHorizon))
                            }, verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = useCustomVisibilityHorizon, onCheckedChange = { 
                                    useCustomVisibilityHorizon = it 
                                    updateDraft(currentEquipment.copy(useCustomVisibilityHorizon = it))
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
                                                    updateDraft(currentEquipment.copy(visibilityHorizon = it))
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
                                            updateDraft(currentEquipment.copy(visibilityHorizonUnit = it))
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
                    onConfirm(description, identifier, unitId, sectionId, usageWindow, usageWindowUnit, manualAverageValue, manualAverageUnit, visibilityHorizon, visibilityHorizonUnit, useCustomUsageWindow, useCustomVisibilityHorizon, isResettable)
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
