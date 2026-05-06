package com.moxmose.moxequiplog.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.MeasurementUnit
import com.moxmose.moxequiplog.utils.AppConstants

@Composable
fun UnitItemCard(
    unit: MeasurementUnit,
    isDefault: Boolean,
    onUnitSelected: () -> Unit,
    onUpdateUnit: (MeasurementUnit) -> Unit,
    onToggleVisibility: () -> Unit,
    onDeleteUnit: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedLabel by remember(unit.label) { mutableStateOf(unit.label) }
    var editedDescription by remember(unit.description) { mutableStateOf(unit.description) }
    var editedDecimalPlaces by remember(unit.decimalPlaces) { mutableIntStateOf(unit.decimalPlaces) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { if (!isEditing) onUnitSelected() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isDefault) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = if (isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(Modifier.width(16.dp))
            
            if (isEditing) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!unit.isSystem) {
                        OutlinedTextField(
                            value = editedLabel,
                            onValueChange = { if (it.length <= AppConstants.UNIT_LABEL_MAX_LENGTH) editedLabel = it },
                            label = { Text(stringResource(R.string.options_unit_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editedDescription,
                            onValueChange = { if (it.length <= AppConstants.UNIT_DESCRIPTION_MAX_LENGTH) editedDescription = it },
                            label = { Text(stringResource(R.string.options_unit_desc_placeholder)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(unit.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(unit.description, style = MaterialTheme.typography.bodySmall)
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
                }
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(unit.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                            Text(unit.decimalPlaces.toString(), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (unit.description.isNotBlank()) {
                        Text(unit.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (isEditing) {
                IconButton(onClick = {
                    onUpdateUnit(unit.copy(
                        label = editedLabel, 
                        description = editedDescription,
                        decimalPlaces = editedDecimalPlaces
                    ))
                    isEditing = false
                }) {
                    Icon(Icons.Default.Done, contentDescription = stringResource(R.string.options_save_unit))
                }
            } else {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        imageVector = if (unit.isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = stringResource(R.string.options_show_hide)
                    )
                }
                IconButton(onClick = { isEditing = true }) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.options_edit_unit))
                }
                if (!unit.isSystem) {
                    IconButton(onClick = onDeleteUnit) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
