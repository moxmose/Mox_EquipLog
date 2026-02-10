package com.moxmose.moxequiplog.ui.equipments

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.Equipment
import com.moxmose.moxequiplog.data.local.MediaIdentifier
import com.moxmose.moxequiplog.ui.components.DraggableLazyColumn
import com.moxmose.moxequiplog.ui.components.MediaPickerDialog
import com.moxmose.moxequiplog.ui.options.EquipmentIconProvider
import com.moxmose.moxequiplog.ui.options.OptionsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun EquipmentsScreen(viewModel: EquipmentsViewModel = koinViewModel(), optionsViewModel: OptionsViewModel = koinViewModel()) {
    val activeEquipments by viewModel.activeEquipments.collectAsState()
    val allEquipments by viewModel.allEquipments.collectAsState()
    val equipmentMedia by viewModel.equipmentMedia.collectAsState()
    val allCategories by optionsViewModel.allCategories.collectAsState()

    var showDismissed by rememberSaveable { mutableStateOf(false) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    val equipmentsToShow = if (showDismissed) allEquipments else activeEquipments
    val equipmentCategory = allCategories.find { it.id == "EQUIPMENT" }

    EquipmentsScreenContent(
        equipments = equipmentsToShow,
        equipmentMedia = equipmentMedia,
        allCategories = allCategories,
        defaultIcon = equipmentCategory?.defaultIconIdentifier,
        defaultPhotoUri = equipmentCategory?.defaultPhotoUri,
        equipmentCategoryColor = equipmentCategory?.color,
        onAddEquipment = viewModel::addEquipment,
        onUpdateEquipments = viewModel::updateEquipments,
        onUpdateEquipment = viewModel::updateEquipment,
        onDismissEquipment = viewModel::dismissEquipment,
        onRestoreEquipment = viewModel::restoreEquipment,
        showDismissed = showDismissed,
        onToggleShowDismissed = { showDismissed = !showDismissed },
        showAddDialog = showAddDialog,
        onShowAddDialogChange = { showAddDialog = it },
        onAddMedia = viewModel::addMedia,
        onToggleMediaVisibility = viewModel::toggleMediaVisibility
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentsScreenContent(
    equipments: List<Equipment>,
    equipmentMedia: List<com.moxmose.moxequiplog.data.local.Media>,
    allCategories: List<Category>,
    defaultIcon: String?,
    defaultPhotoUri: String?,
    equipmentCategoryColor: String?,
    showDismissed: Boolean,
    onToggleShowDismissed: () -> Unit,
    showAddDialog: Boolean,
    onShowAddDialogChange: (Boolean) -> Unit,
    onAddEquipment: (String, MediaIdentifier?) -> Unit,
    onUpdateEquipments: (List<Equipment>) -> Unit,
    onUpdateEquipment: (Equipment) -> Unit,
    onDismissEquipment: (Equipment) -> Unit,
    onRestoreEquipment: (Equipment) -> Unit,
    onAddMedia: (MediaIdentifier, String) -> Unit,
    onToggleMediaVisibility: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val equipmentsState = remember(equipments) { equipments.toMutableStateList() }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(onClick = { onShowAddDialogChange(true) }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_equipment))
                }
                Spacer(modifier = Modifier.padding(8.dp))
                FloatingActionButton(
                    onClick = onToggleShowDismissed,
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(
                        imageVector = if (showDismissed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (showDismissed) stringResource(R.string.hide_dismissed) else stringResource(R.string.show_dismissed)
                    )
                }
            }
        }
    ) { paddingValues ->
        if (showAddDialog) {
            AddEquipmentDialog(
                defaultIcon = defaultIcon,
                defaultPhotoUri = defaultPhotoUri,
                mediaLibrary = equipmentMedia,
                categories = allCategories,
                equipmentCategoryColor = equipmentCategoryColor,
                onDismissRequest = { onShowAddDialogChange(false) },
                onConfirm = { desc, identifier ->
                    onAddEquipment(desc, identifier)
                    onShowAddDialogChange(false)
                },
                onAddMedia = onAddMedia,
                onToggleMediaVisibility = onToggleMediaVisibility
            )
        }

        Column(Modifier.padding(paddingValues)) {
            Text(
                text = stringResource(R.string.hold_and_drag_to_reorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall
            )
            DraggableLazyColumn(
                items = equipmentsState,
                key = { _, equipment -> equipment.id },
                onMove = { from, to ->
                    equipmentsState.add(to, equipmentsState.removeAt(from))
                },
                onDrop = {
                    val reorderedEquipments = equipmentsState.mapIndexed { index, equipment ->
                        equipment.copy(displayOrder = index)
                    }
                    onUpdateEquipments(reorderedEquipments)
                },
                modifier = Modifier.fillMaxSize(),
                itemContent = { _, equipment ->
                    EquipmentCard(
                        equipment = equipment,
                        equipmentMedia = equipmentMedia,
                        allCategories = allCategories,
                        onUpdateEquipment = onUpdateEquipment,
                        onDismissEquipment = onDismissEquipment,
                        onRestoreEquipment = onRestoreEquipment,
                        onAddMedia = onAddMedia,
                        onToggleMediaVisibility = onToggleMediaVisibility,
                        equipmentCategoryColor = equipmentCategoryColor
                    )
                }
            )
        }
    }
}

