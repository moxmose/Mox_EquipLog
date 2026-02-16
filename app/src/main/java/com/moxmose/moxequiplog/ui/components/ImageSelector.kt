package com.moxmose.moxequiplog.ui.components

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotInterested
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.Image
import com.moxmose.moxequiplog.ui.options.EquipmentIconProvider

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageSelector(
    photoUri: String?,
    iconIdentifier: String?,
    onImageSelected: (String?, String?) -> Unit,
    modifier: Modifier = Modifier,
    category: String? = null,
    imageLibrary: List<Image> = emptyList(),
    categories: List<Category> = emptyList(),
    onAddImage: ((String, String) -> Unit)? = null,
    onRemoveImage: ((String, String) -> Unit)? = null,
    onUpdateImageOrder: ((List<Image>) -> Unit)? = null,
    onToggleImageVisibility: ((String, String) -> Unit)? = null,
    onSetDefaultInCategory: ((String, String?, String?) -> Unit)? = null,
    isPhotoUsed: (suspend (String) -> Boolean)? = null,
    isPrefsMode: Boolean = false,
    onDismissRequest: () -> Unit = {},
) {

    if (isPrefsMode) {
        ImagePickerDialog(
            onDismissRequest = onDismissRequest,
            photoUri = photoUri,
            iconIdentifier = iconIdentifier,
            onImageSelected = {
                onImageSelected(it.first, it.second)
                onDismissRequest()
            },
            imageLibrary = imageLibrary,
            categories = categories,
            onAddImage = onAddImage,
            onRemoveImage = onRemoveImage,
            onUpdateImageOrder = onUpdateImageOrder,
            onToggleImageVisibility = onToggleImageVisibility,
            onSetDefaultInCategory = onSetDefaultInCategory,
            isPhotoUsed = isPhotoUsed,
            isPrefsMode = isPrefsMode,
            forcedCategory = if (isPrefsMode) null else category
        )
    } else {
        var showPicker by remember { mutableStateOf(false) }
        if (showPicker) {
            ImagePickerDialog(
                onDismissRequest = { showPicker = false },
                photoUri = photoUri,
                iconIdentifier = iconIdentifier,
                onImageSelected = {
                    onImageSelected(it.first, it.second)
                    showPicker = false
                },
                imageLibrary = imageLibrary,
                categories = categories,
                onAddImage = onAddImage,
                onRemoveImage = onRemoveImage,
                onUpdateImageOrder = onUpdateImageOrder,
                onToggleImageVisibility = onToggleImageVisibility,
                onSetDefaultInCategory = onSetDefaultInCategory,
                isPhotoUsed = isPhotoUsed,
                isPrefsMode = isPrefsMode,
                forcedCategory = if (isPrefsMode) null else category
            )
        }

        Box(
            modifier = modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .clickable { showPicker = true },
            contentAlignment = Alignment.Center
        ) {
            when {
                photoUri != null -> {
                    AsyncImage(model = photoUri, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                }
                iconIdentifier != null -> {
                    Icon(imageVector = EquipmentIconProvider.getIcon(iconIdentifier, category ?: ""), contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                }
                else -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.NotInterested, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Nothing", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePickerDialog(
    onDismissRequest: () -> Unit,
    photoUri: String?,
    iconIdentifier: String?,
    onImageSelected: (Pair<String?, String?>) -> Unit,
    imageLibrary: List<Image>,
    categories: List<Category>,
    onAddImage: ((String, String) -> Unit)?,
    onRemoveImage: ((String, String) -> Unit)?,
    onUpdateImageOrder: ((List<Image>) -> Unit)?,
    onToggleImageVisibility: ((String, String) -> Unit)?,
    onSetDefaultInCategory: ((String, String?, String?) -> Unit)?,
    isPhotoUsed: (suspend (String) -> Boolean)?,
    isPrefsMode: Boolean,
    forcedCategory: String?
) {
    var imageToDelete by remember { mutableStateOf<Image?>(null) }
    var showHidden by remember { mutableStateOf(isPrefsMode) }
    var currentFilterCategory by remember { mutableStateOf(forcedCategory ?: "ALL") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, flags)
                val uriString = it.toString()
                val targetCategory = forcedCategory ?: currentFilterCategory
                onAddImage?.invoke(uriString, targetCategory)
            } catch (e: SecurityException) {
                Log.e("ImageSelector", "Failed to take permission", e)
            }
        }
    }

    imageToDelete?.let {
        var isUsed by remember { mutableStateOf(false) }
        LaunchedEffect(it.uri) { isUsed = isPhotoUsed?.invoke(it.uri) ?: false }
        val categoryName = categories.find { cat -> cat.id == it.category }?.name ?: it.category

        AlertDialog(
            onDismissRequest = { imageToDelete = null },
            title = { Text("Elimina immagine") },
            text = {
                Column {
                    Text("Eliminare definitivamente dalla libreria di '$categoryName'?")
                    if (isUsed) {
                        Spacer(Modifier.height(8.dp))
                        Text("ATTENZIONE: In uso!", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveImage?.invoke(it.uri, it.category)
                        if (photoUri == it.uri) onImageSelected(Pair(null, null))
                        imageToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Elimina") }
            },
            dismissButton = { TextButton(onClick = { imageToDelete = null }) { Text("Annulla") } }
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = if (isPrefsMode) "Gestione Libreria" else "Seleziona Immagine",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(550.dp)) {
                if (forcedCategory == null || isPrefsMode) {
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = if (currentFilterCategory == "ALL") "Tutte le categorie" else categories.find { it.id == currentFilterCategory }?.name ?: currentFilterCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Filtra per Categoria") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Tutte le categorie") },
                                onClick = { currentFilterCategory = "ALL"; dropdownExpanded = false }
                            )
                            categories.sortedBy { it.name }.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = { currentFilterCategory = cat.id; dropdownExpanded = false }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { imagePickerLauncher.launch(arrayOf("image/*")) },
                        modifier = Modifier.weight(1f),
                        enabled = onAddImage != null && (currentFilterCategory != "ALL" || forcedCategory != null)
                    ) {
                        Icon(Icons.Default.AddAPhoto, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Importa")
                    }

                    IconButton(
                        onClick = { showHidden = !showHidden },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (showHidden) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = if (showHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = if (showHidden) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                val isMixedMode = currentFilterCategory == "ALL"
                val canDrag = onUpdateImageOrder != null && !isMixedMode
                if (isMixedMode && isPrefsMode) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(8.dp))
                            Text("Clicca un elemento per impostarlo come default.", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Spacer(Modifier.width(8.dp))
                            Text("Drag & drop e importazione non disponibili. Seleziona una categoria per abilitarli.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(8.dp))
                            val text = when {
                                isPrefsMode && canDrag -> "Clicca per default, trascina per ordinare, importa nuove immagini."
                                isPrefsMode && !canDrag -> "Clicca per impostare come default."
                                !isPrefsMode && canDrag -> "Tocca per selezionare, trascina per ordinare."
                                else -> "Tocca per selezionare."
                            }
                            Text(text, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                val filteredAndSortedImages = imageLibrary
                    .filter { image ->
                        val matchesCategory = currentFilterCategory == "ALL" || image.category == currentFilterCategory
                        matchesCategory && (showHidden || !image.hidden)
                    }
                    .sortedWith(compareBy({ it.category }, { it.displayOrder }))

                DraggableLazyGrid(
                    items = filteredAndSortedImages,
                    onMove = { from, to ->
                        if (canDrag) {
                            val newList = filteredAndSortedImages.toMutableList().apply { add(to, removeAt(from)) }
                            onUpdateImageOrder?.invoke(newList.mapIndexed { index, m -> m.copy(displayOrder = index) })
                        }
                    },
                    canDrag = canDrag,
                    key = { _, m -> "${m.category}:${m.uri}" },
                    itemContent = { image ->
                        val uriKey = image.uri.removePrefix("icon:")
                        val cat = categories.find { it.id == image.category }
                        val catColorHex = cat?.color ?: "#808080"
                        val catColor = Color(android.graphics.Color.parseColor(catColorHex))

                        val isSelected = if (isPrefsMode) {
                            if (uriKey == "none") {
                                cat?.defaultIconIdentifier == null && cat?.defaultPhotoUri == null
                            } else {
                                (image.imageType == "ICON" && uriKey == cat?.defaultIconIdentifier) ||
                                (image.imageType == "IMAGE" && image.uri == cat?.defaultPhotoUri)
                            }
                        } else {
                            if (uriKey == "none") {
                                photoUri == null && iconIdentifier == null
                            } else {
                                (image.imageType == "ICON" && iconIdentifier == uriKey) ||
                                (image.imageType == "IMAGE" && photoUri == image.uri)
                            }
                        }

                        val isDefaultInCat = if (isPrefsMode) {
                            false // Gestito da isSelected
                        } else {
                            if (uriKey == "none") {
                                cat?.defaultIconIdentifier == null && cat?.defaultPhotoUri == null
                            } else {
                                (image.imageType == "ICON" && uriKey == cat?.defaultIconIdentifier) ||
                                (image.imageType == "IMAGE" && image.uri == cat?.defaultPhotoUri)
                            }
                        }

                        ImageGridItem(
                            image = image,
                            categoryColor = catColor,
                            isSelected = isSelected,
                            isDefault = isDefaultInCat,
                            isHidden = image.hidden,
                            isManagementMode = isPrefsMode || forcedCategory == null,
                            onSelect = {
                                if (isPrefsMode) {
                                    if (image.imageType == "ICON") {
                                        onSetDefaultInCategory?.invoke(image.category, if (uriKey == "none") null else uriKey, null)
                                    } else {
                                        onSetDefaultInCategory?.invoke(image.category, null, image.uri)
                                    }
                                } else {
                                    if (image.imageType == "ICON") {
                                        if (uriKey == "none") onImageSelected(Pair(null, null))
                                        else onImageSelected(Pair(uriKey, null))
                                    } else {
                                        onImageSelected(Pair(null, image.uri))
                                    }
                                }
                            },
                            onToggleVisibility = {
                                onToggleImageVisibility?.invoke(image.uri, image.category)
                            },
                            onDelete = { if (image.imageType == "IMAGE") imageToDelete = image }
                        )
                    }
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismissRequest) { Text("Chiudi") } }
    )
}


@Composable
fun ImageGridItem(
    image: Image,
    categoryColor: Color,
    isSelected: Boolean,
    isDefault: Boolean,
    isHidden: Boolean,
    isManagementMode: Boolean,
    onSelect: () -> Unit,
    onToggleVisibility: () -> Unit,
    onDelete: () -> Unit
) {
    Box(contentAlignment = Alignment.TopEnd) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = when {
                        isSelected -> 5.dp
                        isDefault -> 1.dp
                        else -> 2.dp
                    },
                    color = if (isSelected || isDefault) categoryColor else categoryColor.copy(alpha = 0.4f),
                    shape = CircleShape
                )
                .clickable { onSelect() }
                .graphicsLayer(alpha = if (isHidden) 0.5f else 1f),
            contentAlignment = Alignment.Center
        ) {
            val uriKey = image.uri.removePrefix("icon:")
            if (image.imageType == "ICON") {
                if (uriKey == "none") {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.NotInterested, null, modifier = Modifier.size(24.dp), tint = if (isHidden) MaterialTheme.colorScheme.onSurface.copy(0.3f) else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Nothing", style = MaterialTheme.typography.labelSmall, color = if (isHidden) MaterialTheme.colorScheme.onSurface.copy(0.3f) else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Icon(
                        imageVector = EquipmentIconProvider.getIcon(uriKey, image.category),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (isHidden) MaterialTheme.colorScheme.onSurface.copy(0.3f) else MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                AsyncImage(model = image.uri, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
            }
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(categoryColor)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = Color.White)
            }
        }

        if (isManagementMode) {
            Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.BottomCenter) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 2.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(0.8f), CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                ) {
                    IconButton(onClick = onToggleVisibility, modifier = Modifier.size(22.dp)) {
                        Icon(if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, Modifier.size(12.dp))
                    }
                    if (image.imageType == "IMAGE") {
                        IconButton(onClick = onDelete, modifier = Modifier.size(22.dp)) {
                            Icon(Icons.Default.Delete, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
