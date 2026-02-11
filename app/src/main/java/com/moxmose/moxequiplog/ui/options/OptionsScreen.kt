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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moxmose.moxequiplog.BuildConfig
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.AppColor
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.Media
import com.moxmose.moxequiplog.data.local.MediaIdentifier
import com.moxmose.moxequiplog.ui.components.DraggableLazyColumn
import com.moxmose.moxequiplog.ui.components.MediaIcon
import com.moxmose.moxequiplog.ui.components.MediaSelector
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun OptionsScreen(modifier: Modifier = Modifier, viewModel: OptionsViewModel = koinViewModel()) {
    val username by viewModel.username.collectAsState()
    val allMedia by viewModel.allMedia.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val allColors by viewModel.allColors.collectAsState()

    var showAboutDialog by rememberSaveable { mutableStateOf(false) }
    var showColorPicker by rememberSaveable { mutableStateOf<String?>(null) }
    var showMediaDialog by rememberSaveable { mutableStateOf(false) }
    var errorToShow by remember { mutableStateOf<OptionsViewModel.OptionsUiEvent?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collectLatest { event ->
            errorToShow = event
        }
    }

    errorToShow?.let { event ->
        AlertDialog(
            onDismissRequest = { errorToShow = null },
            title = { Text(stringResource(R.string.error_dialog_title)) },
            text = {
                when (event) {
                    is OptionsViewModel.OptionsUiEvent.RemoveMediaFailed -> Text(stringResource(R.string.error_remove_media_failed))
                    is OptionsViewModel.OptionsUiEvent.UpdateColorFailed -> Text(stringResource(R.string.error_update_color_failed))
                    is OptionsViewModel.OptionsUiEvent.ColorNameInvalid -> Text(stringResource(R.string.error_color_name_invalid))
                    else -> {
                        if (BuildConfig.DEBUG) {
                            throw IllegalStateException("Unhandled UI Event: $event")
                        } else {
                            Text(stringResource(R.string.error_unknown))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { errorToShow = null }) {
                    Text(stringResource(R.string.button_ok))
                }
            }
        )
    }

    OptionsScreenContent(
        modifier = modifier,
        username = username,
        allMedia = allMedia,
        allCategories = allCategories,
        allColors = allColors,
        onUsernameChange = viewModel::setUsername,
        onSetCategoryDefault = { categoryId, mediaIdentifier -> viewModel.setCategoryDefault(categoryId, mediaIdentifier) },
        onAddMedia = { mediaIdentifier, category -> viewModel.addMedia(mediaIdentifier, category) },
        onRemoveMedia = viewModel::removeMedia,
        onUpdateMediaOrder = viewModel::updateMediaOrder,
        onToggleMediaVisibility = viewModel::toggleMediaVisibility,
        onUpdateCategoryColor = {
                catId, hex -> viewModel.updateCategoryColor(catId, hex)
        },
        isPhotoUsed = { viewModel.isPhotoUsed(it) },
        showAboutDialog = showAboutDialog,
        onShowAboutDialogChange = { showAboutDialog = it },
        showColorPicker = showColorPicker,
        onShowColorPickerChange = { showColorPicker = it },
        showMediaDialog = showMediaDialog,
        onShowMediaDialogChange = { showMediaDialog = it },
        onAddColor = viewModel::addColor,
        onUpdateColor = viewModel::updateColor,
        onUpdateColorsOrder = viewModel::updateColorsOrder,
        onToggleColorVisibility = viewModel::toggleColorVisibility
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsScreenContent(
    modifier: Modifier = Modifier,
    username: String,
    allMedia: List<Media>,
    allCategories: List<Category>,
    allColors: List<AppColor>,
    onUsernameChange: (String) -> Unit,
    onSetCategoryDefault: (String, MediaIdentifier?) -> Unit,
    onAddMedia: (MediaIdentifier, String) -> Unit,
    onRemoveMedia: (Media) -> Unit,
    onUpdateMediaOrder: (List<Media>) -> Unit,
    onToggleMediaVisibility: (Media) -> Unit,
    onUpdateCategoryColor: (String, String) -> Unit,
    isPhotoUsed: suspend (String) -> Boolean,
    showAboutDialog: Boolean,
    onShowAboutDialogChange: (Boolean) -> Unit,
    showColorPicker: String?,
    onShowColorPickerChange: (String?) -> Unit,
    showMediaDialog: Boolean,
    onShowMediaDialogChange: (Boolean) -> Unit,
    onAddColor: (String, String) -> Unit,
    onUpdateColor: (AppColor) -> Unit,
    onUpdateColorsOrder: (List<AppColor>) -> Unit,
    onToggleColorVisibility: (Long) -> Unit
) {
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

    if (showMediaDialog) {
        MediaSelector(
            photoUri = null,
            iconIdentifier = null,
            mediaLibrary = allMedia,
            categories = allCategories,
            onMediaSelected = { _, _ -> },
            onAddMedia = { uri, category -> onAddMedia(MediaIdentifier.Photo(uri), category) },
            onRemoveMedia = { uri, category -> allMedia.find { it.uri == uri && it.category == category }?.let { onRemoveMedia(it) } },
            onUpdateMediaOrder = onUpdateMediaOrder,
            onToggleMediaVisibility = { uri, category -> allMedia.find { it.uri == uri && it.category == category }?.let { onToggleMediaVisibility(it) } },
            onSetDefaultInCategory = { categoryId, iconId, photoUri ->
                val identifier = when {
                    iconId != null -> MediaIdentifier.Icon(iconId)
                    photoUri != null -> MediaIdentifier.Photo(photoUri)
                    else -> null
                }
                onSetCategoryDefault(categoryId, identifier)
            },
            isPhotoUsed = isPhotoUsed,
            isPrefsMode = true,
            onDismissRequest = { onShowMediaDialogChange(false) }
        )
    }

    showColorPicker?.let { categoryId ->
        val category = allCategories.find { it.id == categoryId }!!
        ColorManagementDialog(
            allColors = allColors,
            category = category,
            onDismiss = { onShowColorPickerChange(null) },
            onColorSelected = { onUpdateCategoryColor(category.id, it) },
            onAddColor = onAddColor,
            onUpdateColor = onUpdateColor,
            onUpdateColorsOrder = onUpdateColorsOrder,
            onToggleColorVisibility = onToggleColorVisibility
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
                value = username,
                onValueChange = onUsernameChange,
                label = { Text("Nome Utente") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        OptionsSectionCard(
            title = "Sezioni e Colori",
            description = "Personalizza i colori identificativi per ogni sezione dell'app."
        ) {
            allCategories.sortedBy { it.name }.forEach { category ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(category.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(category.color)))
                            .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable { onShowColorPickerChange(category.id) }
                    )
                }
                if (category != allCategories.last()) Divider(Modifier.padding(vertical = 12.dp))
            }
        }

        OptionsSectionCard(
            title = "Gestione Media e Default",
            description = "Punto unico per gestire i media. Seleziona una categoria specifica per impostare l'elemento di default per quella sezione."
        ) {
            OutlinedButton(
                onClick = { onShowMediaDialogChange(true) },
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
                        if (allMedia.isEmpty()) {
                            Text("Libreria vuota")
                        } else {
                            allMedia.filter { !it.hidden }.distinctBy { it.uri }.take(4).forEach { media ->
                                MediaIcon(
                                    photoUri = if (media.mediaType == "IMAGE") media.uri else null,
                                    iconIdentifier = if (media.mediaType == "ICON") media.uri.removePrefix("icon:") else null,
                                    modifier = Modifier.size(40.dp)
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
                            contentDescription = "Gestisci Media",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorManagementDialog(
    allColors: List<AppColor>,
    category: Category,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit,
    onAddColor: (String, String) -> Unit,
    onUpdateColor: (AppColor) -> Unit,
    onUpdateColorsOrder: (List<AppColor>) -> Unit,
    onToggleColorVisibility: (Long) -> Unit
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

    AlertDialog(
        onDismissRequest = onDismiss,
    ) {
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
            Column(Modifier.padding(padding)) {
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
                            isSelected = category.color.equals(color.hexValue, ignoreCase = true),
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

@Composable
fun ColorItemCard(
    color: AppColor,
    isSelected: Boolean,
    onColorSelected: () -> Unit,
    onUpdateColor: (AppColor) -> Unit,
    onToggleVisibility: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember(color.name) { mutableStateOf(color.name) }
    val cardAlpha = if (color.hidden) 0.5f else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onColorSelected() }
            .graphicsLayer(alpha = cardAlpha)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(color.hexValue)))
                    .border(
                        2.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        CircleShape
                    )
            )
            Spacer(Modifier.width(16.dp))
            if (isEditing) {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Nome Colore") },
                    modifier = Modifier.weight(1f)
                )
            } else {
                Text(
                    text = color.name.ifEmpty { color.hexValue },
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEditing) {
                    IconButton(onClick = onToggleVisibility) {
                        Icon(if (color.hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = "Visibilità")
                    }
                }
                IconButton(onClick = {
                    if (isEditing) onUpdateColor(color.copy(name = editedName))
                    isEditing = !isEditing
                }) {
                    Icon(if (isEditing) Icons.Default.Done else Icons.Default.Edit, contentDescription = "Modifica")
                }
                IconButton(onClick = { /* Drag is handled by the parent */ }) {
                    Icon(Icons.Default.DragHandle, contentDescription = "Trascina")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddColorDialog(
    onDismiss: () -> Unit,
    onAddColor: (String, String) -> Unit,
) {
    var hex by remember { mutableStateOf("#") }
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val predefinedColors = listOf(
        Color(0xFFE57373), Color(0xFFF06292), Color(0xFFBA68C8), Color(0xFF9575CD), Color(0xFF7986CB),
        Color(0xFF64B5F6), Color(0xFF4FC3F7), Color(0xFF4DD0E1), Color(0xFF4DB6AC), Color(0xFF81C784),
        Color(0xFFAED581), Color(0xFFDCE775), Color(0xFFFFF176), Color(0xFFFFD54F), Color(0xFFFFB74D),
        Color(0xFFFF8A65), Color(0xFFA1887F), Color(0xFF90A4AE), Color(0xFFB0BEC5), Color(0xFFEEEEEE)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aggiungi un nuovo colore") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(predefinedColors) {
                        color ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable {
                                    hex = "#" + color.value.toString(16).substring(2, 8).uppercase()
                                }
                        )
                    }
                }

                OutlinedTextField(
                    value = hex,
                    onValueChange = {
                        if (it.startsWith("#") && it.length <= 7) hex = it
                    },
                    label = { Text("Codice Esadecimale (es. #RRGGBB)") },
                    isError = error != null,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") }
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    try {
                        Color(android.graphics.Color.parseColor(hex))
                        onAddColor(hex, name)
                        onDismiss()
                    } catch (e: Exception) {
                        error = "Codice colore non valido!"
                    }
                }
            ) { Text("Aggiungi") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}

@Composable
fun OptionsSectionCard(
    title: String,
    description: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}