@Composable
fun AddEquipmentDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String, MediaIdentifier?) -> Unit,
    defaultIcon: String?,
    defaultPhotoUri: String?,
    mediaLibrary: List<com.moxmose.moxequiplog.data.local.Media>,
    categories: List<Category>,
    equipmentCategoryColor: String?,
    onAddMedia: (MediaIdentifier, String) -> Unit,
    onToggleMediaVisibility: (String, String) -> Unit
) {
    var description by rememberSaveable { mutableStateOf("") }
    var photoUri by rememberSaveable { mutableStateOf<String?>(null) }
    var iconId by rememberSaveable { mutableStateOf<String?>(null) }
    var isPristine by rememberSaveable { mutableStateOf(true) }
    var showMediaSelectorDialog by remember { mutableStateOf(false) }

    if (isPristine && (defaultIcon != null || defaultPhotoUri != null)) {
        LaunchedEffect(defaultIcon, defaultPhotoUri) {
            iconId = defaultIcon
            photoUri = defaultPhotoUri
        }
    }

    if (showMediaSelectorDialog) {
        MediaPickerDialog(
            onDismissRequest = { showMediaSelectorDialog = false },
            photoUri = photoUri,
            iconIdentifier = iconId,
            onMediaSelected = { (newIconId, newPhotoUri) ->
                isPristine = false
                iconId = newIconId
                photoUri = newPhotoUri
                showMediaSelectorDialog = false
            },
            mediaLibrary = mediaLibrary,
            categories = categories,
            onAddMedia = { uri, category -> onAddMedia(MediaIdentifier.Photo(uri), category) },
            onRemoveMedia = null,
            onUpdateMediaOrder = null,
            onToggleMediaVisibility = onToggleMediaVisibility,
            onSetDefaultInCategory = null,
            isPhotoUsed = null,
            isPrefsMode = false,
            forcedCategory = "EQUIPMENT"
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = stringResource(R.string.add_a_new_equipment),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val borderColor = equipmentCategoryColor?.let {
                    try {
                        Color(android.graphics.Color.parseColor(it))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                    }
                } ?: MaterialTheme.colorScheme.primary

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .border(2.dp, borderColor, CircleShape)
                        .clickable { showMediaSelectorDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(photoUri).crossfade(true).build(),
                            contentDescription = stringResource(R.string.equipment_photo),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        val icon = EquipmentIconProvider.getIcon(iconId)
                        Icon(
                            imageVector = icon,
                            contentDescription = stringResource(R.string.equipment_photo),
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 50) description = it },
                    label = { Text(stringResource(R.string.equipment_description)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    val identifier = when {
                        photoUri != null -> MediaIdentifier.Photo(photoUri!!)
                        iconId != null -> MediaIdentifier.Icon(iconId!!)
                        else -> null
                    }
                    onConfirm(description, identifier) 
                }
            ) {
                Text(stringResource(R.string.button_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.button_cancel))
            }
        }
    )
}

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
                contentDescription = stringResource(R.string.full_size_equipment_photo),
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit
            )
        }
    )
}

