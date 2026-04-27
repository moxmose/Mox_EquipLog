package com.moxmose.moxequiplog.ui.operations

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.moxmose.moxequiplog.data.local.*
import com.moxmose.moxequiplog.ui.components.DraggableLazyColumn
import com.moxmose.moxequiplog.ui.components.ImagePickerDialog
import com.moxmose.moxequiplog.ui.equipments.TimeGranularitySelector
import com.moxmose.moxequiplog.ui.maintenancelog.MaintenanceLogDialog
import com.moxmose.moxequiplog.ui.maintenancelog.MaintenanceLogViewModel
import com.moxmose.moxequiplog.ui.options.EquipmentIconProvider
import com.moxmose.moxequiplog.ui.options.OptionsViewModel
import com.moxmose.moxequiplog.utils.UiConstants
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun OperationTypeScreen(
    viewModel: OperationsTypeViewModel = koinViewModel(), 
    optionsViewModel: OptionsViewModel = koinViewModel(),
    logsViewModel: MaintenanceLogViewModel = koinViewModel()
) {
    val activeOperationTypes by viewModel.activeOperationTypes.collectAsState()
    val allOperationTypes by viewModel.allOperationTypes.collectAsState()
    val operationTypeImages by viewModel.operationImages.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val defaultOperationTypeId by viewModel.defaultOperationTypeId.collectAsState()
    val operationStatuses by viewModel.operationStatuses.collectAsState()
    
    val categoryColor by viewModel.categoryColor.collectAsState()
    val categoryDefaultIcon by viewModel.categoryDefaultIcon.collectAsState()
    val categoryDefaultPhoto by viewModel.categoryDefaultPhoto.collectAsState()

    val categoriesUiState by optionsViewModel.categoriesUiState.collectAsState()
    val categoryColorsMap = remember(categoriesUiState) { categoriesUiState.associate { it.category.id to it.color } }
    val categoryDefaultIconsMap = remember(categoriesUiState) { categoriesUiState.associate { it.category.id to it.defaultIconIdentifier } }
    val categoryDefaultPhotosMap = remember(categoriesUiState) { categoriesUiState.associate { it.category.id to it.defaultPhotoUri } }

    val syncCalendarByDefault by logsViewModel.syncCalendarByDefault.collectAsState()
    val googleAccountName by logsViewModel.googleAccountName.collectAsState()
    val measurementUnits by logsViewModel.measurementUnits.collectAsState()
    val activeEquipments by logsViewModel.allEquipments.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(key1 = true) {
        viewModel.uiEvents.collectLatest { event ->
            val message = when(event) {
                is OperationsTypeViewModel.UiEvent.DescriptionInvalid -> context.getString(R.string.description_invalid)
                is OperationsTypeViewModel.UiEvent.AddOperationTypeFailed -> context.getString(R.string.add_operation_type_failed)
                is OperationsTypeViewModel.UiEvent.UpdateOperationTypeFailed -> context.getString(R.string.update_operation_type_failed)
                is OperationsTypeViewModel.UiEvent.UpdateOperationTypesFailed -> context.getString(R.string.update_operation_types_failed)
                is OperationsTypeViewModel.UiEvent.DismissOperationTypeFailed -> context.getString(R.string.dismiss_operation_type_failed)
                is OperationsTypeViewModel.UiEvent.RestoreOperationTypeFailed -> context.getString(R.string.restore_operation_type_failed)
                is OperationsTypeViewModel.UiEvent.AddImageFailed -> context.getString(R.string.add_image_failed)
                is OperationsTypeViewModel.UiEvent.RemoveImageFailed -> context.getString(R.string.remove_image_failed)
                is OperationsTypeViewModel.UiEvent.UpdateImageOrderFailed -> context.getString(R.string.update_image_order_failed)
                is OperationsTypeViewModel.UiEvent.ToggleImageVisibilityFailed -> context.getString(R.string.toggle_image_visibility_failed)
                is OperationsTypeViewModel.UiEvent.DatabaseCheckFailed -> context.getString(R.string.database_check_failed)
                is OperationsTypeViewModel.UiEvent.PhotoUriInvalid -> context.getString(R.string.photo_uri_invalid)
                is OperationsTypeViewModel.UiEvent.SetDefaultFailed -> context.getString(R.string.error_unknown)
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    var showDismissed by rememberSaveable { mutableStateOf(false) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    var selectedAffectedEquipmentForAdd by remember { mutableStateOf<Pair<Int, EquipmentOperationStatus>?>(null) }

    if (selectedAffectedEquipmentForAdd != null) {
        val (opId, status) = selectedAffectedEquipmentForAdd!!
        MaintenanceLogDialog(
            equipments = activeEquipments.filter { !it.dismissed },
            operationTypes = allOperationTypes.filter { !it.dismissed },
            measurementUnits = measurementUnits,
            onDismissRequest = { selectedAffectedEquipmentForAdd = null },
            onConfirm = { log ->
                logsViewModel.addLog(log.equipmentId, log.operationTypeId, log.notes, log.value, log.date, log.color)
                selectedAffectedEquipmentForAdd = null
            },
            defaultEquipmentId = status.equipment.id,
            defaultOperationTypeId = opId,
            initialDate = status.nextPresumedDate ?: System.currentTimeMillis(),
            equipmentCategoryColor = categoryColorsMap[Category.EQUIPMENT],
            operationCategoryColor = categoryColor,
            syncCalendarByDefault = syncCalendarByDefault,
            googleAccountName = googleAccountName
        )
    }

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
        operationCategoryColor = categoryColor,
        snackbarHostState = snackbarHostState,
        defaultOperationTypeId = defaultOperationTypeId,
        onToggleDefault = viewModel::toggleDefaultOperationType,
        categoryColors = categoryColorsMap,
        categoryDefaultIcons = categoryDefaultIconsMap,
        categoryDefaultPhotos = categoryDefaultPhotosMap,
        operationStatuses = operationStatuses,
        onAffectedAction = { opId, status -> selectedAffectedEquipmentForAdd = opId to status }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
    onAddOperationType: (String, ImageIdentifier?, Boolean, Double?, Int?, TimeGranularity?, Int, TimeGranularity) -> Unit,
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
    operationStatuses: Map<Int, OperationGlobalStatus> = emptyMap(),
    onAffectedAction: (Int, EquipmentOperationStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val operationTypesState = remember(operationTypes) { operationTypes.toMutableStateList() }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
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
                onConfirm = { description, identifier, isPredictable, interval, timeout, timeoutUnit, horizon, horizonUnit ->
                    onAddOperationType(description, identifier, isPredictable, interval, timeout, timeoutUnit, horizon, horizonUnit)
                    onShowAddDialogChange(false)
                },
                onAddImage = onAddImage,
                onToggleImageVisibility = onToggleImageVisibility,
                operationCategoryColor = operationCategoryColor
            )
        }

        Column(Modifier.padding(paddingValues).fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = stringResource(R.string.set_as_default_instruction), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(text = stringResource(R.string.hold_and_drag_to_reorder), style = MaterialTheme.typography.bodySmall)
            }
            DraggableLazyColumn(
                items = operationTypesState,
                key = { _, operationType -> operationType.id },
                onMove = { from, to -> operationTypesState.add(to, operationTypesState.removeAt(from)) },
                onDrop = {
                    val reorderedTypes = operationTypesState.mapIndexed { index, type -> type.copy(displayOrder = index) }
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
                        onToggleDefault = { onToggleDefault(operationType.id) },
                        status = operationStatuses[operationType.id],
                        onAffectedAction = { onAffectedAction(operationType.id, it) }
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOperationTypeDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String, ImageIdentifier?, Boolean, Double?, Int?, TimeGranularity?, Int, TimeGranularity) -> Unit,
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
    var isPredictable by rememberSaveable { mutableStateOf(false) }
    var intervalValue by rememberSaveable { mutableStateOf<Double?>(null) }
    var intervalValueStr by rememberSaveable { mutableStateOf("") }
    var timeoutValue by rememberSaveable { mutableStateOf<Int?>(null) }
    var timeoutValueStr by rememberSaveable { mutableStateOf("") }
    var timeoutUnit by rememberSaveable { mutableStateOf(TimeGranularity.MONTHS) }
    var visibilityHorizon by rememberSaveable { mutableIntStateOf(30) }
    var visibilityHorizonUnit by rememberSaveable { mutableStateOf(TimeGranularity.DAYS) }

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
            onRemoveImage = null,
            onUpdateImageOrder = null,
            onToggleImageVisibility = { uri, category -> imageLibrary.find { it.uri == uri && it.category == category }?.let { onToggleImageVisibility(it) } },
            onSetDefaultInCategory = null,
            isPhotoUsed = null,
            isPrefsMode = false,
            forcedCategory = Category.OPERATION
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(R.string.add_a_new_operation_type), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val borderColor = remember(operationCategoryColor) { try { Color(operationCategoryColor.toColorInt()) } catch (_: Exception) { Color.Gray } }
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer).border(2.dp, borderColor, CircleShape).clickable { showImageSelectorDialog = true }, contentAlignment = Alignment.Center) {
                        if (photoUri != null) AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(photoUri).crossfade(true).build(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Icon(imageVector = EquipmentIconProvider.getIcon(iconId, Category.OPERATION), contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    OutlinedTextField(value = description, onValueChange = { if (it.length <= 50) description = it }, label = { Text(stringResource(R.string.operation_type_description)) }, modifier = Modifier.weight(1f), singleLine = true)
                }

                Row(modifier = Modifier.fillMaxWidth().clickable { isPredictable = !isPredictable }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Checkbox(checked = isPredictable, onCheckedChange = { isPredictable = it }); Text(text = "Automatic Maintenance Prediction", style = MaterialTheme.typography.bodyMedium) }

                if (isPredictable) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Default Recurrence Intervals", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(value = intervalValueStr, onValueChange = { input -> val filtered = input.replace(',', '.'); if (filtered.isEmpty() || filtered.toDoubleOrNull() != null) { intervalValueStr = filtered; intervalValue = filtered.toDoubleOrNull() } }, label = { Text("Usage Interval") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                            Text("Recurrence by usage (km, hours, etc. based on equipment)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(value = timeoutValueStr, onValueChange = { input -> if (input.isEmpty()) { timeoutValueStr = ""; timeoutValue = null } else { input.toIntOrNull()?.let { timeoutValueStr = input; timeoutValue = it } } }, label = { Text("Timeout Value") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                TimeGranularitySelector(selected = timeoutUnit, onSelected = { timeoutUnit = it }, label = "Every", modifier = Modifier.weight(1.2f))
                            }
                            Text("Maximum time allowed between maintenances", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(value = visibilityHorizon.toString(), onValueChange = { input -> input.toIntOrNull()?.let { if (it in 1..999) visibilityHorizon = it } }, label = { Text("Event Horizon") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                TimeGranularitySelector(selected = visibilityHorizonUnit, onSelected = { visibilityHorizonUnit = it }, label = "Future span", modifier = Modifier.weight(1.2f))
                            }
                            Text("How far into the future to show upcoming maintenance items", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { val identifier = when { photoUri != null -> ImageIdentifier.Photo(photoUri!!) ; iconId != null -> ImageIdentifier.Icon(iconId!!) ; else -> null } ; onConfirm(description, identifier, isPredictable, intervalValue, timeoutValue, timeoutUnit, visibilityHorizon, visibilityHorizonUnit) }) { Text(stringResource(R.string.button_add)) } },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.button_cancel)) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
    status: OperationGlobalStatus? = null,
    onAffectedAction: (EquipmentOperationStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    
    var editedDescription by remember(operationType.description) { mutableStateOf(operationType.description) }
    var editedIsPredictable by remember(operationType.isPredictable) { mutableStateOf(operationType.isPredictable) }
    var editedIntervalValueStr by remember(operationType.intervalValue) { mutableStateOf(operationType.intervalValue?.toString() ?: "") }
    var editedTimeoutValueStr by remember(operationType.timeoutValue) { mutableStateOf(operationType.timeoutValue?.toString() ?: "") }
    var editedTimeoutUnit by remember(operationType.timeoutUnit) { mutableStateOf(operationType.timeoutUnit ?: TimeGranularity.MONTHS) }
    var editedVisibilityHorizon by remember(operationType.visibilityHorizon) { mutableIntStateOf(operationType.visibilityHorizon) }
    var editedVisibilityHorizonUnit by remember(operationType.visibilityHorizonUnit) { mutableStateOf(operationType.visibilityHorizonUnit) }

    val context = LocalContext.current
    var showFullImageDialog by remember { mutableStateOf<String?>(null) }
    var showNoPictureDialog by remember { mutableStateOf(false) }
    var showImageSelectorDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val operationColor = remember(operationCategoryColor) { try { Color(operationCategoryColor.toColorInt()) } catch (_: Exception) { Color.Gray } }

    Box(contentAlignment = Alignment.BottomEnd) {
        Card(
            modifier = modifier.fillMaxWidth().animateContentSize().graphicsLayer(alpha = if (operationType.dismissed) 0.5f else 1f).then(if (isDefault) Modifier.border(3.dp, operationColor, MaterialTheme.shapes.medium) else Modifier).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { if (!isEditing) isExpanded = !isExpanded }),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer).border(2.dp, operationColor, CircleShape).clickable { if (isEditing) showImageSelectorDialog = true else if (operationType.photoUri != null) showFullImageDialog = operationType.photoUri else if (operationType.iconIdentifier == null) showNoPictureDialog = true }, contentAlignment = Alignment.Center) {
                        if (operationType.photoUri != null) AsyncImage(model = ImageRequest.Builder(context).data(operationType.photoUri).crossfade(true).build(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Icon(imageVector = EquipmentIconProvider.getIcon(operationType.iconIdentifier, Category.OPERATION), contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        if (isEditing) OutlinedTextField(value = editedDescription, onValueChange = { if (it.length <= 50) editedDescription = it }, label = { Text(stringResource(R.string.operation_type_description)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        else Text(text = if (editedDescription.isNotBlank()) editedDescription else stringResource(R.string.id_no_description, operationType.id), color = if (editedDescription.isNotBlank()) LocalContentColor.current else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        if (isEditing) IconButton(onClick = { if (operationType.dismissed) onRestoreOperationType(operationType) else onDismissOperationType(operationType) }) { Icon(imageVector = if (operationType.dismissed) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null) }
                        IconButton(onClick = { if (isEditing) { onUpdateOperationType(operationType.copy(description = editedDescription, isPredictable = editedIsPredictable, intervalValue = editedIntervalValueStr.toDoubleOrNull(), timeoutValue = editedTimeoutValueStr.toIntOrNull(), timeoutUnit = editedTimeoutUnit, visibilityHorizon = editedVisibilityHorizon, visibilityHorizonUnit = editedVisibilityHorizonUnit)) } ; isEditing = !isEditing ; if (isEditing) isExpanded = true }) { Icon(imageVector = if (isEditing) Icons.Filled.Done else Icons.Filled.Edit, contentDescription = null) }
                        IconButton(onClick = { onToggleDefault() }) { Icon(imageVector = if (isDefault) Icons.Filled.Star else Icons.Filled.StarBorder, contentDescription = null, tint = if (isDefault) Color(0xFFFFB300) else LocalContentColor.current) }
                        IconButton(onClick = {}) { Icon(imageVector = Icons.Filled.DragHandle, contentDescription = null) }
                    }
                }

                if (isEditing) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth().clickable { editedIsPredictable = !editedIsPredictable }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Checkbox(checked = editedIsPredictable, onCheckedChange = { editedIsPredictable = it }); Text(text = "Predictive Maintenance", style = MaterialTheme.typography.bodyMedium) }
                    if (editedIsPredictable) {
                        Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Recurrence Intervals", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            OutlinedTextField(value = editedIntervalValueStr, onValueChange = { input -> val filtered = input.replace(',', '.'); if (filtered.isEmpty() || filtered.toDoubleOrNull() != null) editedIntervalValueStr = filtered }, label = { Text("Usage Interval") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), textStyle = MaterialTheme.typography.bodySmall)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(value = editedTimeoutValueStr, onValueChange = { input -> if (input.isEmpty()) editedTimeoutValueStr = "" else input.toIntOrNull()?.let { editedTimeoutValueStr = input } }, label = { Text("Timeout Value") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), textStyle = MaterialTheme.typography.bodySmall)
                                TimeGranularitySelector(selected = editedTimeoutUnit, onSelected = { editedTimeoutUnit = it }, label = "Every", modifier = Modifier.weight(1.2f))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(value = editedVisibilityHorizon.toString(), onValueChange = { input -> input.toIntOrNull()?.let { editedVisibilityHorizon = it } }, label = { Text("Event Horizon") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), textStyle = MaterialTheme.typography.bodySmall)
                                TimeGranularitySelector(selected = editedVisibilityHorizonUnit, onSelected = { editedVisibilityHorizonUnit = it }, label = "Future span", modifier = Modifier.weight(1.2f))
                            }
                        }
                    }
                } else if (isExpanded && status != null && status.affectedEquipments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Upcoming for Equipments", style = MaterialTheme.typography.labelSmall, color = operationColor, fontWeight = FontWeight.Bold)
                    Column(modifier = Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        status.affectedEquipments.sortedBy { it.nextPresumedDate ?: Long.MAX_VALUE }.forEach { eqStatus ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(imageVector = if (eqStatus.isOverdue) Icons.Default.Warning else Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (eqStatus.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = eqStatus.equipment.description, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = eqStatus.nextPresumedDate?.let { dateFormat.format(Date(it)) } ?: "Never", style = MaterialTheme.typography.labelSmall, color = if (eqStatus.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(onClick = { onAffectedAction(eqStatus) }, modifier = Modifier.size(24.dp)) {
                                        Icon(imageVector = Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp), tint = operationColor)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FullImageDialog(photoUri: String, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, confirmButton = { TextButton(onClick = { onDismiss() }) { Text(stringResource(R.string.button_ok)) } }, modifier = Modifier.padding(16.dp), text = { AsyncImage(model = photoUri, contentDescription = stringResource(R.string.full_size_operation_photo), modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.Fit) } )
}
