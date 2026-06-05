package com.moxmose.moxequiplog.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            .clickable { if (!isEditing) onUnitSelected() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (isEditing) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        Text(
                            text = "[${unit.label}] ${unit.description}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
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
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
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
                        IconButton(onClick = { isEditing = false }) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            } else {
                // Riga 1: [km] Kilometers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "[${unit.label}] ${unit.description}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Riga 2: Azioni allineate a destra con icone ridotte + info utilizzi e decimali
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Used: $usageCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${stringResource(R.string.unit_decimal_places_abbr)}: ${unit.decimalPlaces}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onUnitSelected,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isDefault) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        
                        IconButton(
                            onClick = onToggleVisibility,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (unit.isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = stringResource(R.string.options_show_hide),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        IconButton(
                            onClick = { isEditing = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.options_edit_unit),
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        if (!unit.isSystem) {
                            IconButton(
                                onClick = onDeleteUnit,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
