package com.moxmose.moxequiplog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.MeasurementUnit
import com.moxmose.moxequiplog.utils.AppConstants

@Composable
fun UnitItemCard(
    unit: MeasurementUnit,
    isDefault: Boolean,
    usageCount: Int,
    onUnitSelected: () -> Unit,
    onUpdateUnit: (MeasurementUnit) -> Unit,
    onCloneUnit: (MeasurementUnit) -> Unit,
    onToggleVisibility: () -> Unit,
    onDeleteUnit: () -> Unit,
    compactMode: Boolean = false
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedLabel by remember(unit.label) { mutableStateOf(unit.label) }
    var editedDescription by remember(unit.description) { mutableStateOf(unit.description) }
    var editedDecimalPlaces by remember(unit.decimalPlaces) { mutableIntStateOf(unit.decimalPlaces) }
    var editedIsHidden by remember(unit.isHidden) { mutableStateOf(unit.isHidden) }

    val cardAlpha = if (isEditing) (if (editedIsHidden) 0.5f else 1f) else (if (unit.isHidden) 0.5f else 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .graphicsLayer(alpha = cardAlpha)
            .clickable { if (!isEditing) onUnitSelected() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (isEditing) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Star toggle badge in edit mode
                        IconButton(
                            onClick = onUnitSelected,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isDefault) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (isDefault) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!unit.isSystem) {
                            OutlinedTextField(
                                value = editedLabel,
                                onValueChange = { if (it.length <= AppConstants.UNIT_LABEL_MAX_LENGTH) editedLabel = it },
                                label = { Text(stringResource(R.string.options_unit_label)) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Text(
                                text = "[${unit.label}] ${unit.description}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (!unit.isSystem) {
                        OutlinedTextField(
                            value = editedDescription,
                            onValueChange = { if (it.length <= AppConstants.UNIT_DESCRIPTION_MAX_LENGTH) editedDescription = it },
                            label = { Text(stringResource(R.string.options_unit_desc_placeholder)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column {
                        Text(
                            text = stringResource(R.string.options_unit_decimals) + ": $editedDecimalPlaces",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Slider(
                            value = editedDecimalPlaces.toFloat(),
                            onValueChange = { editedDecimalPlaces = it.toInt() },
                            valueRange = 0f..AppConstants.MAX_DECIMAL_PLACES.toFloat(),
                            steps = AppConstants.MAX_DECIMAL_PLACES - 1
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    val isDirty = editedLabel != unit.label || editedDescription != unit.description || editedDecimalPlaces != unit.decimalPlaces || editedIsHidden != unit.isHidden

                    CommonActionButtons(
                        onConfirm = {
                            onUpdateUnit(unit.copy(
                                label = editedLabel, 
                                description = editedDescription,
                                decimalPlaces = editedDecimalPlaces,
                                isHidden = editedIsHidden
                            ))
                            isEditing = false
                        },
                        onDismiss = { 
                            isEditing = false
                            // Reverting local state
                            editedLabel = unit.label
                            editedDescription = unit.description
                            editedDecimalPlaces = unit.decimalPlaces
                            editedIsHidden = unit.isHidden
                        },
                        confirmText = stringResource(R.string.save_equipment),
                        confirmIcon = Icons.Default.Save,
                        showClone = true,
                        onClone = { onCloneUnit(unit) },
                        showDelete = !unit.isSystem,
                        onDelete = onDeleteUnit,
                        showArchive = true,
                        onArchive = { editedIsHidden = !editedIsHidden },
                        archiveIcon = if (editedIsHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        showUndo = isDirty,
                        onUndo = {
                            editedLabel = unit.label
                            editedDescription = unit.description
                            editedDecimalPlaces = unit.decimalPlaces
                            editedIsHidden = unit.isHidden
                        },
                        compactMode = compactMode
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Title and Star Badge if default
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = unit.label.take(2),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (isDefault) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(14.dp)
                                    .offset(x = 4.dp, y = 4.dp),
                                tint = Color(0xFFFFB300)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "[${unit.label}] ${unit.description}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Used: $usageCount • ${stringResource(R.string.unit_decimal_places_abbr)}: ${unit.decimalPlaces}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (unit.isHidden) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    IconButton(onClick = { isEditing = true }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.options_edit_unit),
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
