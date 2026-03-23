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
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = editedLabel,
                        onValueChange = { editedLabel = it },
                        label = { Text(stringResource(R.string.options_unit_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editedDescription,
                        onValueChange = { editedDescription = it },
                        label = { Text(stringResource(R.string.options_unit_desc_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Text(unit.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (unit.description.isNotBlank()) {
                        Text(unit.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (isEditing) {
                IconButton(onClick = {
                    onUpdateUnit(unit.copy(label = editedLabel, description = editedDescription))
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
                if (!unit.isSystem) {
                    IconButton(onClick = { isEditing = true }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.options_edit_unit))
                    }
                    IconButton(onClick = onDeleteUnit) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Icon(
                        Icons.Default.Lock, 
                        contentDescription = null, 
                        modifier = Modifier.size(40.dp).padding(8.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
