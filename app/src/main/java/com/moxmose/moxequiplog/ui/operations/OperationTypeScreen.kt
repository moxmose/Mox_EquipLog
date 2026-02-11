package com.moxmose.moxequiplog.ui.operations

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.core.graphics.toColorInt
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.Media
import com.moxmose.moxequiplog.data.local.MediaIdentifier
import com.moxmose.moxequiplog.data.local.OperationType
import com.moxmose.moxequiplog.ui.components.DraggableLazyColumn
import com.moxmose.moxequiplog.ui.components.MediaPickerDialog
import com.moxmose.moxequiplog.ui.options.EquipmentIconProvider
import org.koin.androidx.compose.koinViewModel

@Composable
fun OperationTypeScreen(viewModel: OperationTypeViewModel = koinViewModel()) {
    val activeOperationTypes by viewModel.activeOperationTypes.collectAsState()
    val allOperationTypes by viewModel.allOperationTypes.collectAsState()
    val operationTypeMedia by viewModel.operationTypeMedia.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()

    var showDismissed by rememberSaveable { mutableStateOf(false) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    val typesToShow = if (showDismissed) allOperationTypes else activeOperationTypes
    val operationCategory = allCategories.find { it.id == "OPERATION" }

    OperationTypeScreenContent(
        operationTypes = typesToShow,
        operationTypeMedia = operationTypeMedia,
        allCategories = allCategories,
        defaultIcon = operationCategory?.defaultIconIdentifier,
        defaultPhotoUri = operationCategory?.defaultPhotoUri,
        onAddOperationType = viewModel::addOperationType,
        onUpdateOperationTypes = viewModel::updateOperationTypes,
        onUpdateOperationType = viewModel::updateOperationType,
        onDismissOperationType = viewModel::dismissOperationType,
        onRestoreOperationType = viewModel::restoreOperationType,
        showDismissed = showDismissed,
        onToggleShowDismissed = { showDismissed = !showDismissed },
        showAddDialog = showAddDialog,
        onShowAddDialogChange = { showAddDialog = it },
        onAddMedia = viewModel::addMedia,
        onToggleMediaVisibility = viewModel::toggleMediaVisibility,
        operationCategoryColor = operationCategory?.color
    )
}

@Composable
fun OperationTypeScreenContent(
    operationTypes: List<OperationType>,
    operationTypeMedia: List<Media>,
    allCategories: List<Category>,
    defaultIcon: String?,
    defaultPhotoUri: String?,
    showDismissed: Boolean,
    onToggleShowDismissed: () -> Unit,
    showAddDialog: Boolean,
    onShowAddDialogChange: (Boolean) -> Unit,
    onAddOperationType: (String, MediaIdentifier?) -> Unit,
    onUpdateOperationTypes: (List<OperationType>) -> Unit,
    onUpdateOperationType: (OperationType) -> Unit,
    onDismissOperationType: (OperationType) -> Unit,
    onRestoreOperationType: (OperationType) -> Unit,
    onAddMedia: (MediaIdentifier, String) -> Unit,
    onToggleMediaVisibility: (Media) -> Unit,
    operationCategoryColor: String?,
    modifier: Modifier = Modifier
) {
    val operationTypesState = remember(operationTypes) { operationTypes.toMutableStateList() }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(onClick = { onShowAddDialogChange(true) }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_operation_type))
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
            AddOperationTypeDialog(
                mediaLibrary = operationTypeMedia,
                categories = allCategories,
                defaultIcon = defaultIcon,
                defaultPhotoUri = defaultPhotoUri,
                onDismissRequest = { onShowAddDialogChange(false) },
                onConfirm = { description, identifier ->
                    onAddOperationType(description, identifier)
                    onShowAddDialogChange(false)
                },
                onAddMedia = onAddMedia,
                onToggleMediaVisibility = onToggleMediaVisibility,
                operationCategoryColor = operationCategoryColor
            )
        }

        Column(
            Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Text(
                text = stringResource(R.string.hold_and_drag_to_reorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall
            )
            DraggableLazyColumn(
                items = operationTypesState,
                key = { _, operationType -> operationType.id },
                onMove = { from, to ->
                    operationTypesState.add(to, operationTypesState.removeAt(from))
                },
                onDrop = {
                    val reorderedTypes = operationTypesState.mapIndexed { index, type ->
                        type.copy(displayOrder = index)
                    }
                    onUpdateOperationTypes(reorderedTypes)
                },
                modifier = Modifier.fillMaxSize(),
                itemContent = { _, operationType ->
                    OperationTypeCard(
                        operationType = operationType,
                        onUpdateOperationType = onUpdateOperationType,
                        onDismissOperationType = onDismissOperationType,
                        onRestoreOperationType = onRestoreOperationType,
                        operationTypeMedia = operationTypeMedia,
                        allCategories = allCategories,
                        onAddMedia = onAddMedia,
                        onToggleMediaVisibility = onToggleMediaVisibility,
                        operationCategoryColor = operationCategoryColor
                    )
                }
            )
        }
    }
}

