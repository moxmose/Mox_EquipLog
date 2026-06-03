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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.AppColor
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.Image
import com.moxmose.moxequiplog.data.local.Section
import com.moxmose.moxequiplog.ui.options.CategoryUiState
import com.moxmose.moxequiplog.ui.options.EquipmentIconProvider
import com.moxmose.moxequiplog.utils.AppConstants

@Composable
fun SectionItemCard(
    section: Section,
    allColors: List<AppColor>,
    allImages: List<Image>,
    categoriesUiState: List<CategoryUiState>,
    onUpdateSection: (Section) -> Unit,
    onDeleteSection: (Section) -> Unit,
    onShowColorManager: (String, (String) -> Unit) -> Unit,
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

    val cardAlpha = if (section.dismissed) 0.5f else 1f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .graphicsLayer(alpha = cardAlpha),
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
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .border(2.dp, sectionColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    ImageIcon(
                        photoUri = section.photoUri,
                        iconIdentifier = section.iconIdentifier,
                        modifier = Modifier.fillMaxSize(),
                        category = Category.SECTIONS,
                        borderColor = null,
                        contentPadding = 4.dp,
                        tint = sectionColor
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    val name = if (section.id == AppConstants.DEFAULT_SECTION_ID) {
                        stringResource(R.string.section_general)
                    } else {
                        section.name
                    }
                    Text(text = name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (section.dismissed) {
                        Text(text = stringResource(R.string.dismissed_suffix), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Row {
                IconButton(onClick = {
                    onUpdateSection(section.copy(dismissed = !section.dismissed))
                }) {
                    Icon(
                        imageVector = if (section.dismissed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null
                    )
                }
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_section), tint = MaterialTheme.colorScheme.primary)
                }
                if (section.id != AppConstants.DEFAULT_SECTION_ID) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_section), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        var name by remember { mutableStateOf(section.name) }
        var selectedIcon by remember { mutableStateOf(section.iconIdentifier) }
        var selectedPhotoUri by remember { mutableStateOf(section.photoUri) }
        var selectedColor by remember { mutableStateOf(section.color ?: "#808080") }
        var showImagePicker by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(stringResource(R.string.edit_section)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.section_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(stringResource(R.string.select_icon), style = MaterialTheme.typography.labelMedium)
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .border(2.dp, Color(selectedColor.toColorInt()), CircleShape)
                                .clickable { showImagePicker = true },
                            contentAlignment = Alignment.Center
                        ) {
                            ImageIcon(
                                photoUri = selectedPhotoUri,
                                iconIdentifier = selectedIcon,
                                modifier = Modifier.fillMaxSize(),
                                category = Category.SECTIONS,
                                borderColor = null,
                                contentPadding = 8.dp,
                                tint = Color(selectedColor.toColorInt())
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(selectedColor.toColorInt()))
                                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable { 
                                    onShowColorManager("edit_section") { newColor ->
                                        selectedColor = newColor
                                    }
                                }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateSection(section.copy(name = name, iconIdentifier = selectedIcon, photoUri = selectedPhotoUri, color = selectedColor))
                        showEditDialog = false
                    },
                    enabled = name.isNotBlank()
                ) { Text(stringResource(R.string.save_equipment)) }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text(stringResource(R.string.button_cancel)) }
            }
        )

        if (showImagePicker) {
            val categoryColorsMap = remember(categoriesUiState) { categoriesUiState.associate { it.category.id to it.color } }
            val categoryDefaultIconsMap = remember(categoriesUiState) { categoriesUiState.associate { it.category.id to it.defaultIconIdentifier } }
            val categoryDefaultPhotosMap = remember(categoriesUiState) { categoriesUiState.associate { it.category.id to it.defaultPhotoUri } }
            val allCategories = categoriesUiState.map { it.category }.filter { 
                it.id != Category.LOGS && it.id != Category.REPORTS && it.id != Category.OPTIONS 
            }

            ImagePickerDialog(
                onDismissRequest = { showImagePicker = false },
                photoUri = selectedPhotoUri,
                iconIdentifier = selectedIcon,
                onImageSelected = { (icon, photo) ->
                    selectedIcon = icon
                    selectedPhotoUri = photo
                    showImagePicker = false
                },
                imageLibrary = allImages,
                categories = allCategories,
                categoryColors = categoryColorsMap,
                categoryDefaultIcons = categoryDefaultIconsMap,
                categoryDefaultPhotos = categoryDefaultPhotosMap,
                onAddImage = { _, _ -> },
                onRemoveImage = null,
                onUpdateImageOrder = null,
                onToggleImageVisibility = null,
                onSetDefaultInCategory = null,
                isPhotoUsed = null,
                isPrefsMode = false,
                forcedCategory = Category.SECTIONS
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_section)) },
            text = { Text(stringResource(R.string.delete_section_confirm, section.name)) },
            confirmButton = {
                TextButton(onClick = { onDeleteSection(section); showDeleteConfirm = false }) {
                    Text(stringResource(R.string.button_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.button_cancel)) }
            }
        )
    }
}
