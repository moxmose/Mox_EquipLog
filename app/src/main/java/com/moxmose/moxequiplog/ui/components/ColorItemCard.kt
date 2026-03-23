package com.moxmose.moxequiplog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moxmose.moxequiplog.data.local.AppColor
import com.moxmose.moxequiplog.utils.AppConstants

@Composable
fun ColorItemCard(
    color: AppColor,
    isSelected: Boolean,
    onColorSelected: () -> Unit,
    onUpdateColor: (AppColor) -> Unit,
    onToggleVisibility: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember(color.name) { mutableStateOf(color.name) }
    var editedHex by remember(color.hexValue) { mutableStateOf(color.hexValue) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { if (!isEditing) onColorSelected() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
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
                        label = { Text("Nome") },
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
            }
            if (isEditing) {
                IconButton(onClick = {
                    onUpdateColor(color.copy(name = editedName, hexValue = editedHex))
                    isEditing = false
                }) {
                    Icon(Icons.Default.Done, contentDescription = "Salva colore")
                }
            } else {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        imageVector = if (color.hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (color.hidden) "Mostra" else "Nascondi"
                    )
                }
                IconButton(onClick = { isEditing = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Modifica colore")
                }
            }
        }
    }
}