@Composable
fun AddOperationTypeDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String, MediaIdentifier?) -> Unit,
    mediaLibrary: List<Media>,
    categories: List<Category>,
    defaultIcon: String?,
    defaultPhotoUri: String?,
    onAddMedia: (MediaIdentifier, String) -> Unit,
    onToggleMediaVisibility: (Media) -> Unit,
    operationCategoryColor: String?
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
            onToggleMediaVisibility = { uri, category -> mediaLibrary.find { it.uri == uri && it.category == category }?.let { onToggleMediaVisibility(it) } },
            onSetDefaultInCategory = null,
            isPhotoUsed = null,
            isPrefsMode = false,
            forcedCategory = "OPERATION"
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = stringResource(R.string.add_a_new_operation_type),
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
                val borderColor = operationCategoryColor?.let {
                    try {
                        Color(it.toColorInt())
                    } catch (_: Exception) {
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
                            contentDescription = stringResource(R.string.operation_type_photo),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        val icon = EquipmentIconProvider.getIcon(iconId, "OPERATION")
                        Icon(
                            imageVector = icon,
                            contentDescription = stringResource(R.string.operation_type_photo),
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 50) description = it },
                    label = { Text(stringResource(R.string.operation_type_description)) },
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
                contentDescription = stringResource(R.string.full_size_operation_photo),
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit
            )
        }
    )
}

@Composable
fun OperationTypeCard(
    operationType: OperationType,
    onUpdateOperationType: (OperationType) -> Unit,
    onDismissOperationType: (OperationType) -> Unit,
    onRestoreOperationType: (OperationType) -> Unit,
    operationTypeMedia: List<Media>,
    allCategories: List<Category>,
    onAddMedia: (MediaIdentifier, String) -> Unit,
    onToggleMediaVisibility: (Media) -> Unit,
    operationCategoryColor: String?,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedDescription by remember(operationType.description) { mutableStateOf(operationType.description) }
    val context = LocalContext.current
    var showFullImageDialog by remember { mutableStateOf<String?>(null) }
    var showNoPictureDialog by remember { mutableStateOf(false) }
    var showMediaSelectorDialog by remember { mutableStateOf(false) }

    val cardAlpha = if (operationType.dismissed) 0.5f else 1f

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
            photoUri = operationType.photoUri,
            iconIdentifier = operationType.iconIdentifier,
            onMediaSelected = { (iconId, photoUri) ->
                onUpdateOperationType(operationType.copy(iconIdentifier = iconId, photoUri = photoUri))
                showMediaSelectorDialog = false
            },
            mediaLibrary = operationTypeMedia,
            categories = allCategories,
            onAddMedia = { uri, category -> onAddMedia(MediaIdentifier.Photo(uri), category) },
            onRemoveMedia = null,
            onUpdateMediaOrder = null,
            onToggleMediaVisibility = { uri, category -> operationTypeMedia.find { it.uri == uri && it.category == category }?.let { onToggleMediaVisibility(it) } },
            onSetDefaultInCategory = null,
            isPhotoUsed = null,
            isPrefsMode = false,
            forcedCategory = "OPERATION"
        )
    }

    showFullImageDialog?.let { uri ->
        FullImageDialog(photoUri = uri, onDismiss = { showFullImageDialog = null })
    }

    val imageRequest = ImageRequest.Builder(context)
        .data(operationType.photoUri)
        .crossfade(true)
        .build()

    val operationBorderColor = if (isEditing) {
        operationCategoryColor?.let {
            try {
                Color(it.toColorInt())
            } catch (_: Exception) {
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
                    .border(2.dp, operationBorderColor, CircleShape)
                    .clickable {
                        if (isEditing) {
                            showMediaSelectorDialog = true
                        } else {
                            if (operationType.photoUri != null) {
                                showFullImageDialog = operationType.photoUri
                            } else if (operationType.iconIdentifier == null) {
                                showNoPictureDialog = true
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (operationType.photoUri != null) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = stringResource(R.string.operation_type_photo),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val icon = EquipmentIconProvider.getIcon(operationType.iconIdentifier, "OPERATION")
                    Icon(
                        imageVector = icon,
                        contentDescription = stringResource(R.string.operation_type_photo),
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
                        label = { Text(stringResource(R.string.operation_type_description)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    Text(
                        text = editedDescription.ifBlank { "id:${operationType.id} - no description" },
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
                            if (operationType.dismissed) {
                                onRestoreOperationType(operationType)
                            } else {
                                onDismissOperationType(operationType)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (operationType.dismissed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (operationType.dismissed) stringResource(R.string.restore_operation_type) else stringResource(R.string.dismiss_operation_type)
                        )
                    }
                }
                IconButton(
                    onClick = {
                        if (isEditing) {
                            onUpdateOperationType(operationType.copy(description = editedDescription))
                        }
                        isEditing = !isEditing
                    }
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Filled.Done else Icons.Filled.Edit,
                        contentDescription = if (isEditing) stringResource(R.string.save_operation_type) else stringResource(R.string.edit_operation_type)
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
