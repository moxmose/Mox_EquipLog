package com.moxmose.moxequiplog.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.Section
import com.moxmose.moxequiplog.utils.AppConstants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SelectionDropdown(
    label: String,
    selectedItem: T?,
    items: List<T>,
    allSections: List<Section>,
    onItemSelected: (T) -> Unit,
    itemDescription: @Composable (T) -> String,
    itemPhotoUri: (T) -> String?,
    itemIconIdentifier: (T) -> String?,
    itemSectionId: (T) -> Int?,
    category: String,
    categoryColor: Color,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    showSectionBadge: Boolean = true,
    isDismissed: (T) -> Boolean = { false }
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        val selectedValue = selectedItem?.let { itemDescription(it) } ?: placeholder
        
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = {
                ImageIcon(
                    photoUri = selectedItem?.let(itemPhotoUri),
                    iconIdentifier = selectedItem?.let(itemIconIdentifier),
                    modifier = Modifier.size(24.dp),
                    category = category,
                    borderColor = categoryColor,
                    contentPadding = 2.dp,
                    tint = if (category == Category.SECTIONS) categoryColor else null
                )
            },
            suffix = {
                if (showSectionBadge && allSections.size > 1) {
                    selectedItem?.let { item ->
                        val sectionId = itemSectionId(item)
                        allSections.find { it.id == sectionId }?.let { section ->
                            SectionBadge(
                                name = section.name,
                                colorHex = section.color
                            )
                        }
                    }
                }
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            colors = if (selectedItem?.let(isDismissed) == true) 
                OutlinedTextFieldDefaults.colors(
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                ) else OutlinedTextFieldDefaults.colors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                val dismissed = isDismissed(item)
                DropdownMenuItem(
                    modifier = Modifier.graphicsLayer(alpha = if (dismissed) 0.5f else 1f),
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = itemDescription(item).takeIf { it.isNotBlank() } ?: "",
                                modifier = Modifier.weight(1f)
                            )
                            if (showSectionBadge && allSections.size > 1) {
                                val sectionId = itemSectionId(item)
                                allSections.find { it.id == sectionId }?.let { section ->
                                    SectionBadge(
                                        name = section.name,
                                        colorHex = section.color,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                            if (dismissed) {
                                Text(
                                    text = " ${stringResource(R.string.dismissed_suffix)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    },
                    leadingIcon = {
                        val itemColor = if (category == Category.SECTIONS) {
                            val colorHex = (item as? Section)?.color
                            try { colorHex?.toColorInt()?.let { Color(it) } ?: categoryColor } catch (_: Exception) { categoryColor }
                        } else categoryColor

                        ImageIcon(
                            photoUri = itemPhotoUri(item),
                            iconIdentifier = itemIconIdentifier(item),
                            modifier = Modifier.size(24.dp),
                            category = category,
                            borderColor = itemColor,
                            contentPadding = 2.dp,
                            tint = if (category == Category.SECTIONS) itemColor else null
                        )
                    },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}
