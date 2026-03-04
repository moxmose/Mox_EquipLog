package com.moxmose.moxequiplog.ui.operations

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.Image
import com.moxmose.moxequiplog.data.local.ImageIdentifier
import com.moxmose.moxequiplog.data.local.OperationType
import com.moxmose.moxequiplog.ui.components.DraggableLazyColumn
import com.moxmose.moxequiplog.ui.components.ImagePickerDialog
import com.moxmose.moxequiplog.ui.options.EquipmentIconProvider
import com.moxmose.moxequiplog.ui.options.OptionsViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun OperationTypeScreen(viewModel: OperationTypeViewModel = koinViewModel(), optionsViewModel: OptionsViewModel = koinViewModel()) {
    val activeOperationTypes by viewModel.activeOperationTypes.collectAsState()
    val allOperationTypes by viewModel.allOperationTypes.collectAsState()
    val operationTypeImages by viewModel.operationTypeImages.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val defaultOperationTypeId by viewModel.defaultOperationTypeId.collectAsState()
    
    val categoryColor by viewModel.getCategoryColor("OPERATION").collectAsState(initial = "#808080")
    val categoryDefaultIcon by viewModel.getCategoryDefaultIcon("OPERATION").collectAsState(initial = null)
    val categoryDefaultPhoto by viewModel.getCategoryDefaultPhoto("OPERATION").collectAsState(initial = null)

    val categoriesUiState by optionsViewModel.categoriesUiState.collectAsState()
    val categoryColorsMap = remember(categoriesUiState) { categoriesUiState.associate { it.category.id to it.color } }
    val categoryDefaultIconsMap = remember(categoriesUiState) { categoriesUiState.associate { it.category.id to it.defaultIconIdentifier } }
    val categoryDefaultPhotosMap = remember(categoriesUiState) { categoriesUiState.associate { it.category.id to it.defaultPhotoUri } }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(key1 = true) {
        viewModel.uiEvents.collectLatest { event ->
            val message = when(event) {
                is OperationTypeViewModel.UiEvent.DescriptionInvalid -> context.getString(R.string.description_invalid)
                is OperationTypeViewModel.UiEvent.AddOperationTypeFailed -> context.getString(R.string.add_operation_type_failed)
                is OperationTypeViewModel.UiEvent.UpdateOperationTypeFailed -> context.getString(R.string.update_operation_type_failed)
                is OperationTypeViewModel.UiEvent.UpdateOperationTypesFailed -> context.getString(R.string.update_operation_types_failed)
                is OperationTypeViewModel.UiEvent.DismissOperationTypeFailed -> context.getString(R.string.dismiss_operation_type_failed)
                is OperationTypeViewModel.UiEvent.RestoreOperationTypeFailed -> context.getString(R.string.restore_operation_type_failed)
                is OperationTypeViewModel.UiEvent.AddImageFailed -> context.getString(R.string.add_image_failed)
                is OperationTypeViewModel.UiEvent.RemoveImageFailed -> context.getString(R.string.remove_image_failed)
                is OperationTypeViewModel.UiEvent.UpdateImageOrderFailed -> context.getString(R.string.update_image_order_failed)
                is OperationTypeViewModel.UiEvent.ToggleImageVisibilityFailed -> context.getString(R.string.toggle_image_visibility_failed)
                is OperationTypeViewModel.UiEvent.DatabaseCheckFailed -> context.getString(R.string.database_check_failed)
                is OperationTypeViewModel.UiEvent.PhotoUriInvalid -> context.getString(R.string.photo_uri_invalid)
                is OperationTypeViewModel.UiEvent.SetDefaultFailed -> context.getString(R.string.error_unknown)
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    var showDismissed by rememberSaveable { mutableStateOf(false) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    val typesToShow = if (showDismissed) allOperationTypes else activeOperationTypes

    OperationTypeScreenContent(
        operationTypes = typesToShow,
        operationTypeImages = operationTypeImages,
        allCategories = allCategories,
        defaultIcon = categoryDefaultIcon,
        defaultPhotoUri = categoryDefaultPhoto,
        onAddOperationType = viewModel::addOperationType,
        onUpdateOperationTypes = viewModel::updateOperationTypes,
        onUpdateOperationType = viewModel::updateOperationType,
        onDismissOperationType = viewModel::dismissOperationType,
        onRestoreOperationType = viewModel::restoreOperationType,
        showDismissed = showDismissed,
        onToggleShowDismissed = { showDismissed = !showDismissed },
        showAddDialog = showAddDialog,
        onShowAddDialogChange = { showAddDialog = it },
        onAddImage = viewModel::addImage,
        onToggleImageVisibility = viewModel::toggleImageVisibility,
        operationCategoryColor = categoryColor ?: "#808080",
        snackbarHostState = snackbarHostState,
        defaultOperationTypeId = defaultOperationTypeId,
        onToggleDefault = viewModel::toggleDefaultOperationType,
        categoryColors = categoryColorsMap,
        categoryDefaultIcons = categoryDefaultIconsMap,
        categoryDefaultPhotos = categoryDefaultPhotosMap
    )
}

@Composable
fun OperationTypeScreenContent(
    operationTypes: List<OperationType>,
    operationTypeImages: List<Image>,
    allCategories: List<Category>,
    defaultIcon: String?,
    defaultPhotoUri: String?,
    showDismissed: Boolean,
    onToggleShowDismissed: () -> Unit,
    showAddDialog: Boolean,
    onShowAddDialogChange: (Boolean) -> Unit,
    onAddOperationType: (String, ImageIdentifier?) -> Unit,
    onUpdateOperationTypes: (List<OperationType>) -> Unit,
    onUpdateOperationType: (OperationType) -> Unit,
    onDismissOperationType: (OperationType) -> Unit,
    onRestoreOperationType: (OperationType) -> Unit,
    onAddImage: (ImageIdentifier, String) -> Unit,
    onToggleImageVisibility: (Image) -> Unit,
    operationCategoryColor: String,
    snackbarHostState: SnackbarHostState,
    defaultOperationTypeId: Int?,
    onToggleDefault: (Int) -> Unit,
    categoryColors: Map<String, String>,
    categoryDefaultIcons: Map<String, String?>,
    categoryDefaultPhotos: Map<String, String?>,
    modifier: Modifier = Modifier
) {
    val operationTypesState = remember(operationTypes) { operationTypes.toMutableStateList() }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                imageLibrary = operationTypeImages,
                categories = allCategories,
                categoryColors = categoryColors,
                categoryDefaultIcons = categoryDefaultIcons,
                categoryDefaultPhotos = categoryDefaultPhotos,
                defaultIcon = defaultIcon,
                defaultPhotoUri = defaultPhotoUri,
                onDismissRequest = { onShowAddDialogChange(false) },
                onConfirm = { description, identifier ->
                    onAddOperationType(description, identifier)
                    onShowAddDialogChange(false)
                },
                onAddImage = onAddImage,
                onToggleImageVisibility = onToggleImageVisibility,
                operationCategoryColor = operationCategoryColor
            )
        }

        Column(
            Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.set_as_default_instruction),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.hold_and_drag_to_reorder),
                    style = MaterialTheme.typography.bodySmall
                )
            }
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
                        operationTypeImages = operationTypeImages,
                        allCategories = allCategories,
                        categoryColors = categoryColors,
                        categoryDefaultIcons = categoryDefaultIcons,
                        categoryDefaultPhotos = categoryDefaultPhotos,
                        onAddImage = onAddImage,
                        onToggleImageVisibility = onToggleImageVisibility,
                        operationCategoryColor = operationCategoryColor,
                        isDefault = operationType.id == defaultOperationTypeId,
                        onToggleDefault = { onToggleDefault(operationType.id) }
                    )
                }
            )
        }
    }
}

