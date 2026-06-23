package com.moxmose.moxequiplog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import coil.compose.AsyncImage
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.MeasurementUnit
import com.moxmose.moxequiplog.data.local.Section
import com.moxmose.moxequiplog.data.local.TimeGranularity
import com.moxmose.moxequiplog.ui.options.EquipmentIconProvider

@Composable
fun FullImageDialog(photoUri: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(R.string.button_ok))
            }
        },
        modifier = Modifier.padding(16.dp),
        text = {
            AsyncImage(
                model = photoUri,
                contentDescription = stringResource(R.string.full_size_image),
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeGranularitySelector(
    selected: TimeGranularity,
    onSelected: (TimeGranularity) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = formatTimeGranularity(selected),
            onValueChange = {},
            readOnly = true,
            label = label?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable),
            textStyle = MaterialTheme.typography.bodyMedium
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TimeGranularity.entries.forEach { entry ->
                DropdownMenuItem(text = { Text(formatTimeGranularity(entry)) }, onClick = { onSelected(entry); expanded = false })
            }
        }
    }
}

fun formatTimeGranularity(granularity: TimeGranularity): String {
    return when (granularity) {
        TimeGranularity.MINUTES_5 -> "5 Minutes"
        TimeGranularity.MINUTES_15 -> "15 Minutes"
        else -> granularity.name.lowercase().replaceFirstChar { it.titlecase() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionSelector(
    allSections: List<Section>,
    selectedSectionId: Int,
    onSectionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showDismissed: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedSection = allSections.find { it.id == selectedSectionId } ?: allSections.firstOrNull { it.id == 1 }
    
    val sectionName = selectedSection?.let {
        if (it.dismissed) "${it.name} ${stringResource(R.string.dismissed_suffix)}" else it.name
    } ?: stringResource(R.string.section_common)

    ExposedDropdownMenuBox(
        expanded = expanded, 
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = sectionName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.options_manage_sections)) },
            leadingIcon = {
                val color = remember(selectedSection?.color) {
                    try { selectedSection?.color?.toColorInt()?.let { Color(it) } ?: Color.Gray } catch (_: Exception) { Color.Gray }
                }
                ImageIcon(
                    photoUri = selectedSection?.photoUri,
                    iconIdentifier = selectedSection?.iconIdentifier,
                    modifier = Modifier.size(24.dp),
                    category = Category.SECTIONS,
                    borderColor = color,
                    contentPadding = 2.dp,
                    tint = color
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier.fillMaxWidth().menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            val filteredSections = allSections.filter { 
                !it.dismissed || showDismissed || it.id == selectedSectionId
            }
            filteredSections.forEach { section ->
                val alpha = if (section.dismissed) 0.5f else 1.0f
                DropdownMenuItem(
                    modifier = Modifier.graphicsLayer(alpha = alpha),
                    text = { 
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            val color = remember(section.color) {
                                try { section.color?.toColorInt()?.let { Color(it) } ?: Color.Gray } catch (_: Exception) { Color.Gray }
                            }
                            ImageIcon(
                                photoUri = section.photoUri,
                                iconIdentifier = section.iconIdentifier,
                                modifier = Modifier.size(24.dp),
                                category = Category.SECTIONS,
                                borderColor = color,
                                contentPadding = 2.dp,
                                tint = color
                            )
                            Text(
                                text = section.name,
                                fontWeight = if (section.id == selectedSectionId) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (section.dismissed) {
                                Text(
                                    text = " ${stringResource(R.string.dismissed_suffix)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    onClick = { onSectionSelected(section.id); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitSelector(
    measurementUnits: List<MeasurementUnit>,
    selectedUnitId: Int,
    onUnitSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedUnit = measurementUnits.find { it.id == selectedUnitId }
    ExposedDropdownMenuBox(
        expanded = expanded, 
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = if (selectedUnit != null) {
                if (selectedUnit.description.isNotBlank()) "${selectedUnit.label} - ${selectedUnit.description}"
                else selectedUnit.label
            } else "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label ?: stringResource(R.string.measurement_unit)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            measurementUnits.filter { !it.isHidden }.forEach { unit ->
                DropdownMenuItem(
                    text = { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text(unit.label, fontWeight = FontWeight.Bold); if (unit.description.isNotBlank()) Text(unit.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                    onClick = { onUnitSelected(unit.id); expanded = false }
                )
            }
        }
    }
}
