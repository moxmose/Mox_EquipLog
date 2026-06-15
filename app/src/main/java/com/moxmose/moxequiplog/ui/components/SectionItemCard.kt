package com.moxmose.moxequiplog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    usageCount: Int,
    onUpdateSection: (Section) -> Unit,
    onCloneSection: (Section) -> Unit,
    onDeleteSection: (Section) -> Unit,
    onShowColorManager: (String, (String) -> Unit) -> Unit,
    onAddImage: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    compactMode: Boolean = false
) {
    var isEditing by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showFullImageDialog by remember { mutableStateOf<String?>(null) }

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
        Column(modifier = Modifier.padding(12.dp)) {
            if (isEditing) {
                var editedName by remember { mutableStateOf(section.name) }
                var editedIcon by remember { mutableStateOf(section.iconIdentifier) }
                var editedPhotoUri by remember { mutableStateOf(section.photoUri) }
                var editedColor by remember { mutableStateOf(section.color ?: "#808080") }
                var showImagePicker by remember { mutableStateOf(false) }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ImageIcon(
                            photoUri = editedPhotoUri,
                            iconIdentifier = editedIcon,
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { showImagePicker = true },
                            category = Category.SECTIONS,
                            borderColor = Color(editedColor.toColorInt()),
                            contentPadding = 4.dp,
                            tint = Color(editedColor.toColorInt())
                        )
                        
                        OutlinedTextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            label = { Text(stringResource(R.string.section_name)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(editedColor.toColorInt()))
                                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable { 
                                    onShowColorManager("edit_section") { newColor ->
                                        editedColor = newColor
                                    }
                                }
                        )
                    }

                    CommonActionButtons(
                        onConfirm = {
                            onUpdateSection(section.copy(name = editedName, iconIdentifier = editedIcon, photoUri = editedPhotoUri, color = editedColor))
                            isEditing = false
                        },
                        onDismiss = { isEditing = false },
                        confirmText = stringResource(R.string.save_equipment),
                        confirmIcon = Icons.Default.Save,
                        showClone = true,
                        onClone = { onCloneSection(section) },
                        showDelete = section.id != AppConstants.DEFAULT_SECTION_ID,
                        onDelete = { showDeleteConfirm = true },
                        showArchive = true,
                        onArchive = { onUpdateSection(section.copy(dismissed = !section.dismissed)) },
                        archiveIcon = if (section.dismissed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        compactMode = compactMode
                    )
                }

                if (showImagePicker) {
                    val categoryColorsMap = remember(categoriesUiState) { categoriesUiState.associate { it.category.id to it.color } }
                    val categoryDefaultIconsMap = remember(categoriesUiState) { categoriesUiState.associate { it.category.id to it.defaultIconIdentifier } }
                    val categoryDefaultPhotosMap = remember(categoriesUiState) { categoriesUiState.associate { it.category.id to it.defaultPhotoUri } }
                    val allCategories = categoriesUiState.map { it.category }.filter { 
                        it.id != Category.LOGS && it.id != Category.REPORTS && it.id != Category.OPTIONS 
                    }

                    ImagePickerDialog(
                        onDismissRequest = { showImagePicker = false },
                        photoUri = editedPhotoUri,
                        iconIdentifier = editedIcon,
                        onImageSelected = { (icon, photo) ->
                            editedIcon = icon
                            editedPhotoUri = photo
                            showImagePicker = false
                        },
                        imageLibrary = allImages,
                        categories = allCategories,
                        categoryColors = categoryColorsMap.toMutableMap().apply { put(Category.SECTIONS, editedColor) },
                        categoryDefaultIcons = categoryDefaultIconsMap,
                        categoryDefaultPhotos = categoryDefaultPhotosMap,
                        onAddImage = onAddImage,
                        onRemoveImage = null,
                        onUpdateImageOrder = null,
                        onToggleImageVisibility = null,
                        onSetDefaultInCategory = null,
                        isPhotoUsed = null,
                        isPrefsMode = false,
                        forcedCategory = Category.SECTIONS
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .border(1.dp, sectionColor.copy(alpha = 0.5f), CircleShape)
                            .clickable {
                                if (section.photoUri != null) showFullImageDialog = section.photoUri
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        ImageIcon(
                            photoUri = section.photoUri,
                            iconIdentifier = section.iconIdentifier,
                            modifier = Modifier.fillMaxSize(),
                            category = Category.SECTIONS,
                            borderColor = null,
                            contentPadding = 6.dp,
                            tint = sectionColor
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        val name = if (section.id == AppConstants.DEFAULT_SECTION_ID) {
                            stringResource(R.string.section_common)
                        } else {
                            section.name
                        }
                        
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Used: $usageCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (section.dismissed) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    IconButton(onClick = { isEditing = true }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_section),
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
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

    showFullImageDialog?.let { uri ->
        FullImageDialog(photoUri = uri, onDismiss = { showFullImageDialog = null })
    }
}