@Composable
fun AddOperationTypeDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String, ImageIdentifier?) -> Unit,
    imageLibrary: List<Image>,
    categories: List<Category>,
    categoryColors: Map<String, String>,
    categoryDefaultIcons: Map<String, String?>,
    categoryDefaultPhotos: Map<String, String?>,
    defaultIcon: String?,
    defaultPhotoUri: String?,
    onAddImage: (ImageIdentifier, String) -> Unit,
    onToggleImageVisibility: (Image) -> Unit,
    operationCategoryColor: String
) {
    var description by rememberSaveable { mutableStateOf("") }
    var photoUri by rememberSaveable { mutableStateOf<String?>(null) }
    var iconId by rememberSaveable { mutableStateOf<String?>(null) }
    var isPristine by rememberSaveable { mutableStateOf(true) }
    var showImageSelectorDialog by remember { mutableStateOf(false) }


    if (isPristine && (defaultIcon != null || defaultPhotoUri != null)) {
        LaunchedEffect(defaultIcon, defaultPhotoUri) {
            iconId = defaultIcon
            photoUri = defaultPhotoUri
        }
    }

    if (showImageSelectorDialog) {
        ImagePickerDialog(
            onDismissRequest = { showImageSelectorDialog = false },
            photoUri = photoUri,
            iconIdentifier = iconId,
            onImageSelected = { (newIconId, newPhotoUri) ->
                isPristine = false
                iconId = newIconId
                photoUri = newPhotoUri
                showImageSelectorDialog = false
            },
            imageLibrary = imageLibrary,
            categories = categories,
            categoryColors = categoryColors,
            categoryDefaultIcons = categoryDefaultIcons,
            categoryDefaultPhotos = categoryDefaultPhotos,
            onAddImage = { uri, category -> onAddImage(ImageIdentifier.Photo(uri), category) },
            onRemoveImage = { uri, category -> imageLibrary.find { it.uri == uri && it.category == category }?.let { onToggleImageVisibility(it) } },
            onUpdateImageOrder = null,
            onToggleImageVisibility = { uri, category -> imageLibrary.find { it.uri == uri && it.category == category }?.let { onToggleImageVisibility(it) } },
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
                val borderColor = try {
                    Color(operationCategoryColor.toColorInt())
                } catch (_: Exception) {
                    MaterialTheme.colorScheme.primary
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .border(2.dp, borderColor, CircleShape)
                        .clickable { showImageSelectorDialog = true },
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
                        photoUri != null -> ImageIdentifier.Photo(photoUri!!)
                        iconId != null -> ImageIdentifier.Icon(iconId!!)
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
    operationTypeImages: List<Image>,
    allCategories: List<Category>,
    categoryColors: Map<String, String>,
    categoryDefaultIcons: Map<String, String?>,
    categoryDefaultPhotos: Map<String, String?>,
    onAddImage: (ImageIdentifier, String) -> Unit,
    onToggleImageVisibility: (Image) -> Unit,
    operationCategoryColor: String,
    isDefault: Boolean,
    onToggleDefault: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedDescription by remember(operationType.description) { mutableStateOf(operationType.description) }
    val context = LocalContext.current
    var showFullImageDialog by remember { mutableStateOf<String?>(null) }
    var showNoPictureDialog by remember { mutableStateOf(false) }
    var showImageSelectorDialog by remember { mutableStateOf(false) }

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

    if (showImageSelectorDialog) {
        ImagePickerDialog(
            onDismissRequest = { showImageSelectorDialog = false },
            photoUri = operationType.photoUri,
            iconIdentifier = operationType.iconIdentifier,
            onImageSelected = { (iconId, photoUri) ->
                onUpdateOperationType(operationType.copy(iconIdentifier = iconId, photoUri = photoUri))
                showImageSelectorDialog = false
            },
            imageLibrary = operationTypeImages,
            categories = allCategories,
            categoryColors = categoryColors,
            categoryDefaultIcons = categoryDefaultIcons,
            categoryDefaultPhotos = categoryDefaultPhotos,
            onAddImage = { uri, category -> onAddImage(ImageIdentifier.Photo(uri), category) },
            onRemoveImage = { uri, category -> operationTypeImages.find { it.uri == uri && it.category == category }?.let { onToggleImageVisibility(it) } },
            onUpdateImageOrder = null,
            onToggleImageVisibility = { uri, category -> operationTypeImages.find { it.uri == uri && it.category == category }?.let { onToggleImageVisibility(it) } },
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

    val operationColor = try {
        Color(operationCategoryColor.toColorInt())
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Box(contentAlignment = Alignment.BottomEnd) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize()
                .graphicsLayer(alpha = cardAlpha)
                .then(
                    if (isDefault) Modifier.border(3.dp, operationColor, MaterialTheme.shapes.medium)
                    else Modifier
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (!isEditing) onToggleDefault()
                    }
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (isDefault) operationColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
            )
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
                        .border(2.dp, operationColor, CircleShape)
                        .clickable {
                            if (isEditing) {
                                showImageSelectorDialog = true
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
        if (isDefault) {
            Box(
                modifier = Modifier
                    .padding(end = 4.dp, bottom = 4.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(operationColor)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
            }
        }
    }
}
