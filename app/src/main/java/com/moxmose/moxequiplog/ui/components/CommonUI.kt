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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.moxmose.moxequiplog.R

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
    isDialog: Boolean = false,
    modifier: Modifier = Modifier
) {
    val horizontalSpacing = if (isDialog) 4.dp else 8.dp
    val buttonPadding = if (isDialog) PaddingValues(horizontal = 8.dp, vertical = 8.dp) else ButtonDefaults.ContentPadding

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalArrangement = Arrangement.Center,
        maxItemsInEachRow = Int.MAX_VALUE
    ) {
        // Left side actions (Secondary - Icons only to save space)
        Row(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
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
        }

        // Right side actions (Primary - Icon + Text)
        Row(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .weight(1f), // Occupa lo spazio rimanente sulla riga corrente
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing, Alignment.End), // Allinea il contenuto a destra
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onDismiss,
                contentPadding = buttonPadding
            ) {
                Icon(dismissIcon ?: Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(if (isDialog) 4.dp else 8.dp))
                Text(dismissText)
            }
            Button(
                onClick = onConfirm,
                enabled = confirmEnabled,
                contentPadding = buttonPadding
            ) {
                Icon(confirmIcon ?: Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(if (isDialog) 4.dp else 8.dp))
                Text(confirmText)
            }
        }
    }
}
