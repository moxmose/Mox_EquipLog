package com.moxmose.moxequiplog.ui.options

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.moxmose.moxequiplog.BuildConfig
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.*
import com.moxmose.moxequiplog.ui.components.*
import com.moxmose.moxequiplog.utils.AppConstants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun OptionsScreen(modifier: Modifier = Modifier, viewModel: OptionsViewModel = koinViewModel()) {
    val username by viewModel.username.collectAsState()
    val allImages by viewModel.allImages.collectAsState()
    val categoriesUiState by viewModel.categoriesUiState.collectAsState()
    val allColors by viewModel.allColors.collectAsState()
    val measurementUnits by viewModel.measurementUnits.collectAsState()
    val defaultUnitId by viewModel.defaultUnitId.collectAsState()
    
    val backgroundUri by viewModel.backgroundUri.collectAsState()
    val backgroundBlur by viewModel.backgroundBlur.collectAsState()
    val backgroundSaturation by viewModel.backgroundSaturation.collectAsState()
    val backgroundTintEnabled by viewModel.backgroundTintEnabled.collectAsState()
    val backgroundTintAlpha by viewModel.backgroundTintAlpha.collectAsState()
    val backgroundImageAlpha by viewModel.backgroundImageAlpha.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(key1 = true) {
        viewModel.uiEvents.collectLatest { event ->
            val message = when (event) {
                is OptionsViewModel.OptionsUiEvent.DatabaseCheckFailed -> context.getString(R.string.database_check_failed)
                is OptionsViewModel.OptionsUiEvent.UsernameInvalid -> context.getString(R.string.username_invalid)
                is OptionsViewModel.OptionsUiEvent.UpdateUsernameFailed -> context.getString(R.string.update_username_failed)
                is OptionsViewModel.OptionsUiEvent.RemoveImageFailed -> context.getString(R.string.remove_image_failed)
                is OptionsViewModel.OptionsUiEvent.UpdateColorFailed -> context.getString(R.string.update_color_failed)
                is OptionsViewModel.OptionsUiEvent.ColorNameInvalid -> context.getString(R.string.color_name_invalid)
                is OptionsViewModel.OptionsUiEvent.PhotoUriInvalid -> context.getString(R.string.photo_uri_invalid)
                is OptionsViewModel.OptionsUiEvent.SetCategoryDefaultFailed -> context.getString(R.string.set_category_default_failed)
                is OptionsViewModel.OptionsUiEvent.CategoryIdInvalid -> context.getString(R.string.category_id_invalid)
                is OptionsViewModel.OptionsUiEvent.NoImageSelectedForDefault -> context.getString(R.string.no_image_selected)
                is OptionsViewModel.OptionsUiEvent.ToggleImageVisibilityFailed -> context.getString(R.string.toggle_image_visibility_failed)
                is OptionsViewModel.OptionsUiEvent.AddImageFailed -> context.getString(R.string.add_image_failed)
                is OptionsViewModel.OptionsUiEvent.UpdateImageOrderFailed -> context.getString(R.string.update_image_order_failed)
                is OptionsViewModel.OptionsUiEvent.UpdateCategoryColorFailed -> context.getString(R.string.update_category_color_failed)
                is OptionsViewModel.OptionsUiEvent.ColorHexInvalid -> context.getString(R.string.color_hex_invalid)
                is OptionsViewModel.OptionsUiEvent.AddColorFailed -> context.getString(R.string.add_color_failed, event.name)
                is OptionsViewModel.OptionsUiEvent.UpdateColorsOrderFailed -> context.getString(R.string.update_colors_order_failed)
                is OptionsViewModel.OptionsUiEvent.ToggleColorVisibilityFailed -> context.getString(R.string.toggle_color_visibility_failed)
                is OptionsViewModel.OptionsUiEvent.ColorIdInvalid -> context.getString(R.string.color_id_invalid)
                is OptionsViewModel.OptionsUiEvent.DeleteColorFailed -> context.getString(R.string.delete_color_failed)
                is OptionsViewModel.OptionsUiEvent.UpdateBackgroundFailed -> context.getString(R.string.update_background_failed)
                is OptionsViewModel.OptionsUiEvent.AddUnitFailed -> context.getString(R.string.add_unit_failed)
                is OptionsViewModel.OptionsUiEvent.DeleteUnitFailed -> context.getString(R.string.delete_unit_failed)
                is OptionsViewModel.OptionsUiEvent.UpdateUnitFailed -> context.getString(R.string.update_unit_failed)
                is OptionsViewModel.OptionsUiEvent.UpdateUnitsOrderFailed -> context.getString(R.string.update_units_order_failed)
                is OptionsViewModel.OptionsUiEvent.ToggleUnitVisibilityFailed -> context.getString(R.string.toggle_unit_visibility_failed)
                is OptionsViewModel.OptionsUiEvent.SetDefaultFailed -> context.getString(R.string.set_default_failed)
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    var showAboutDialog by rememberSaveable { mutableStateOf(false) }
    var showColorPicker by rememberSaveable { mutableStateOf<String?>(null) }
    var showImageDialog by rememberSaveable { mutableStateOf(false) }

    OptionsScreenContent(
        modifier = modifier,
        username = username,
        allImages = allImages,
        categoriesUiState = categoriesUiState,
        allColors = allColors,
        measurementUnits = measurementUnits,
        defaultUnitId = defaultUnitId,
        backgroundUri = backgroundUri,
        backgroundBlur = backgroundBlur,
        backgroundSaturation = backgroundSaturation,
        backgroundTintEnabled = backgroundTintEnabled,
        backgroundTintAlpha = backgroundTintAlpha,
        backgroundImageAlpha = backgroundImageAlpha,
        onUsernameChange = viewModel::setUsername,
        onSetCategoryDefault = viewModel::setCategoryDefault,
        onAddImage = viewModel::addImage,
        onRemoveImage = viewModel::removeImage,
        onUpdateImageOrder = viewModel::updateImageOrder,
        onToggleImageVisibility = viewModel::toggleImageVisibility,
        onUpdateCategoryColor = viewModel::updateCategoryColor,
        onSetBackgroundUri = viewModel::setBackgroundUri,
        onSetBackgroundBlur = viewModel::setBackgroundBlur,
        onSetBackgroundSaturation = viewModel::setBackgroundSaturation,
        onSetBackgroundTintEnabled = viewModel::setBackgroundTintEnabled,
        onSetBackgroundTintAlpha = viewModel::setBackgroundTintAlpha,
        onSetBackgroundImageAlpha = viewModel::setBackgroundImageAlpha,
        onResetBackgroundSettings = viewModel::resetBackgroundSettings,
        onAddUnit = viewModel::addMeasurementUnit,
        onUpdateUnit = viewModel::updateMeasurementUnit,
        onToggleUnitVisibility = viewModel::toggleMeasurementUnitVisibility,
        onUpdateUnitsOrder = viewModel::updateMeasurementUnitsOrder,
        onDeleteUnit = viewModel::deleteMeasurementUnit,
        onToggleDefaultUnit = viewModel::toggleDefaultUnit,
        isPhotoUsed = viewModel::isPhotoUsed,
        showAboutDialog = showAboutDialog,
        onShowAboutDialogChange = { showAboutDialog = it },
        showColorPicker = showColorPicker,
        onShowColorPickerChange = { showColorPicker = it },
        showImageDialog = showImageDialog,
        onShowImageDialogChange = { showImageDialog = it },
        onAddColor = viewModel::addColor,
        onUpdateColor = viewModel::updateColor,
        onUpdateColorsOrder = viewModel::updateColorsOrder,
        onToggleColorVisibility = viewModel::toggleColorVisibility,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class)
@Composable
fun OptionsScreenContent(
    modifier: Modifier = Modifier,
    username: String,
    allImages: List<Image>,
    categoriesUiState: List<CategoryUiState>,
    allColors: List<AppColor>,
    measurementUnits: List<MeasurementUnit>,
    defaultUnitId: Int?,
    backgroundUri: String?,
    backgroundBlur: Float,
    backgroundSaturation: Float,
    backgroundTintEnabled: Boolean,
    backgroundTintAlpha: Float,
    backgroundImageAlpha: Float,
    onUsernameChange: (String) -> Unit,
    onSetCategoryDefault: (String, ImageIdentifier?) -> Unit,
    onAddImage: (ImageIdentifier, String) -> Unit,
    onRemoveImage: (Image) -> Unit,
    onUpdateImageOrder: (List<Image>) -> Unit,
    onToggleImageVisibility: (Image) -> Unit,
    onUpdateCategoryColor: (String, String) -> Unit,
    onSetBackgroundUri: (String?) -> Unit,
    onSetBackgroundBlur: (Float) -> Unit,
    onSetBackgroundSaturation: (Float) -> Unit,
    onSetBackgroundTintEnabled: (Boolean) -> Unit,
    onSetBackgroundTintAlpha: (Float) -> Unit,
    onSetBackgroundImageAlpha: (Float) -> Unit,
    onResetBackgroundSettings: () -> Unit,
    onAddUnit: (String, String) -> Unit,
    onUpdateUnit: (MeasurementUnit) -> Unit,
    onToggleUnitVisibility: (Int) -> Unit,
    onUpdateUnitsOrder: (List<MeasurementUnit>) -> Unit,
    onDeleteUnit: (MeasurementUnit) -> Unit,
    onToggleDefaultUnit: (Int) -> Unit,
    isPhotoUsed: suspend (String) -> Boolean,
    showAboutDialog: Boolean,
    onShowAboutDialogChange: (Boolean) -> Unit,
    showColorPicker: String?,
    onShowColorPickerChange: (String?) -> Unit,
    showImageDialog: Boolean,
    onShowImageDialogChange: (Boolean) -> Unit,
    onAddColor: (String, String) -> Unit,
    onUpdateColor: (AppColor) -> Unit,
    onUpdateColorsOrder: (List<AppColor>) -> Unit,
    onToggleColorVisibility: (Long) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var editedUsername by rememberSaveable(username) { mutableStateOf(username) }
    var showBackgroundPicker by remember { mutableStateOf(false) }
    var showUnitManagement by remember { mutableStateOf(false) }
    
    val categoryColorsMap = remember(categoriesUiState) { categoriesUiState.associate { it.category.id to it.color } }
    val categoryDefaultIconsMap = remember(categoriesUiState) { categoriesUiState.associate { it.category.id to it.defaultIconIdentifier } }
    val categoryDefaultPhotosMap = remember(categoriesUiState) { categoriesUiState.associate { it.category.id to it.defaultPhotoUri } }

    if (showAboutDialog) {
        BasicAlertDialog(onDismissRequest = { onShowAboutDialogChange(false) }) {
            Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(text = stringResource(R.string.about_dialog_title), style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = stringResource(R.string.about_dialog_content, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(24.dp))
                    TextButton(onClick = { onShowAboutDialogChange(false) }, modifier = Modifier.align(Alignment.End)) {
                        Text(stringResource(R.string.button_ok))
                    }
                }
            }
        }
    }

    if (showUnitManagement) {
        UnitManagementDialog(
            allUnits = measurementUnits,
            defaultUnitId = defaultUnitId,
            onDismiss = { showUnitManagement = false },
            onAddUnit = onAddUnit,
            onUpdateUnit = onUpdateUnit,
            onUpdateUnitsOrder = onUpdateUnitsOrder,
            onToggleUnitVisibility = onToggleUnitVisibility,
            onDeleteUnit = onDeleteUnit,
            onToggleDefaultUnit = onToggleDefaultUnit
        )
    }

    if (showImageDialog) {
        val allCategories = categoriesUiState.map { it.category }.filter { it.id != Category.LOGS }
        ImageSelector(
            photoUri = null,
            iconIdentifier = null,
            imageLibrary = allImages,
            categories = allCategories,
            categoryColors = categoryColorsMap,
            categoryDefaultIcons = categoryDefaultIconsMap,
            categoryDefaultPhotos = categoryDefaultPhotosMap,
            onImageSelected = { _, _ -> },
            onAddImage = { uri, category -> onAddImage(ImageIdentifier.Photo(uri), category) },
            onRemoveImage = { uri, category -> allImages.find { it.uri == uri && it.category == category }?.let { onRemoveImage(it) } },
            onUpdateImageOrder = onUpdateImageOrder,
            onToggleImageVisibility = { uri, category -> allImages.find { it.uri == uri && it.category == category }?.let { onToggleImageVisibility(it) } },
            onSetDefaultInCategory = { categoryId, iconId, photoUri ->
                val identifier = when {
                    iconId != null -> ImageIdentifier.Icon(iconId)
                    photoUri != null -> ImageIdentifier.Photo(photoUri)
                    else -> null
                }
                onSetCategoryDefault(categoryId, identifier)
            },
            isPhotoUsed = isPhotoUsed,
            isPrefsMode = true,
            onDismissRequest = { onShowImageDialogChange(false) }
        )
    }

    if (showBackgroundPicker) {
        val allCategories = categoriesUiState.map { it.category }.filter { it.id != Category.LOGS }
        ImagePickerDialog(
            onDismissRequest = { showBackgroundPicker = false },
            photoUri = backgroundUri,
            iconIdentifier = null,
            onImageSelected = { (_, photoUri) ->
                if (photoUri != null) onSetBackgroundUri(photoUri)
                showBackgroundPicker = false
            },
            imageLibrary = allImages,
            categories = allCategories,
            categoryColors = categoryColorsMap,
            categoryDefaultIcons = categoryDefaultIconsMap,
            categoryDefaultPhotos = categoryDefaultPhotosMap,
            onAddImage = { uri, category -> onAddImage(ImageIdentifier.Photo(uri), category) },
            onRemoveImage = null,
            onUpdateImageOrder = null,
            onToggleImageVisibility = { uri, category -> allImages.find { it.uri == uri && it.category == category }?.let { onToggleImageVisibility(it) } },
            onSetDefaultInCategory = null,
            isPhotoUsed = null,
            isPrefsMode = false,
            forcedCategory = null,
            onlyPhotos = true
        )
    }

    showColorPicker?.let { categoryId ->
        val categoryUiState = categoriesUiState.find { it.category.id == categoryId }!!
        ColorManagementDialog(
            allColors = allColors,
            categoryUiState = categoryUiState,
            onDismiss = { onShowColorPickerChange(null) },
            onColorSelected = { onUpdateCategoryColor(categoryId, it) },
            onAddColor = onAddColor,
            onUpdateColor = onUpdateColor,
            onUpdateColorsOrder = onUpdateColorsOrder,
            onToggleColorVisibility = onToggleColorVisibility
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(it)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.options_version_label, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            // 1. PROFILO
            OptionsSectionCard(title = stringResource(R.string.options_profile_section)) {
                OutlinedTextField(
                    value = editedUsername,
                    onValueChange = { if (it.length <= AppConstants.USERNAME_MAX_LENGTH) editedUsername = it },
                    label = { Text(stringResource(R.string.options_username_field_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (editedUsername != username && editedUsername.isNotBlank()) {
                            IconButton(onClick = { onUsernameChange(editedUsername) }) {
                                Icon(Icons.Default.Done, contentDescription = stringResource(R.string.options_save_username))
                            }
                        }
                    }
                )
            }

            // 2. IMMAGINI
            OptionsSectionCard(
                title = stringResource(R.string.options_image_mgmt_title),
                description = stringResource(R.string.options_image_mgmt_desc)
            ) {
                OutlinedButton(onClick = { onShowImageDialogChange(true) }, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (allImages.isEmpty()) {
                                Text(stringResource(R.string.options_empty_library))
                            } else {
                                allImages.filter { !it.hidden }.distinctBy { it.uri }.take(4).forEach { image ->
                                    val allCategories = categoriesUiState.map { it.category }
                                    ImageSelector(
                                        photoUri = if (image.imageType == "IMAGE") image.uri else null,
                                        iconIdentifier = if (image.imageType == "ICON") image.uri.removePrefix("icon:") else null,
                                        onImageSelected = {_,_ ->},
                                        modifier = Modifier.size(40.dp),
                                        category = image.category,
                                        categories = allCategories,
                                        categoryColors = categoryColorsMap,
                                        categoryDefaultIcons = categoryDefaultIconsMap,
                                        categoryDefaultPhotos = categoryDefaultPhotosMap,
                                        imageLibrary = allImages,
                                        forcedCategory = Category.EQUIPMENT
                                    )
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.options_manage_label), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Icon(imageVector = Icons.Default.Edit, contentDescription = stringResource(R.string.options_manage_images), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // 3. COLORI
            OptionsSectionCard(
                title = stringResource(R.string.options_sections_colors_title),
                description = stringResource(R.string.options_sections_colors_desc)
            ) {
                val sectionOrder = listOf(Category.LOGS, Category.EQUIPMENT, Category.OPERATION)
                categoriesUiState
                    .sortedBy { uiState -> 
                        val index = sectionOrder.indexOf(uiState.category.id)
                        if (index != -1) index else Int.MAX_VALUE
                    }
                    .forEach { uiState ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text(uiState.category.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(uiState.color.toColorInt()))
                                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    .clickable { onShowColorPickerChange(uiState.category.id) }
                            )
                        }
                    }
            }

            // 4. UNITÀ DI MISURA
            OptionsSectionCard(
                title = stringResource(R.string.options_units_title),
                description = stringResource(R.string.options_units_desc)
            ) {
                OutlinedButton(onClick = { showUnitManagement = true }, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            val visibleUnits = measurementUnits.filter { !it.isHidden }.take(5)
                            if (visibleUnits.isEmpty()) {
                                Text(stringResource(R.string.options_empty_library))
                            } else {
                                visibleUnits.forEach { unit ->
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = if (unit.id == defaultUnitId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    ) {
                                        Text(
                                            text = unit.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontWeight = if (unit.id == defaultUnitId) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.options_manage_label), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Icon(imageVector = Icons.Default.Edit, contentDescription = stringResource(R.string.options_manage_label), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // 5. BACKGROUND
            OptionsSectionCard(
                title = stringResource(R.string.options_background_custom_title),
                description = stringResource(R.string.options_background_custom_desc)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.options_enable_section_color), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            Switch(checked = backgroundTintEnabled, onCheckedChange = onSetBackgroundTintEnabled)
                        }
                        if (backgroundTintEnabled) {
                            Text(stringResource(R.string.options_color_intensity, (backgroundTintAlpha * 100).toInt()), style = MaterialTheme.typography.labelMedium)
                            Slider(value = backgroundTintAlpha, onValueChange = onSetBackgroundTintAlpha, valueRange = 0f..1f)
                        }
                    }
                    HorizontalDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { showBackgroundPicker = true }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Image, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.options_select_image))
                            }
                            if (backgroundUri != null) {
                                IconButton(onClick = { onSetBackgroundUri(null) }) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.options_remove_background), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        if (backgroundUri != null) {
                            Text(stringResource(R.string.options_image_opacity, (backgroundImageAlpha * 100).toInt()), style = MaterialTheme.typography.labelMedium)
                            Slider(value = backgroundImageAlpha, onValueChange = onSetBackgroundImageAlpha, valueRange = 0f..1f)
                            Text(stringResource(R.string.options_blur_radius_label, backgroundBlur.toInt()), style = MaterialTheme.typography.labelMedium)
                            Slider(value = backgroundBlur, onValueChange = onSetBackgroundBlur, valueRange = 0f..25f, steps = 25)
                            Text(stringResource(R.string.options_saturation_label, (backgroundSaturation * 100).toInt()), style = MaterialTheme.typography.labelMedium)
                            Slider(value = backgroundSaturation, onValueChange = onSetBackgroundSaturation, valueRange = 0f..2f)
                        }
                    }
                    HorizontalDivider()
                    TextButton(onClick = onResetBackgroundSettings, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.options_reset_background_settings))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onShowAboutDialogChange(true) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.button_about))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorManagementDialog(
    allColors: List<AppColor>,
    categoryUiState: CategoryUiState,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit,
    onAddColor: (String, String) -> Unit,
    onUpdateColor: (AppColor) -> Unit,
    onUpdateColorsOrder: (List<AppColor>) -> Unit,
    onToggleColorVisibility: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddColorDialog by remember { mutableStateOf(false) }
    var showHidden by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val colorsState = remember(allColors, showHidden) { allColors.filter { !it.hidden || showHidden }.toMutableStateList() }
    if (showAddColorDialog) { AddColorDialog(onDismiss = { showAddColorDialog = false }, onAddColor = { hex, name -> onAddColor(hex, name) }) }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(modifier = Modifier.padding(16.dp), shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
            Scaffold(
                modifier = Modifier.height(500.dp),
                floatingActionButton = {
                    Column(horizontalAlignment = Alignment.End) {
                        FloatingActionButton(onClick = { showAddColorDialog = true }) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.options_add_color)) }
                        Spacer(Modifier.height(8.dp))
                        FloatingActionButton(onClick = { showHidden = !showHidden; scope.launch { lazyListState.animateScrollToItem(0) } }, containerColor = MaterialTheme.colorScheme.secondary) { Icon(if (showHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = stringResource(R.string.options_show_hide)) }
                    }
                }
            ) { paddingValues ->
                Column(Modifier.padding(paddingValues).padding(16.dp)) {
                    Text(text = stringResource(R.string.options_color_mgmt_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), textAlign = TextAlign.Center)
                    
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.options_info_drag_reorder), style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    DraggableLazyColumn(
                        items = colorsState,
                        key = { _, color -> color.id },
                        onMove = { from, to -> colorsState.add(to, colorsState.removeAt(from)) },
                        onDrop = { val hiddenColors = allColors.filter { it.hidden && !showHidden }; val fullNewList = colorsState + hiddenColors; onUpdateColorsOrder(fullNewList.mapIndexed { index, appColor -> appColor.copy(displayOrder = index) }) },
                        itemContent = { _, color -> ColorItemCard(color = color, isSelected = categoryUiState.color.equals(color.hexValue, ignoreCase = true), onColorSelected = { onColorSelected(color.hexValue); onDismiss() }, onUpdateColor = onUpdateColor, onToggleVisibility = { onToggleColorVisibility(color.id) }) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitManagementDialog(
    allUnits: List<MeasurementUnit>,
    defaultUnitId: Int?,
    onDismiss: () -> Unit,
    onAddUnit: (String, String) -> Unit,
    onUpdateUnit: (MeasurementUnit) -> Unit,
    onUpdateUnitsOrder: (List<MeasurementUnit>) -> Unit,
    onToggleUnitVisibility: (Int) -> Unit,
    onDeleteUnit: (MeasurementUnit) -> Unit,
    onToggleDefaultUnit: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddUnitDialog by remember { mutableStateOf(false) }
    var showHidden by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val unitsState = remember(allUnits, showHidden) { allUnits.filter { !it.isHidden || showHidden }.toMutableStateList() }

    if (showAddUnitDialog) {
        var label by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddUnitDialog = false },
            title = { Text(stringResource(R.string.options_add_unit)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { if (it.length <= AppConstants.UNIT_LABEL_MAX_LENGTH) label = it },
                        label = { Text(stringResource(R.string.options_unit_label_placeholder)) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { if (it.length <= AppConstants.UNIT_DESCRIPTION_MAX_LENGTH) description = it },
                        label = { Text(stringResource(R.string.options_unit_desc_placeholder)) },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onAddUnit(label, description)
                    showAddUnitDialog = false
                }) { Text(stringResource(R.string.button_add)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddUnitDialog = false }) { Text(stringResource(R.string.button_cancel)) }
            }
        )
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(modifier = Modifier.padding(16.dp), shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
            Scaffold(
                modifier = Modifier.height(500.dp),
                floatingActionButton = {
                    Column(horizontalAlignment = Alignment.End) {
                        FloatingActionButton(onClick = { showAddUnitDialog = true }) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.options_add_unit)) }
                        Spacer(Modifier.height(8.dp))
                        FloatingActionButton(onClick = { showHidden = !showHidden; scope.launch { lazyListState.animateScrollToItem(0) } }, containerColor = MaterialTheme.colorScheme.secondary) { Icon(if (showHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = stringResource(R.string.options_show_hide)) }
                    }
                }
            ) { paddingValues ->
                Column(Modifier.padding(paddingValues).padding(16.dp)) {
                    Text(text = stringResource(R.string.options_unit_mgmt_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), textAlign = TextAlign.Center)
                    
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.options_info_select_default) + " " + stringResource(R.string.options_info_drag_reorder),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    DraggableLazyColumn(
                        items = unitsState,
                        key = { _, unit -> unit.id },
                        onMove = { from, to -> unitsState.add(to, unitsState.removeAt(from)) },
                        onDrop = { 
                            val hiddenUnits = allUnits.filter { it.isHidden && !showHidden }
                            val fullNewList = unitsState + hiddenUnits
                            onUpdateUnitsOrder(fullNewList.mapIndexed { index, unit -> unit.copy(displayOrder = index) }) 
                        },
                        itemContent = { _, unit -> 
                            UnitItemCard(
                                unit = unit, 
                                isDefault = unit.id == defaultUnitId,
                                onUnitSelected = { onToggleDefaultUnit(unit.id) },
                                onUpdateUnit = onUpdateUnit, 
                                onToggleVisibility = { onToggleUnitVisibility(unit.id) },
                                onDeleteUnit = { onDeleteUnit(unit) }
                            ) 
                        }
                    )
                }
            }
        }
    }
}