@Composable
fun EquipmentCard(
    equipment: Equipment,
    equipmentMedia: List<com.moxmose.moxequiplog.data.local.Media>,
    allCategories: List<Category>,
    onUpdateEquipment: (Equipment) -> Unit,
    onDismissEquipment: (Equipment) -> Unit,
    onRestoreEquipment: (Equipment) -> Unit,
    onAddMedia: (MediaIdentifier, String) -> Unit,
    onToggleMediaVisibility: (String, String) -> Unit,
    equipmentCategoryColor: String?,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedDescription by remember(equipment.description) { mutableStateOf(equipment.description) }
    val context = LocalContext.current
    var showFullImageDialog by remember { mutableStateOf<String?>(null) }
    var showNoPictureDialog by remember { mutableStateOf(false) }
    var showMediaSelectorDialog by remember { mutableStateOf(false) }

    val cardAlpha = if (equipment.dismissed) 0.5f else 1f

    if (showNoPictureDialog) {
        AlertDialog(
            onDismissRequest = { showNoPictureDialog = false },
            title = { Text(stringResource(R.string.no_image_title)) },
            text = { Text(stringResource(R.string.no_image_message)) },
            confirmButton = {
                TextButton(onClick = { showNoPictureDialog = false }) {
                    Text(stringResource(R.string.button_ok))
                }
            }
        )
    }

    if (showMediaSelectorDialog) {
        MediaPickerDialog(
            onDismissRequest = { showMediaSelectorDialog = false },
            photoUri = equipment.photoUri,
            iconIdentifier = equipment.iconIdentifier,
            onMediaSelected = { (iconId, photoUri) ->
                onUpdateEquipment(equipment.copy(iconIdentifier = iconId, photoUri = photoUri))
                showMediaSelectorDialog = false
            },
            mediaLibrary = equipmentMedia,
            categories = allCategories,
            onAddMedia = { uri, category -> onAddMedia(MediaIdentifier.Photo(uri), category) },
            onRemoveMedia = null,
            onUpdateMediaOrder = null,
            onToggleMediaVisibility = onToggleMediaVisibility,
            onSetDefaultInCategory = null,
            isPhotoUsed = null,
            isPrefsMode = false,
            forcedCategory = "EQUIPMENT"
        )
    }

    showFullImageDialog?.let { uri ->
        FullImageDialog(photoUri = uri, onDismiss = { showFullImageDialog = null })
    }

    val imageRequest = ImageRequest.Builder(context)
        .data(equipment.photoUri)
        .crossfade(true)
        .build()

    val equipmentBorderColor = if (isEditing) {
        equipmentCategoryColor?.let {
            try {
                Color(android.graphics.Color.parseColor(it))
            } catch (e: Exception) {
                MaterialTheme.colorScheme.primary
            }
        } ?: MaterialTheme.colorScheme.primary
    } else Color.Transparent

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .graphicsLayer(alpha = cardAlpha),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .border(2.dp, equipmentBorderColor, CircleShape)
                    .clickable {
                        if (isEditing) {
                            showMediaSelectorDialog = true
                        } else {
                            if (equipment.photoUri != null) {
                                showFullImageDialog = equipment.photoUri
                            } else if (equipment.iconIdentifier == null) {
                                showNoPictureDialog = true
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (equipment.photoUri != null) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = stringResource(R.string.equipment_photo),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val icon = EquipmentIconProvider.getIcon(equipment.iconIdentifier)
                    Icon(
                        imageVector = icon,
                        contentDescription = stringResource(R.string.equipment_photo),
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                if (isEditing) {
                    OutlinedTextField(
                        value = editedDescription,
                        onValueChange = { if (it.length <= 50) editedDescription = it },
                        label = { Text(stringResource(R.string.equipment_description)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    Text(
                        text = if (editedDescription.isNotBlank()) editedDescription else "id:${equipment.id} - no description",
                        color = if (editedDescription.isNotBlank()) LocalContentColor.current else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEditing) {
                    IconButton(
                        onClick = { 
                            if (equipment.dismissed) {
                                onRestoreEquipment(equipment)
                            } else {
                                onDismissEquipment(equipment)
                            }
                         }
                    ) {
                        Icon(
                            imageVector = if (equipment.dismissed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (equipment.dismissed) stringResource(R.string.restore_equipment) else stringResource(R.string.dismiss_equipment)
                        )
                    }
                }
                IconButton(
                    onClick = {
                        if (isEditing) {
                            onUpdateEquipment(equipment.copy(description = editedDescription))
                        }
                        isEditing = !isEditing
                    }
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Filled.Done else Icons.Filled.Edit,
                        contentDescription = if (isEditing) stringResource(R.string.save_equipment) else stringResource(R.string.edit_equipment)
                    )
                }
                IconButton(onClick = { /* Drag is handled by the parent */ }) {
                    Icon(
                        imageVector = Icons.Filled.DragHandle,
                        contentDescription = stringResource(R.string.drag_to_reorder)
                    )
                }
            }
        }
    }
}
