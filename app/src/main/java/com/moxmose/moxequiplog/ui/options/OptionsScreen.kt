package com.moxmose.moxequiplog.ui.options

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.moxmose.moxequiplog.BuildConfig
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.AppColor
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.Image
import com.moxmose.moxequiplog.data.local.ImageIdentifier
import com.moxmose.moxequiplog.ui.components.AddColorDialog
import com.moxmose.moxequiplog.ui.components.ColorItemCard
import com.moxmose.moxequiplog.ui.components.DraggableLazyColumn
import com.moxmose.moxequiplog.ui.components.ImageSelector
import com.moxmose.moxequiplog.ui.components.OptionsSectionCard
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun OptionsScreen(modifier: Modifier = Modifier, viewModel: OptionsViewModel = koinViewModel()) {
    val username by viewModel.username.collectAsState()
    val allImages by viewModel.allImages.collectAsState()
    val categoriesUiState by viewModel.categoriesUiState.collectAsState()
    val allColors by viewModel.allColors.collectAsState()
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
        onUsernameChange = viewModel::setUsername,
        onSetCategoryDefault = viewModel::setCategoryDefault,
        onAddImage = viewModel::addImage,
        onRemoveImage = viewModel::removeImage,
        onUpdateImageOrder = viewModel::updateImageOrder,
        onToggleImageVisibility = viewModel::toggleImageVisibility,
        onUpdateCategoryColor = viewModel::updateCategoryColor,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsScreenContent(
    modifier: Modifier = Modifier,
    username: String,
    allImages: List<Image>,
    categoriesUiState: List<CategoryUiState>,
    allColors: List<AppColor>,
    onUsernameChange: (String) -> Unit,
    onSetCategoryDefault: (String, ImageIdentifier?) -> Unit,
    onAddImage: (ImageIdentifier, String) -> Unit,
    onRemoveImage: (Image) -> Unit,
    onUpdateImageOrder: (List<Image>) -> Unit,
    onToggleImageVisibility: (Image) -> Unit,
    onUpdateCategoryColor: (String, String) -> Unit,
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
    
    val categoryColorsMap = remember(categoriesUiState) { categoriesUiState.associate { it.category.id to it.color } }
    val categoryDefaultIconsMap = remember(categoriesUiState) { categoriesUiState.associate { it.category.id to it.defaultIconIdentifier } }
    val categoryDefaultPhotosMap = remember(categoriesUiState) { categoriesUiState.associate { it.category.id to it.defaultPhotoUri } }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { onShowAboutDialogChange(false) },
            title = { Text(stringResource(R.string.about_dialog_title)) },
            text = { Text(stringResource(R.string.about_dialog_content, BuildConfig.VERSION_NAME)) },
            confirmButton = {
                TextButton(onClick = { onShowAboutDialogChange(false) }) {
                    Text(stringResource(R.string.button_ok))
                }
            }
        )
    }

    if (showImageDialog) {
        val allCategories = categoriesUiState.map { it.category }
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
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

            OptionsSectionCard(title = "Profilo") {
                OutlinedTextField(
                    value = editedUsername,
                    onValueChange = { editedUsername = it },
                    label = { Text("Nome Utente") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (editedUsername != username && editedUsername.isNotBlank()) {
                            IconButton(onClick = { onUsernameChange(editedUsername) }) {
                                Icon(Icons.Default.Done, contentDescription = "Save Username")
                            }
                        }
                    }
                )
            }

            OptionsSectionCard(
                title = "Sezioni e Colori",
                description = "Personalizza i colori identificativi per ogni sezione dell'app."
            ) {
                categoriesUiState.sortedBy { it.category.name }.forEach { uiState ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(uiState.category.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(uiState.color)))
                                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable { onShowColorPickerChange(uiState.category.id) }
                        )
                    }
                    if (uiState != categoriesUiState.last()) HorizontalDivider(Modifier.padding(vertical = 12.dp))
                }
            }

            OptionsSectionCard(
                title = "Gestione Immagini e Default",
                description = "Punto unico per gestire le immagini. Seleziona una categoria specifica per impostare l'elemento di default per quella sezione."
            ) {
                OutlinedButton(
                    onClick = { onShowImageDialogChange(true) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (allImages.isEmpty()) {
                                Text("Libreria vuota")
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
                                        imageLibrary = allImages
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Gestisci",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Gestisci Immagini",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onShowAboutDialogChange(true) },
                modifier = Modifier.fillMaxWidth()
            ) {
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

    val colorsState = remember(allColors, showHidden) {
        allColors.filter { !it.hidden || showHidden }.toMutableStateList()
    }

    if (showAddColorDialog) {
        AddColorDialog(
            onDismiss = { showAddColorDialog = false },
            onAddColor = { hex, name -> onAddColor(hex, name) }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Scaffold(
            floatingActionButton = {
                Column(horizontalAlignment = Alignment.End) {
                    FloatingActionButton(onClick = { showAddColorDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Aggiungi colore")
                    }
                    Spacer(Modifier.height(8.dp))
                    FloatingActionButton(
                        onClick = {
                            showHidden = !showHidden
                            scope.launch { lazyListState.animateScrollToItem(0) }
                        },
                        containerColor = MaterialTheme.colorScheme.secondary
                    ) {
                        Icon(if (showHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = "Mostra/Nascondi")
                    }
                }
            }
        ) { padding ->
            Surface(modifier = Modifier.padding(padding)) {
                Column {
                    Text(
                        text = "Gestione Colori",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        textAlign = TextAlign.Center
                    )
                    DraggableLazyColumn(
                        items = colorsState,
                        key = { _, color -> color.id },
                        onMove = { from, to -> colorsState.add(to, colorsState.removeAt(from)) },
                        onDrop = {
                            val hiddenColors = allColors.filter { it.hidden && !showHidden }
                            val fullNewList = colorsState + hiddenColors
                            onUpdateColorsOrder(fullNewList.mapIndexed { index, appColor -> appColor.copy(displayOrder = index) })
                        },
                        itemContent = { _, color ->
                            ColorItemCard(
                                color = color,
                                isSelected = categoryUiState.color.equals(color.hexValue, ignoreCase = true),
                                onColorSelected = {
                                    onColorSelected(color.hexValue)
                                    onDismiss()
                                },
                                onUpdateColor = onUpdateColor,
                                onToggleVisibility = { onToggleColorVisibility(color.id) }
                            )
                        }
                    )
                }
            }
        }
    }
}
