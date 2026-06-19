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
import androidx.compose.ui.unit.dp
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.AppColor
import com.moxmose.moxequiplog.ui.components.CommonActionButtons
import com.moxmose.moxequiplog.utils.AppConstants

@Composable
fun ColorItemCard(
    color: AppColor,
    isSelected: Boolean,
    onColorSelected: () -> Unit,
    onUpdateColor: (AppColor) -> Unit,
    onDeleteColor: (AppColor) -> Unit,
    onToggleVisibility: () -> Unit,
    showReportVisibility: Boolean = false,
    canDelete: Boolean = true,
    compactMode: Boolean = false
) {
    var isEditing by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var editedName by remember(color.name) { mutableStateOf(color.name) }
    var editedHex by remember(color.hexValue) { mutableStateOf(color.hexValue) }
    var editedHidden by remember(color.hidden) { mutableStateOf(color.hidden) }
    var editedReportHidden by remember(color.reportHidden) { mutableStateOf(color.reportHidden) }

    val isHidden = if (showReportVisibility) color.reportHidden else color.hidden
    val currentEditedHidden = if (showReportVisibility) editedReportHidden else editedHidden

    val cardAlpha = if (isEditing) (if (currentEditedHidden) 0.5f else 1f) else (if (isHidden) 0.5f else 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .graphicsLayer(alpha = cardAlpha)
            .clickable { if (!isEditing) onColorSelected() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(color.hexValue)))
                        .border(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                )
                Spacer(Modifier.width(16.dp))
                if (isEditing) {
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = editedName,
                            onValueChange = { if (it.length <= AppConstants.COLOR_NAME_MAX_LENGTH) editedName = it },
                            label = { Text(stringResource(R.string.color_name_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        if (!color.isDefault) {
                            OutlinedTextField(
                                value = editedHex,
                                onValueChange = { if (it.length <= 7) editedHex = it },
                                label = { Text("HEX") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(color.name, style = MaterialTheme.typography.bodyLarge)
                        Text(color.hexValue, style = MaterialTheme.typography.bodySmall)
                    }
                    
                    IconButton(onClick = { isEditing = true }) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (isEditing) {
                Spacer(modifier = Modifier.height(12.dp))
                val isDirty = editedName != color.name || editedHex != color.hexValue || editedHidden != color.hidden || editedReportHidden != color.reportHidden
                CommonActionButtons(
                    onConfirm = {
                        onUpdateColor(color.copy(name = editedName, hexValue = editedHex, hidden = editedHidden, reportHidden = editedReportHidden))
                        isEditing = false
                    },
                    onDismiss = { 
                        isEditing = false
                        // Resetting local state to ensure reversibility when re-entering edit mode
                        editedName = color.name
                        editedHex = color.hexValue
                        editedHidden = color.hidden
                        editedReportHidden = color.reportHidden
                    },
                    confirmText = stringResource(R.string.save_equipment),
                    confirmIcon = Icons.Default.Save,
                    showDelete = !color.isDefault && canDelete,
                    onDelete = { showDeleteConfirm = true },
                    showArchive = true,
                    onArchive = {
                        if (showReportVisibility) editedReportHidden = !editedReportHidden
                        else editedHidden = !editedHidden
                    },
                    archiveIcon = if (currentEditedHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    showUndo = isDirty,
                    onUndo = {
                        editedName = color.name
                        editedHex = color.hexValue
                        editedHidden = color.hidden
                        editedReportHidden = color.reportHidden
                    },
                    compactMode = compactMode
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.button_delete)) },
            text = { Text("Are you sure you want to permanently delete this color?") },
            confirmButton = {
                TextButton(onClick = { 
                    onDeleteColor(color)
                    showDeleteConfirm = false 
                }) {
                    Text(stringResource(R.string.button_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.button_cancel)) }
            }
        )
    }
}
