package com.moxmose.moxequiplog.ui.maintenancelog

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.Equipment
import com.moxmose.moxequiplog.data.local.MaintenanceLog
import com.moxmose.moxequiplog.data.local.MaintenanceLogDetails
import com.moxmose.moxequiplog.data.local.OperationType
import com.moxmose.moxequiplog.ui.components.MediaIcon
import com.moxmose.moxequiplog.ui.options.OptionsViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*


enum class SortProperty {
    DATE, EQUIPMENT, OPERATION, KILOMETERS, NOTES
}

enum class SortDirection {
    ASCENDING, DESCENDING
}

@Composable
fun MaintenanceLogScreen(viewModel: MaintenanceLogViewModel = koinViewModel(), optionsViewModel: OptionsViewModel = koinViewModel()) {
    val logs by viewModel.logs.collectAsState()
    val equipments by viewModel.allEquipments.collectAsState()
    val operationTypes by viewModel.allOperationTypes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortProperty by viewModel.sortProperty.collectAsState()
    val sortDirection by viewModel.sortDirection.collectAsState()
    val showDismissed by viewModel.showDismissed.collectAsState()
    val allCategories by optionsViewModel.allCategories.collectAsState()

    val activeEquipments = remember(equipments) { equipments.filter { !it.dismissed }.sortedBy { it.displayOrder } }
    val activeOperationTypes = remember(operationTypes) { operationTypes.filter { !it.dismissed }.sortedBy { it.displayOrder } }

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var expandedCardId by rememberSaveable { mutableStateOf<Int?>(null) }
    var editingCardId by rememberSaveable { mutableStateOf<Int?>(null) }

    MaintenanceLogScreenContent(
        logs = logs,
        equipments = activeEquipments,
        operationTypes = activeOperationTypes,
        searchQuery = searchQuery,
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        sortProperty = sortProperty,
        onSortPropertyChange = viewModel::onSortPropertyChanged,
        sortDirection = sortDirection,
        onSortDirectionChange = viewModel::onSortDirectionChanged,
        showDismissed = showDismissed,
        onShowDismissedToggle = viewModel::onShowDismissedToggled,
        showAddDialog = showAddDialog,
        onShowAddDialogChange = { showAddDialog = it },
        onAddLog = viewModel::addLog,
        expandedCardId = expandedCardId,
        onCardExpanded = { id -> expandedCardId = if (expandedCardId == id) null else id },
        editingCardId = editingCardId,
        onEditLog = { log -> editingCardId = log.id },
        onUpdateLog = {
            viewModel.updateLog(it)
            editingCardId = null
        },
        onDismissLog = viewModel::dismissLog,
        onRestoreLog = viewModel::restoreLog,
        allCategories = allCategories
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceLogScreenContent(
    logs: List<MaintenanceLogDetails>,
    equipments: List<Equipment>,
    operationTypes: List<OperationType>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortProperty: SortProperty,
    onSortPropertyChange: (SortProperty) -> Unit,
    sortDirection: SortDirection,
    onSortDirectionChange: () -> Unit,
    showDismissed: Boolean,
    onShowDismissedToggle: () -> Unit,
    showAddDialog: Boolean,
    onShowAddDialogChange: (Boolean) -> Unit,
    onAddLog: (Int, Int, String?, Int?, Long, String?) -> Unit,
    expandedCardId: Int?,
    onCardExpanded: (Int) -> Unit,
    editingCardId: Int?,
    onEditLog: (MaintenanceLog) -> Unit,
    onUpdateLog: (MaintenanceLog) -> Unit,
    onDismissLog: (MaintenanceLog) -> Unit,
    onRestoreLog: (MaintenanceLog) -> Unit,
    allCategories: List<Category>
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(onClick = { onShowAddDialogChange(true) }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_log))
                }
                Spacer(modifier = Modifier.padding(8.dp))
                FloatingActionButton(
                    onClick = onShowDismissedToggle,
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
            MaintenanceLogDialog(
                equipments = equipments,
                operationTypes = operationTypes,
                onDismissRequest = { onShowAddDialogChange(false) },
                onConfirm = { log ->
                    onAddLog(log.equipmentId, log.operationTypeId, log.notes, log.kilometers, log.date, log.color)
                    onShowAddDialogChange(false)
                },
                allCategories = allCategories
            )
        }

        Column(Modifier.padding(paddingValues)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text(stringResource(R.string.search_logs)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_logs)) },
                    modifier = Modifier.weight(1f)
                )

                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.sort_by))
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortProperty.values().forEach { prop ->
                            DropdownMenuItem(
                                text = { Text(prop.name.lowercase().replaceFirstChar { it.titlecase() }) },
                                onClick = {
                                    onSortPropertyChange(prop)
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (sortProperty == prop) {
                                        Icon(Icons.Default.Check, contentDescription = "Selected")
                                    }
                                }
                            )
                        }
                    }
                }

                IconButton(onClick = onSortDirectionChange) {
                    Icon(
                        imageVector = if (sortDirection == SortDirection.DESCENDING) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = stringResource(R.string.sort_direction)
                    )
                }
            }
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
                items(logs, key = { it.log.id }) { logDetail ->
                    MaintenanceLogCard(
                        logDetail = logDetail,
                        equipments = equipments,
                        operationTypes = operationTypes,
                        isExpanded = logDetail.log.id == expandedCardId,
                        isEditing = logDetail.log.id == editingCardId,
                        onExpand = { onCardExpanded(logDetail.log.id) },
                        onEdit = { onEditLog(logDetail.log) },
                        onSave = onUpdateLog,
                        onDismiss = { onDismissLog(logDetail.log) },
                        onRestore = { onRestoreLog(logDetail.log) },
                        allCategories = allCategories
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceLogDialog(
    equipments: List<Equipment>,
    operationTypes: List<OperationType>,
    onDismissRequest: () -> Unit,
    onConfirm: (MaintenanceLog) -> Unit,
    allCategories: List<Category>
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    var notes by remember { mutableStateOf("") }
    var kilometers by remember { mutableStateOf("") }
    var selectedEquipment by remember { mutableStateOf<Equipment?>(null) }
    var selectedOperationType by remember { mutableStateOf<OperationType?>(null) }
    var isEquipmentDropdownExpanded by remember { mutableStateOf(false) }
    var isOperationDropdownExpanded by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDate = it }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.button_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.add_new_maintenance_log)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = isEquipmentDropdownExpanded,
                    onExpandedChange = { isEquipmentDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedEquipment?.description?.takeIf { it.isNotBlank() } ?: selectedEquipment?.let { "id:${it.id} - no description" } ?: stringResource(R.string.select_an_equipment),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.navigation_equipments)) },
                        leadingIcon = {
                            MediaIcon(
                                photoUri = selectedEquipment?.photoUri,
                                iconIdentifier = selectedEquipment?.iconIdentifier,
                                modifier = Modifier.size(24.dp),
                                category = "EQUIPMENT"
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isEquipmentDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isEquipmentDropdownExpanded,
                        onDismissRequest = { isEquipmentDropdownExpanded = false }
                    ) {
                        equipments.forEach { equipment ->
                            DropdownMenuItem(
                                text = { Text(equipment.description.takeIf { it.isNotBlank() } ?: "id:${equipment.id} - no description") },
                                leadingIcon = {
                                    MediaIcon(
                                        photoUri = equipment.photoUri,
                                        iconIdentifier = equipment.iconIdentifier,
                                        modifier = Modifier.size(24.dp),
                                        category = "EQUIPMENT"
                                    )
                                },
                                onClick = {
                                    selectedEquipment = equipment
                                    isEquipmentDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = isOperationDropdownExpanded,
                    onExpandedChange = { isOperationDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedOperationType?.description?.takeIf { it.isNotBlank() } ?: selectedOperationType?.let { "id:${it.id} - no description" } ?: stringResource(R.string.select_an_operation),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.navigation_operations)) },
                        leadingIcon = {
                            MediaIcon(
                                photoUri = selectedOperationType?.photoUri,
                                iconIdentifier = selectedOperationType?.iconIdentifier,
                                modifier = Modifier.size(24.dp),
                                category = "OPERATION"
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isOperationDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isOperationDropdownExpanded,
                        onDismissRequest = { isOperationDropdownExpanded = false }
                    ) {
                        operationTypes.forEach { operation ->
                            DropdownMenuItem(
                                text = { Text(operation.description.takeIf { it.isNotBlank() } ?: "id:${operation.id} - no description") },
                                leadingIcon = {
                                    MediaIcon(
                                        photoUri = operation.photoUri,
                                        iconIdentifier = operation.iconIdentifier,
                                        modifier = Modifier.size(24.dp),
                                        category = "OPERATION"
                                    )
                                },
                                onClick = {
                                    selectedOperationType = operation
                                    isOperationDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = kilometers,
                    onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) kilometers = it },
                    label = { Text(stringResource(R.string.kilometers_optional)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { if (it.length <= 200) notes = it },
                    label = { Text(stringResource(R.string.notes_optional)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dateFormat.format(Date(selectedDate)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.date)) },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.select_date))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val equipment = selectedEquipment
                    val op = selectedOperationType
                    if (equipment != null && op != null) {
                        onConfirm(
                            MaintenanceLog(
                                equipmentId = equipment.id,
                                operationTypeId = op.id,
                                notes = notes.takeIf { it.isNotBlank() },
                                kilometers = kilometers.toIntOrNull(),
                                date = selectedDate
                            )
                        )
                    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceLogCard(
    logDetail: MaintenanceLogDetails,
    equipments: List<Equipment>,
    operationTypes: List<OperationType>,
    isExpanded: Boolean,
    isEditing: Boolean,
    onExpand: () -> Unit,
    onEdit: () -> Unit,
    onSave: (MaintenanceLog) -> Unit,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
    allCategories: List<Category>
) {
    var editedNotes by remember(logDetail, isEditing) { mutableStateOf(logDetail.log.notes ?: "") }
    var editedKilometers by remember(logDetail, isEditing) { mutableStateOf(logDetail.log.kilometers?.toString() ?: "") }
    var editedDate by remember(logDetail, isEditing) { mutableLongStateOf(logDetail.log.date) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedEquipment by remember(logDetail, isEditing) { mutableStateOf(equipments.find { it.id == logDetail.log.equipmentId }) }
    var selectedOperationType by remember(logDetail, isEditing) { mutableStateOf(operationTypes.find { it.id == logDetail.log.operationTypeId }) }
    var isEquipmentDropdownExpanded by remember { mutableStateOf(false) }
    var isOperationDropdownExpanded by remember { mutableStateOf(false) }

    val cardAlpha = if (logDetail.log.dismissed) 0.5f else 1f
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    val equipmentCategory = allCategories.find { it.id == "EQUIPMENT" }
    val operationCategory = allCategories.find { it.id == "OPERATION" }

    val equipmentBorderColor = equipmentCategory?.color?.let { Color(android.graphics.Color.parseColor(it)) } ?: MaterialTheme.colorScheme.primary
    val operationBorderColor = operationCategory?.color?.let { Color(android.graphics.Color.parseColor(it)) } ?: MaterialTheme.colorScheme.primary

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = editedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { editedDate = it }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.button_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .graphicsLayer(alpha = cardAlpha)
            .clickable { onExpand() }
            .animateContentSize(),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isEditing) {
                    ExposedDropdownMenuBox(
                        expanded = isEquipmentDropdownExpanded,
                        onExpandedChange = { isEquipmentDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedEquipment?.description?.takeIf { it.isNotBlank() } ?: selectedEquipment?.let { "id:${it.id} - no description" } ?: stringResource(id = R.string.select_an_equipment),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.navigation_equipments)) },
                            leadingIcon = {
                                MediaIcon(
                                    photoUri = selectedEquipment?.photoUri,
                                    iconIdentifier = selectedEquipment?.iconIdentifier,
                                    modifier = Modifier.size(24.dp),
                                    category = "EQUIPMENT"
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isEquipmentDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = isEquipmentDropdownExpanded,
                            onDismissRequest = { isEquipmentDropdownExpanded = false }
                        ) {
                            equipments.forEach { equipment ->
                                DropdownMenuItem(
                                    text = { Text(equipment.description.takeIf { it.isNotBlank() } ?: "id:${equipment.id} - no description") },
                                    leadingIcon = {
                                        MediaIcon(
                                            photoUri = equipment.photoUri,
                                            iconIdentifier = equipment.iconIdentifier,
                                            modifier = Modifier.size(24.dp),
                                            category = "EQUIPMENT"
                                        )
                                    },
                                    onClick = {
                                        selectedEquipment = equipment
                                        isEquipmentDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    ExposedDropdownMenuBox(
                        expanded = isOperationDropdownExpanded,
                        onExpandedChange = { isOperationDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedOperationType?.description?.takeIf { it.isNotBlank() } ?: selectedOperationType?.let { "id:${it.id} - no description" } ?: stringResource(id = R.string.select_an_operation),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.navigation_operations)) },
                            leadingIcon = {
                                MediaIcon(
                                    photoUri = selectedOperationType?.photoUri,
                                    iconIdentifier = selectedOperationType?.iconIdentifier,
                                    modifier = Modifier.size(24.dp),
                                    category = "OPERATION"
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isOperationDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = isOperationDropdownExpanded,
                            onDismissRequest = { isOperationDropdownExpanded = false }
                        ) {
                            operationTypes.forEach { operation ->
                                DropdownMenuItem(
                                    text = { Text(operation.description.takeIf { it.isNotBlank() } ?: "id:${operation.id} - no description") },
                                    leadingIcon = {
                                        MediaIcon(
                                            photoUri = operation.photoUri,
                                            iconIdentifier = operation.iconIdentifier,
                                            modifier = Modifier.size(24.dp),
                                            category = "OPERATION"
                                        )
                                    },
                                    onClick = {
                                        selectedOperationType = operation
                                        isOperationDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = editedKilometers,
                        onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) editedKilometers = it },
                        label = { Text(stringResource(R.string.kilometers_optional)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editedNotes,
                        onValueChange = { if (it.length <= 200) editedNotes = it },
                        label = { Text(stringResource(R.string.notes_optional)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = { showDatePicker = true }) {
                        Text(text = stringResource(R.string.date) + ": " + dateFormat.format(Date(editedDate)))
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val equipmentTextAlpha = if (logDetail.equipmentDismissed) 0.5f else 1f
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(1.dp, equipmentBorderColor, CircleShape)
                                .graphicsLayer(alpha = equipmentTextAlpha)
                        ) {
                            MediaIcon(
                                photoUri = logDetail.equipmentPhotoUri,
                                iconIdentifier = logDetail.equipmentIconIdentifier,
                                modifier = Modifier.size(24.dp),
                                category = "EQUIPMENT"
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = (logDetail.equipmentDescription.takeIf { it.isNotBlank() } ?: "id:${logDetail.log.equipmentId} - no description") + if (logDetail.equipmentDismissed) " (dismissed)" else "",
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.graphicsLayer(alpha = equipmentTextAlpha)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val operationTypeAlpha = if (logDetail.operationTypeDismissed) 0.5f else 1f
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(1.dp, operationBorderColor, CircleShape)
                                .graphicsLayer(alpha = operationTypeAlpha)
                        ) {
                            MediaIcon(
                                photoUri = logDetail.operationTypePhotoUri,
                                iconIdentifier = logDetail.operationTypeIconIdentifier,
                                modifier = Modifier.size(24.dp),
                                category = "OPERATION"
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = (logDetail.operationTypeDescription.takeIf { it.isNotBlank() } ?: "id:${logDetail.log.operationTypeId} - no description") + if (logDetail.operationTypeDismissed) " (dismissed)" else "",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.graphicsLayer(alpha = operationTypeAlpha)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = logDetail.log.kilometers?.let { "Km: $it" } ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = dateFormat.format(Date(logDetail.log.date)),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    logDetail.log.notes?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Column {
                IconButton(onClick = {
                    if (isEditing) {
                        val updatedLog = logDetail.log.copy(
                            notes = editedNotes,
                            kilometers = editedKilometers.toIntOrNull(),
                            date = editedDate,
                            equipmentId = selectedEquipment?.id ?: logDetail.log.equipmentId,
                            operationTypeId = selectedOperationType?.id ?: logDetail.log.operationTypeId
                        )
                        onSave(updatedLog)
                    } else {
                        onEdit()
                    }
                }) {
                    Icon(
                        imageVector = if (isEditing) Icons.Filled.Done else Icons.Filled.Edit,
                        contentDescription = if (isEditing) stringResource(R.string.save_log) else stringResource(R.string.edit_log)
                    )
                }
                if (isEditing) {
                    IconButton(
                        onClick = {
                            if (logDetail.log.dismissed) {
                                onRestore()
                            } else {
                                onDismiss()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (logDetail.log.dismissed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (logDetail.log.dismissed) stringResource(R.string.restore_log) else stringResource(R.string.dismiss_log)
                        )
                    }
                }
            }
        }
    }
}
