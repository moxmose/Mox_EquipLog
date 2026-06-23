package com.moxmose.moxequiplog.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.core.graphics.toColorInt
import com.moxmose.moxequiplog.R

@Composable
fun SectionBadge(
    name: String,
    colorHex: String?,
    modifier: Modifier = Modifier
) {
    val sectionColor = remember(colorHex) {
        try {
            colorHex?.toColorInt()?.let { Color(it) } ?: Color.Gray
        } catch (_: Exception) {
            Color.Gray
        }
    }
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = sectionColor.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, sectionColor.copy(alpha = 0.5f))
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = sectionColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun CommonActionButtons(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String,
    dismissText: String = stringResource(R.string.button_cancel),
    confirmIcon: ImageVector? = Icons.Default.Save,
    dismissIcon: ImageVector? = Icons.Default.Cancel,
    confirmEnabled: Boolean = true,
    showDelete: Boolean = false,
    onDelete: (() -> Unit)? = null,
    deleteIcon: ImageVector = Icons.Default.Delete,
    showClone: Boolean = false,
    onClone: (() -> Unit)? = null,
    showArchive: Boolean = false,
    onArchive: (() -> Unit)? = null,
    archiveIcon: ImageVector = Icons.Default.Visibility,
    showDefault: Boolean = false,
    isDefault: Boolean = false,
    onToggleDefault: (() -> Unit)? = null,
    defaultEnabled: Boolean = true,
    showUndo: Boolean = false,
    onUndo: (() -> Unit)? = null,
    undoIcon: ImageVector = Icons.Default.Undo,
    compactMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val hasSecondaryActions = showDelete || showClone || showArchive || showDefault || showUndo
    val useCompactLayout = compactMode
    
    val horizontalSpacing = if (useCompactLayout) 4.dp else 8.dp
    val buttonPadding = if (useCompactLayout) PaddingValues(horizontal = 8.dp, vertical = 8.dp) else ButtonDefaults.ContentPadding

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        // Usiamo End come allineamento predefinito per le righe del FlowRow
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing, Alignment.End),
        verticalArrangement = Arrangement.Center,
        maxItemsInEachRow = Int.MAX_VALUE
    ) {
        // Blocco Sinistro: Icone (Azioni Secondarie)
        if (hasSecondaryActions) {
            Row(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    // Il weight(1f) è fondamentale: spinge i bottoni a destra se sulla stessa riga,
                    // o espande le icone a sinistra se i bottoni vanno a capo.
                    .weight(1f),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showDelete && onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(deleteIcon, contentDescription = stringResource(R.string.button_delete), tint = MaterialTheme.colorScheme.error)
                    }
                }
                if (showArchive && onArchive != null) {
                    IconButton(onClick = onArchive) {
                        Icon(archiveIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (showClone && onClone != null) {
                    IconButton(onClick = onClone) {
                        Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.button_clone), tint = MaterialTheme.colorScheme.secondary)
                    }
                }
                if (showUndo && onUndo != null) {
                    IconButton(onClick = onUndo) {
                        Icon(undoIcon, contentDescription = stringResource(R.string.filter_reset), tint = MaterialTheme.colorScheme.secondary)
                    }
                }
                if (showDefault && onToggleDefault != null) {
                    IconButton(onClick = onToggleDefault, enabled = defaultEnabled) {
                        Icon(
                            imageVector = if (isDefault) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Default",
                            tint = if (!defaultEnabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                   else if (isDefault) Color(0xFFFFB300) 
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Blocco Destro: Pulsanti Primari (Uniti in un Row per non separarsi mai)
        Row(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onDismiss,
                contentPadding = buttonPadding
            ) {
                if (!useCompactLayout) {
                    Icon(dismissIcon ?: Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(dismissText, style = if (useCompactLayout) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium)
            }
            
            Button(
                onClick = onConfirm,
                enabled = confirmEnabled,
                contentPadding = buttonPadding
            ) {
                if (!useCompactLayout) {
                    Icon(confirmIcon ?: Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(confirmText, style = if (useCompactLayout) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
