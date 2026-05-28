package com.moxmose.moxequiplog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.AppColor
import com.moxmose.moxequiplog.data.local.Section
import com.moxmose.moxequiplog.ui.options.EquipmentIconProvider
import com.moxmose.moxequiplog.utils.AppConstants

@Composable
fun SectionItemCard(
    section: Section,
    allColors: List<AppColor>,
    onUpdateSection: (Section) -> Unit,
    onDeleteSection: (Section) -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val sectionColor = remember(section.color) {
        try {
            section.color?.let { Color(it.toColorInt()) } ?: Color.Gray
        } catch (e: Exception) {
            Color.Gray
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(sectionColor)
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = EquipmentIconProvider.getIcon(section.iconIdentifier),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = section.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (section.id == AppConstants.DEFAULT_SECTION_ID) {
                        Text(text = "(Predefinita)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Row {
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                if (section.id != AppConstants.DEFAULT_SECTION_ID) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        var name by remember { mutableStateOf(section.name) }
        var selectedIcon by remember { mutableStateOf(section.iconIdentifier ?: "build") }
        var selectedColor by remember { mutableStateOf(section.color ?: "#808080") }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Modifica Sezione") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome Sezione") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Scegli Icona", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("build", "car", "moto", "bike", "medical", "flight").forEach { iconId ->
                            IconButton(
                                onClick = { selectedIcon = iconId },
                                modifier = Modifier
                                    .size(40.dp)
                                    .border(
                                        width = if (selectedIcon == iconId) 2.dp else 0.dp,
                                        color = if (selectedIcon == iconId) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(EquipmentIconProvider.getIcon(iconId), contentDescription = null)
                            }
                        }
                    }

                    Text("Scegli Colore", style = MaterialTheme.typography.labelMedium)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allColors.filter { !it.hidden }) { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(color.hexValue.toColorInt()))
                                    .border(
                                        width = if (selectedColor == color.hexValue) 2.dp else 0.dp,
                                        color = if (selectedColor == color.hexValue) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = color.hexValue }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateSection(section.copy(name = name, iconIdentifier = selectedIcon, color = selectedColor))
                        showEditDialog = false
                    },
                    enabled = name.isNotBlank()
                ) { Text("Salva") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Annulla") }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Elimina Sezione") },
            text = { Text("Sei sicuro di voler eliminare la sezione '${section.name}'? Tutti i mezzi associati verranno spostati nella sezione Generale.") },
            confirmButton = {
                TextButton(onClick = { onDeleteSection(section); showDeleteConfirm = false }) {
                    Text("Elimina", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Annulla") }
            }
        )
    }
}
