package com.moxmose.moxequiplog.ui.options

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.ImageRepository
import com.moxmose.moxequiplog.data.MaintenanceManager
import com.moxmose.moxequiplog.data.SectionRepository
import com.moxmose.moxequiplog.data.local.AppColor
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.Equipment
import com.moxmose.moxequiplog.data.local.EquipmentDao
import com.moxmose.moxequiplog.data.local.Image
import com.moxmose.moxequiplog.data.local.ImageIdentifier
import com.moxmose.moxequiplog.data.local.MaintenanceLog
import com.moxmose.moxequiplog.data.local.MaintenanceLogDao
import com.moxmose.moxequiplog.data.local.MaintenanceReminder
import com.moxmose.moxequiplog.data.local.MaintenanceReminderDao
import com.moxmose.moxequiplog.data.local.MeasurementUnit
import com.moxmose.moxequiplog.data.local.MeasurementUnitDao
import com.moxmose.moxequiplog.data.local.OperationType
import com.moxmose.moxequiplog.data.local.OperationTypeDao
import com.moxmose.moxequiplog.data.local.Section
import com.moxmose.moxequiplog.data.local.TimeGranularity
import com.moxmose.moxequiplog.utils.AppConstants
import com.moxmose.moxequiplog.utils.BackupManager
import com.moxmose.moxequiplog.utils.UiConstants
import java.util.Calendar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DemoScenario {
    CARS,
    GARDEN,
    HEALTH,
    BIKES,
    ALL
}

data class CategoryUiState(
    val category: Category,
    val color: String,
    val defaultIconIdentifier: String?,
    val defaultPhotoUri: String?
)

@OptIn(ExperimentalCoroutinesApi::class)
class OptionsViewModel(
    private val appSettingsManager: AppSettingsManager,
    private val sectionRepository: SectionRepository,
    private val equipmentDao: EquipmentDao,
    private val maintenanceLogDao: MaintenanceLogDao,
    private val operationTypeDao: OperationTypeDao,
    private val imageRepository: ImageRepository,
    private val measurementUnitDao: MeasurementUnitDao,
    private val maintenanceReminderDao: MaintenanceReminderDao,
    private val backupManager: BackupManager,
    private val maintenanceManager: MaintenanceManager
) : ViewModel() {

    sealed class OptionsUiEvent {
        data object DatabaseCheckFailed : OptionsUiEvent()
        data object UsernameInvalid : OptionsUiEvent()
        data object UpdateUsernameFailed : OptionsUiEvent()
        data object RemoveImageFailed : OptionsUiEvent()
        data object UpdateColorFailed : OptionsUiEvent()
        data object ColorNameInvalid : OptionsUiEvent()
        data object PhotoUriInvalid : OptionsUiEvent()
        data object SetCategoryDefaultFailed : OptionsUiEvent()
        data object CategoryIdInvalid : OptionsUiEvent()
        data object NoImageSelectedForDefault : OptionsUiEvent()
        data object ToggleImageVisibilityFailed : OptionsUiEvent()
        data object AddImageFailed : OptionsUiEvent()
        data object UpdateImageOrderFailed : OptionsUiEvent()
        data object UpdateCategoryColorFailed : OptionsUiEvent()
        data object ColorHexInvalid : OptionsUiEvent()
        data class AddColorFailed(val name: String) : OptionsUiEvent()
        data object UpdateColorsOrderFailed : OptionsUiEvent()
        data object ToggleColorVisibilityFailed : OptionsUiEvent()
        data object ColorIdInvalid : OptionsUiEvent()
        data object DeleteColorFailed : OptionsUiEvent()
        data object UpdateBackgroundFailed : OptionsUiEvent()
        data object AddUnitFailed : OptionsUiEvent()
        data object AddUnitFailedDuplicate : OptionsUiEvent()
        data object DeleteUnitFailed : OptionsUiEvent()
        data object UpdateUnitFailed : OptionsUiEvent()
        data object UpdateUnitsOrderFailed : OptionsUiEvent()
        data object ToggleUnitVisibilityFailed : OptionsUiEvent()
        data object SetDefaultFailed : OptionsUiEvent()
        data object UpdateReportsSettingsFailed : OptionsUiEvent()
        data object UpdateSettingsFailed : OptionsUiEvent()
        data object UpdateGoogleAccountFailed : OptionsUiEvent()
        data class BackupResult(val success: Boolean, val message: String?) : OptionsUiEvent()
        data class RestoreResult(val success: Boolean, val message: String?) : OptionsUiEvent()
        data class TotalExportResult(val success: Boolean, val message: String?) : OptionsUiEvent()
        data object RecalculateSuccess : OptionsUiEvent()
        data object DemoDataGenerated : OptionsUiEvent()
        data object DemoDataDeleted : OptionsUiEvent()
        data object AddSectionFailed : OptionsUiEvent()
        data object UpdateSectionFailed : OptionsUiEvent()
        data object DeleteSectionFailed : OptionsUiEvent()
        data object UpdateSectionsOrderFailed : OptionsUiEvent()
    }

    private val _uiEvents = Channel<OptionsUiEvent>(Channel.BUFFERED)
    val uiEvents: Flow<OptionsUiEvent> = _uiEvents.receiveAsFlow()

    // Hoisted UI State
    private val _showAboutDialog = MutableStateFlow(false)
    val showAboutDialog = _showAboutDialog.asStateFlow()

    private val _colorMgmtState = MutableStateFlow<Pair<ColorManagerMode, String?>?>(null)
    val colorMgmtState = _colorMgmtState.asStateFlow()

    private val _showImageDialog = MutableStateFlow(false)
    val showImageDialog = _showImageDialog.asStateFlow()

    private val _showBackgroundPicker = MutableStateFlow(false)
    val showBackgroundPicker = _showBackgroundPicker.asStateFlow()

    private val _showUnitManagement = MutableStateFlow(false)
    val showUnitManagement = _showUnitManagement.asStateFlow()

    private val _showSectionManagement = MutableStateFlow(false)
    val showSectionManagement = _showSectionManagement.asStateFlow()

    private val _showRestoreConfirm = MutableStateFlow<Uri?>(null)
    val showRestoreConfirm = _showRestoreConfirm.asStateFlow()

    fun onShowAboutDialogChange(show: Boolean) { _showAboutDialog.value = show }
    fun onShowColorManager(mode: ColorManagerMode, categoryId: String?) { _colorMgmtState.value = mode to categoryId }
    fun onDismissColorManager() { _colorMgmtState.value = null }
    fun onShowImageDialogChange(show: Boolean) { _showImageDialog.value = show }
    fun onShowBackgroundPickerChange(show: Boolean) { _showBackgroundPicker.value = show }
    fun onShowUnitManagementChange(show: Boolean) { _showUnitManagement.value = show }
    fun onShowSectionManagementChange(show: Boolean) { _showSectionManagement.value = show }
    fun onShowRestoreConfirmChange(uri: Uri?) { _showRestoreConfirm.value = uri }

    init {
        viewModelScope.launch {
            // L'inizializzazione dei dati dell'app viene gestita ora solo nella MainActivity
            // per evitare race condition durante il primo avvio.
            checkAndPopulateUnits()
        }
    }

    private suspend fun checkAndPopulateUnits() {
        if (measurementUnitDao.countUnits() == 0) {
            AppConstants.INITIAL_MEASUREMENT_UNITS.forEachIndexed { index, unit ->
                measurementUnitDao.insertUnit(unit.copy(displayOrder = index))
            }
        }
    }

    val username: StateFlow<String> = appSettingsManager.username
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), "")

    val allImages: StateFlow<List<Image>> = imageRepository.allImages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), emptyList())

    val categoriesUiState: StateFlow<List<CategoryUiState>> = imageRepository.allCategories
        .flatMapLatest { categories ->
            if (categories.isEmpty()) return@flatMapLatest flowOf(emptyList<CategoryUiState>())
            
            val flows = categories.map { category ->
                combine(
                    imageRepository.getCategoryColor(category.id),
                    imageRepository.getCategoryDefaultIcon(category.id),
                    imageRepository.getCategoryDefaultPhoto(category.id)
                ) { color, icon, photo ->
                    CategoryUiState(category, color ?: UiConstants.DEFAULT_FALLBACK_COLOR, icon, photo)
                }
            }
            combine(flows) { it.toList() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), emptyList())

    val allColors: StateFlow<List<AppColor>> = imageRepository.allColors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), emptyList())

    val reportsColors: StateFlow<List<AppColor>> = imageRepository.allColorsForReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), emptyList())

    val measurementUnits: StateFlow<List<MeasurementUnit>> = measurementUnitDao.getAllUnits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), emptyList())

    val allSections: StateFlow<List<Section>> = sectionRepository.allSections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), emptyList())

    val unitUsageCounts: StateFlow<Map<Int, Int>> = equipmentDao.getAllEquipmentList()
        .map { equipments -> 
            equipments.groupBy { it.unitId }.mapValues { it.value.size }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), emptyMap())

    val sectionUsageCounts: StateFlow<Map<Int, Int>> = equipmentDao.getAllEquipmentList()
        .map { equipments ->
            equipments.groupBy { it.sectionId }.mapValues { it.value.size }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), emptyMap())

    fun addSection(name: String, iconIdentifier: String?, photoUri: String?, color: String?, defaultUnitId: Int = 1) {
        viewModelScope.launch {
            try {
                val currentList = allSections.value
                val nextOrder = if (currentList.isEmpty()) 0 else currentList.maxOf { it.displayOrder } + 1
                sectionRepository.insertSection(Section(
                    name = name, 
                    iconIdentifier = iconIdentifier, 
                    photoUri = photoUri,
                    color = color, 
                    displayOrder = nextOrder,
                    defaultUnitId = defaultUnitId
                ))
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.AddSectionFailed)
            }
        }
    }

    fun updateSection(section: Section) {
        viewModelScope.launch {
            try {
                sectionRepository.updateSection(section)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateSectionFailed)
            }
        }
    }

    fun deleteSection(section: Section) {
        if (section.id == AppConstants.DEFAULT_SECTION_ID) return // Protect default section
        viewModelScope.launch {
            try {
                sectionRepository.deleteSection(section)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.DeleteSectionFailed)
            }
        }
    }

    fun updateSectionsOrder(sections: List<Section>) {
        viewModelScope.launch {
            try {
                sectionRepository.updateSectionList(sections.mapIndexed { index, section ->
                    section.copy(displayOrder = index)
                })
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateSectionsOrderFailed)
            }
        }
    }

    fun cloneSection(section: Section) {
        viewModelScope.launch {
            try {
                val currentList = allSections.value
                val nextOrder = if (currentList.isEmpty()) 0 else currentList.maxOf { it.displayOrder } + 1
                sectionRepository.insertSection(section.copy(
                    id = 0,
                    name = "${section.name} (Copy)",
                    displayOrder = nextOrder
                ))
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.AddSectionFailed)
            }
        }
    }
        
    val defaultUnitId: StateFlow<Int?> = appSettingsManager.defaultUnitId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), null)

    val backgroundUri: StateFlow<String?> = appSettingsManager.backgroundUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), null)

    val backgroundBlur: StateFlow<Float> = appSettingsManager.backgroundBlur
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), UiConstants.DEFAULT_BACKGROUND_BLUR)

    val backgroundSaturation: StateFlow<Float> = appSettingsManager.backgroundSaturation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), UiConstants.DEFAULT_BACKGROUND_SATURATION)

    val backgroundTintEnabled: StateFlow<Boolean> = appSettingsManager.backgroundTintEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), UiConstants.DEFAULT_BACKGROUND_TINT_ENABLED)

    val backgroundTintAlpha: StateFlow<Float> = appSettingsManager.backgroundTintAlpha
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), UiConstants.DEFAULT_BACKGROUND_TINT_ALPHA)

    val backgroundImageAlpha: StateFlow<Float> = appSettingsManager.backgroundImageAlpha
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), UiConstants.DEFAULT_BACKGROUND_IMAGE_ALPHA)

    val reportsColorMode: StateFlow<String> = appSettingsManager.reportsColorMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), UiConstants.DEFAULT_REPORTS_COLOR_MODE)

    val reportsCustomColors: StateFlow<List<String>> = appSettingsManager.reportsCustomColors
        .map { it ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), emptyList())

    val googleAccountName: StateFlow<String?> = appSettingsManager.googleAccountName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), null)

    val costTrendThreshold: StateFlow<Float> = appSettingsManager.costTrendThreshold
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), UiConstants.DEFAULT_COST_TREND_THRESHOLD)

    val syncCalendarByDefault: StateFlow<Boolean> = appSettingsManager.syncCalendarByDefault
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), false)

    val globalUsageWindowValue: StateFlow<Int> = appSettingsManager.defaultUsageWindowValue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), UiConstants.DEFAULT_USAGE_WINDOW_VALUE)

    val globalUsageWindowUnit: StateFlow<String> = appSettingsManager.defaultUsageWindowUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), UiConstants.DEFAULT_USAGE_WINDOW_UNIT)

    val globalVisibilityHorizonValue: StateFlow<Int> = appSettingsManager.defaultVisibilityHorizonValue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), UiConstants.DEFAULT_VISIBILITY_HORIZON_VALUE)

    val globalVisibilityHorizonUnit: StateFlow<String> = appSettingsManager.defaultVisibilityHorizonUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), UiConstants.DEFAULT_VISIBILITY_HORIZON_UNIT)

    val sectionSelectorType: StateFlow<String> = appSettingsManager.sectionSelectorType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), UiConstants.DEFAULT_SECTION_SELECTOR_TYPE)

    val costAnalysisWindowValue: StateFlow<Int> = appSettingsManager.costAnalysisWindowValue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), UiConstants.DEFAULT_COST_ANALYSIS_WINDOW_VALUE)

    val costAnalysisWindowUnit: StateFlow<String> = appSettingsManager.costAnalysisWindowUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(AppConstants.FLOW_STOP_TIMEOUT), UiConstants.DEFAULT_COST_ANALYSIS_WINDOW_UNIT)

    fun resetBackgroundSettings() {
        viewModelScope.launch {
            try {
                appSettingsManager.setBackgroundBlur(UiConstants.DEFAULT_BACKGROUND_BLUR)
                appSettingsManager.setBackgroundSaturation(UiConstants.DEFAULT_BACKGROUND_SATURATION)
                appSettingsManager.setBackgroundTintEnabled(UiConstants.DEFAULT_BACKGROUND_TINT_ENABLED)
                appSettingsManager.setBackgroundTintAlpha(UiConstants.DEFAULT_BACKGROUND_TINT_ALPHA)
                appSettingsManager.setBackgroundImageAlpha(UiConstants.DEFAULT_BACKGROUND_IMAGE_ALPHA)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateBackgroundFailed)
            }
        }
    }

    fun setBackgroundUri(uri: String?) {
        viewModelScope.launch {
            try {
                appSettingsManager.setBackgroundUri(uri)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateBackgroundFailed)
            }
        }
    }

    fun setBackgroundBlur(blur: Float) {
        viewModelScope.launch {
            try {
                appSettingsManager.setBackgroundBlur(blur)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateBackgroundFailed)
            }
        }
    }

    fun setBackgroundSaturation(saturation: Float) {
        viewModelScope.launch {
            try {
                appSettingsManager.setBackgroundSaturation(saturation)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateBackgroundFailed)
            }
        }
    }

    fun setBackgroundTintEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                appSettingsManager.setBackgroundTintEnabled(enabled)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateBackgroundFailed)
            }
        }
    }

    fun setBackgroundTintAlpha(alpha: Float) {
        viewModelScope.launch {
            try {
                appSettingsManager.setBackgroundTintAlpha(alpha)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateBackgroundFailed)
            }
        }
    }

    fun setBackgroundImageAlpha(alpha: Float) {
        viewModelScope.launch {
            try {
                appSettingsManager.setBackgroundImageAlpha(alpha)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateBackgroundFailed)
            }
        }
    }

    fun setReportsColorMode(mode: String) {
        viewModelScope.launch {
            try {
                appSettingsManager.setReportsColorMode(mode)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateReportsSettingsFailed)
            }
        }
    }

    fun setReportsCustomColors(colors: List<String>) {
        viewModelScope.launch {
            try {
                appSettingsManager.setReportsCustomColors(colors)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateReportsSettingsFailed)
            }
        }
    }

    fun setUsername(newUsername: String) {
        if (newUsername.isBlank() || newUsername.length > AppConstants.USERNAME_MAX_LENGTH) {
            _uiEvents.trySend(OptionsUiEvent.UsernameInvalid)
            return
        }
        viewModelScope.launch {
            try {
                appSettingsManager.setUsername(newUsername)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateUsernameFailed)
            }
        }
    }

    fun setGoogleAccountName(name: String?) {
        viewModelScope.launch {
            try {
                appSettingsManager.setGoogleAccountName(name)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateGoogleAccountFailed)
            }
        }
    }

    fun setSyncCalendarByDefault(enabled: Boolean) {
        viewModelScope.launch {
            try {
                appSettingsManager.setSyncCalendarByDefault(enabled)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateReportsSettingsFailed)
            }
        }
    }

    fun setSectionSelectorType(type: String) {
        viewModelScope.launch {
            try {
                appSettingsManager.setSectionSelectorType(type)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateSettingsFailed)
            }
        }
    }

    fun setGlobalUsageWindow(value: Int, unit: String) {
        viewModelScope.launch {
            try {
                appSettingsManager.setDefaultUsageWindowValue(value)
                appSettingsManager.setDefaultUsageWindowUnit(unit)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateSettingsFailed)
            }
        }
    }

    fun setGlobalVisibilityHorizon(value: Int, unit: String) {
        viewModelScope.launch {
            try {
                appSettingsManager.setDefaultVisibilityHorizonValue(value)
                appSettingsManager.setDefaultVisibilityHorizonUnit(unit)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateSettingsFailed)
            }
        }
    }

    fun setCostAnalysisWindow(value: Int, unit: String) {
        viewModelScope.launch {
            try {
                appSettingsManager.setCostAnalysisWindowValue(value)
                appSettingsManager.setCostAnalysisWindowUnit(unit)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateSettingsFailed)
            }
        }
    }

    fun setCostTrendThreshold(threshold: Float) {
        viewModelScope.launch {
            try {
                appSettingsManager.setCostTrendThreshold(threshold)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateSettingsFailed)
            }
        }
    }

    fun setCategoryDefault(categoryId: String, imageIdentifier: ImageIdentifier?) {
        if (categoryId.isBlank()) {
            _uiEvents.trySend(OptionsUiEvent.CategoryIdInvalid)
            return
        }
        viewModelScope.launch {
            try {
                imageRepository.setCategoryDefault(categoryId, imageIdentifier)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.SetCategoryDefaultFailed)
            }
        }
    }

    fun toggleImageVisibility(image: Image) {
        viewModelScope.launch {
            try {
                imageRepository.toggleImageVisibility(image)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.ToggleImageVisibilityFailed)
            }
        }
    }

    fun addImage(imageIdentifier: ImageIdentifier, category: String) {
        if (category.isBlank()) {
            _uiEvents.trySend(OptionsUiEvent.CategoryIdInvalid)
            return
        }
        viewModelScope.launch {
            try {
                imageRepository.addImage(imageIdentifier, category)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.AddImageFailed)
            }
        }
    }

    fun removeImage(image: Image) {
        viewModelScope.launch {
            try {
                imageRepository.removeImage(image)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.RemoveImageFailed)
            }
        }
    }

    fun updateImageOrder(imageList: List<Image>) {
        if (imageList.isEmpty()) return
        viewModelScope.launch {
            try {
                imageRepository.updateImageOrder(imageList)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateImageOrderFailed)
            }
        }
    }

    fun updateCategoryColor(categoryId: String, colorHex: String) {
        if (categoryId.isBlank()) {
            _uiEvents.trySend(OptionsUiEvent.CategoryIdInvalid)
            return
        }
        if (colorHex.isBlank()) {
            _uiEvents.trySend(OptionsUiEvent.ColorHexInvalid)
            return
        }
        viewModelScope.launch {
            try {
                imageRepository.updateCategoryColor(categoryId, colorHex)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateCategoryColorFailed)
            }
        }
    }

    fun addColor(hex: String, name: String) {
        if (name.isBlank()) {
            _uiEvents.trySend(OptionsUiEvent.ColorNameInvalid)
            return
        }
        if (hex.isBlank()) {
            _uiEvents.trySend(OptionsUiEvent.ColorHexInvalid)
            return
        }
        viewModelScope.launch {
            try {
                imageRepository.addColor(hex, name)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.AddColorFailed(name))
            }
        }
    }

    fun updateColor(color: AppColor) {
        if (color.name.isBlank()) {
            _uiEvents.trySend(OptionsUiEvent.ColorNameInvalid)
            return
        }
        viewModelScope.launch {
            try {
                imageRepository.updateColor(color)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateColorFailed)
            }
        }
    }

    fun updateColorsOrder(colors: List<AppColor>) {
        if (colors.isEmpty()) return
        viewModelScope.launch {
            try {
                imageRepository.updateColorsOrder(colors)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateColorsOrderFailed)
            }
        }
    }

    fun updateReportColorsOrder(colors: List<AppColor>) {
        if (colors.isEmpty()) return
        viewModelScope.launch {
            try {
                // Aggiorniamo l'ordine dei report mappando la lista sulla colonna reportOrder
                val updatedColors = colors.mapIndexed { index, appColor ->
                    appColor.copy(reportOrder = index)
                }
                imageRepository.updateColorsOrder(updatedColors)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateColorsOrderFailed)
            }
        }
    }

    fun toggleColorVisibility(id: Long) {
        if (id == 0L) {
            _uiEvents.trySend(OptionsUiEvent.ColorIdInvalid)
            return
        }
        viewModelScope.launch {
            try {
                imageRepository.toggleColorVisibility(id)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.ToggleColorVisibilityFailed)
            }
        }
    }

    fun toggleReportColorVisibility(id: Long) {
        if (id == 0L) {
            _uiEvents.trySend(OptionsUiEvent.ColorIdInvalid)
            return
        }
        viewModelScope.launch {
            try {
                // Vincolo: Almeno un colore deve rimanere visibile per i report
                val currentVisibleCount = reportsColors.value.count { !it.reportHidden }
                val targetColor = reportsColors.value.find { it.id == id }
                
                if (currentVisibleCount <= 1 && targetColor != null && !targetColor.reportHidden) {
                    // Stiamo cercando di nascondere l'ultimo visibile: lo impediamo
                    _uiEvents.send(OptionsUiEvent.UpdateReportsSettingsFailed)
                    return@launch
                }

                imageRepository.toggleReportColorVisibility(id)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.ToggleColorVisibilityFailed)
            }
        }
    }

    fun deleteColor(color: AppColor) {
        if (color.id == 0L) {
            _uiEvents.trySend(OptionsUiEvent.ColorIdInvalid)
            return
        }
        viewModelScope.launch {
            try {
                imageRepository.deleteColor(color)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.DeleteColorFailed)
            }
        }
    }

    fun addMeasurementUnit(label: String, description: String, decimalPlaces: Int = 0) {
        if (label.isBlank() || label.length > AppConstants.UNIT_LABEL_MAX_LENGTH) return
        
        val normalizedLabel = label.trim().lowercase()
        if (measurementUnits.value.any { it.label.lowercase() == normalizedLabel }) {
            viewModelScope.launch { _uiEvents.send(OptionsUiEvent.AddUnitFailedDuplicate) }
            return
        }

        viewModelScope.launch {
            try {
                val maxOrder = measurementUnits.value.maxOfOrNull { it.displayOrder } ?: -1
                measurementUnitDao.insertUnit(MeasurementUnit(
                    label = normalizedLabel,
                    description = description, 
                    displayOrder = maxOrder + 1,
                    decimalPlaces = decimalPlaces.coerceIn(0, AppConstants.MAX_DECIMAL_PLACES)
                ))
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.AddUnitFailed)
            }
        }
    }

    fun updateMeasurementUnit(unit: MeasurementUnit) {
        viewModelScope.launch {
            try {
                measurementUnitDao.updateUnit(unit)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateUnitFailed)
            }
        }
    }

    fun toggleMeasurementUnitVisibility(id: Int) {
        viewModelScope.launch {
            try {
                measurementUnitDao.getUnitById(id)?.let { unit ->
                    measurementUnitDao.updateUnit(unit.copy(isHidden = !unit.isHidden))
                }
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.ToggleUnitVisibilityFailed)
            }
        }
    }

    fun updateMeasurementUnitsOrder(units: List<MeasurementUnit>) {
        viewModelScope.launch {
            try {
                units.forEachIndexed { index, unit ->
                    measurementUnitDao.updateUnit(unit.copy(displayOrder = index))
                }
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateUnitsOrderFailed)
            }
        }
    }

    fun deleteMeasurementUnit(unit: MeasurementUnit) {
        if (unit.isSystem) return
        viewModelScope.launch {
            try {
                measurementUnitDao.deleteUnit(unit)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.DeleteUnitFailed)
            }
        }
    }

    fun cloneMeasurementUnit(unit: MeasurementUnit) {
        viewModelScope.launch {
            try {
                val currentList = measurementUnits.value
                val nextOrder = if (currentList.isEmpty()) 0 else currentList.maxOf { it.displayOrder } + 1
                measurementUnitDao.insertUnit(unit.copy(
                    id = 0,
                    label = "${unit.label}_copy",
                    description = "${unit.description} (Copy)",
                    displayOrder = nextOrder,
                    isSystem = false
                ))
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.AddUnitFailed)
            }
        }
    }
    
    fun toggleDefaultUnit(id: Int) {
        viewModelScope.launch {
            try {
                val currentDefault = defaultUnitId.value
                if (currentDefault == id) {
                    appSettingsManager.setDefaultUnitId(null)
                } else {
                    appSettingsManager.setDefaultUnitId(id)
                }
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.SetDefaultFailed)
            }
        }
    }

    fun backupDatabase(uri: Uri) {
        viewModelScope.launch {
            backupManager.backupDatabase(uri).fold(
                onSuccess = { _uiEvents.send(OptionsUiEvent.BackupResult(true, null)) },
                onFailure = { _uiEvents.send(OptionsUiEvent.BackupResult(false, it.message)) }
            )
        }
    }

    fun restoreDatabase(uri: Uri) {
        viewModelScope.launch {
            backupManager.restoreDatabase(uri).fold(
                onSuccess = { _uiEvents.send(OptionsUiEvent.RestoreResult(true, null)) },
                onFailure = { _uiEvents.send(OptionsUiEvent.RestoreResult(false, it.message)) }
            )
        }
    }

    fun getSuggestedBackupFileName(): String = backupManager.getSuggestedBackupFileName()

    fun totalExport(uri: Uri) {
        viewModelScope.launch {
            backupManager.exportAllToZip(uri).fold(
                onSuccess = { _uiEvents.send(OptionsUiEvent.TotalExportResult(true, null)) },
                onFailure = { _uiEvents.send(OptionsUiEvent.TotalExportResult(false, it.message)) }
            )
        }
    }

    fun recalculateAllAccumulatedValues() {
        viewModelScope.launch {
            try {
                maintenanceManager.recalculateAllAccumulatedValues()
                _uiEvents.send(OptionsUiEvent.RecalculateSuccess)
            } catch (e: Exception) {
                // Silently fail or handle
            }
        }
    }

    fun getSuggestedTotalExportFileName(): String = backupManager.getSuggestedTotalExportFileName()

    fun generateDemoData(scenario: DemoScenario = DemoScenario.ALL) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val monthMs = 30 * AppConstants.MS_PER_DAY
                val logs = mutableListOf<MaintenanceLog>()

                // Helper to generate logs
                fun addHistory(equipmentId: Int, operationTypeId: Int, startValue: Double, monthlyIncrement: Double, costPerMonth: Double, months: Int = 12) {
                    var currentVal = startValue
                    for (i in months downTo 0) {
                        val date = now - (i * monthMs) + (Math.random() * AppConstants.MS_PER_DAY).toLong()
                        if (date > now) continue
                        currentVal += monthlyIncrement + (Math.random() * (monthlyIncrement * 0.2))
                        logs.add(MaintenanceLog(
                            equipmentId = equipmentId, 
                            operationTypeId = operationTypeId, 
                            value = currentVal, 
                            date = date, 
                            cost = costPerMonth + (Math.random() * (costPerMonth * 0.1))
                        ))
                    }
                }

                // --- CARS SCENARIO ---
                if (scenario == DemoScenario.CARS || scenario == DemoScenario.ALL) {
                    val vehicleSectionId = sectionRepository.insertSection(Section(
                        name = "Cars (Demo)",
                        iconIdentifier = "directions_car",
                        color = "#FF4285F4",
                        defaultUnitId = 1 // km
                    )).toInt()

                    val pandaId = equipmentDao.insertEquipment(Equipment(
                        description = "Fiat Panda (Demo)",
                        unitId = 1,
                        sectionId = vehicleSectionId,
                        color = "#FF4285F4",
                        estimatedCostPerUnit = 0.15
                    )).toInt()

                    val golfId = equipmentDao.insertEquipment(Equipment(
                        description = "VW Golf (Demo)",
                        unitId = 1,
                        sectionId = vehicleSectionId,
                        color = "#FF34A853",
                        estimatedCostPerUnit = 0.22
                    )).toInt()

                    val fuelOpId = operationTypeDao.insertOperationType(OperationType(
                        description = "Refuel (Demo)",
                        color = "#FF34A853",
                        iconIdentifier = "local_gas_station",
                        sectionId = vehicleSectionId,
                        unitId = 1 // km
                    )).toInt()

                    val serviceOpId = operationTypeDao.insertOperationType(OperationType(
                        description = "Service (Demo)",
                        color = "#FFFBBC05",
                        iconIdentifier = "build",
                        estimatedCost = 250.0,
                        isPredictable = true,
                        intervalValue = 15000.0,
                        sectionId = vehicleSectionId,
                        unitId = 1 // km
                    )).toInt()

                    addHistory(pandaId, fuelOpId, 10000.0, 800.0, 60.0)
                    addHistory(golfId, fuelOpId, 25000.0, 1200.0, 90.0)

                    // Baseline service
                    logs.add(MaintenanceLog(equipmentId = pandaId, operationTypeId = serviceOpId, value = 5000.0, date = now - (10 * monthMs), cost = 220.0))
                    logs.add(MaintenanceLog(equipmentId = golfId, operationTypeId = serviceOpId, value = 20000.0, date = now - (8 * monthMs), cost = 310.0))
                }

                // --- GARDEN SCENARIO ---
                if (scenario == DemoScenario.GARDEN || scenario == DemoScenario.ALL) {
                    val gardenSectionId = sectionRepository.insertSection(Section(
                        name = "Garden (Demo)",
                        iconIdentifier = "grass",
                        color = "#FF34A853",
                        defaultUnitId = 2 // hh
                    )).toInt()

                    val mowerId = equipmentDao.insertEquipment(Equipment(
                        description = "Lawn Mower (Demo)",
                        unitId = 2,
                        sectionId = gardenSectionId,
                        color = "#FF34A853"
                    )).toInt()

                    val lemonId = equipmentDao.insertEquipment(Equipment(
                        description = "Lemon Tree (Demo)",
                        unitId = 3, // dy
                        sectionId = gardenSectionId,
                        color = "#FFFBC02D"
                    )).toInt()

                    val bladeOpId = operationTypeDao.insertOperationType(OperationType(
                        description = "Sharpen Blades (Demo)",
                        color = "#FF795548",
                        iconIdentifier = "content_cut",
                        isPredictable = true,
                        intervalValue = 50.0,
                        unitId = 2,
                        sectionId = gardenSectionId
                    )).toInt()

                    val waterOpId = operationTypeDao.insertOperationType(OperationType(
                        description = "Fertilize (Demo)",
                        color = "#FF2196F3",
                        iconIdentifier = "opacity",
                        isPredictable = true,
                        timeoutValue = 3,
                        timeoutUnit = TimeGranularity.MONTHS,
                        sectionId = gardenSectionId,
                        unitId = 3 // dy
                    )).toInt()

                    addHistory(mowerId, bladeOpId, 0.0, 5.0, 0.0, months = 6)
                    addHistory(lemonId, waterOpId, 100.0, 30.0, 5.0, months = 12)
                }

                // --- HEALTH SCENARIO ---
                if (scenario == DemoScenario.HEALTH || scenario == DemoScenario.ALL) {
                    val healthSectionId = sectionRepository.insertSection(Section(
                        name = "Health (Demo)",
                        iconIdentifier = "monitor_heart",
                        color = "#FFE91E63",
                        defaultUnitId = 4 // un
                    )).toInt()

                    val pumpId = equipmentDao.insertEquipment(Equipment(
                        description = "Insulin Pump (Demo)",
                        unitId = 3, // 4 un (Change to 2 for hh, 3 for dy)
                        sectionId = healthSectionId,
                        color = "#FFE91E63"
                    )).toInt()

                    val infusionOpId = operationTypeDao.insertOperationType(OperationType(
                        description = "Infusion Set Change (Demo)",
                        color = "#FF00BCD4",
                        iconIdentifier = "refresh",
                        isPredictable = true,
                        timeoutValue = 3,
                        timeoutUnit = TimeGranularity.DAYS,
                        sectionId = healthSectionId,
                        unitId = 3, // dy
                        hasValue = false
                    )).toInt()

                    val sensorOpId = operationTypeDao.insertOperationType(OperationType(
                        description = "Sensor Change (Demo)",
                        color = "#FF9C27B0",
                        iconIdentifier = "sensors",
                        isPredictable = true,
                        timeoutValue = 10,
                        timeoutUnit = TimeGranularity.DAYS,
                        sectionId = healthSectionId,
                        unitId = 3, // dy
                        hasValue = false
                    )).toInt()

                    // Recent history for health
                    for (i in 30 downTo 0 step 3) {
                        logs.add(MaintenanceLog(equipmentId = pumpId, operationTypeId = infusionOpId, value = (30 - i).toDouble(), date = now - (i * AppConstants.MS_PER_DAY)))
                    }
                    for (i in 30 downTo 0 step 10) {
                        logs.add(MaintenanceLog(equipmentId = pumpId, operationTypeId = sensorOpId, value = (30 - i).toDouble(), date = now - (i * AppConstants.MS_PER_DAY)))
                    }
                }

                // --- BIKES SCENARIO ---
                if (scenario == DemoScenario.BIKES || scenario == DemoScenario.ALL) {
                    val bikesSectionId = sectionRepository.insertSection(Section(
                        name = "Bikes (Demo)",
                        iconIdentifier = "directions_bike",
                        color = "#FF673AB7",
                        defaultUnitId = 1 // km
                    )).toInt()

                    val mtbId = equipmentDao.insertEquipment(Equipment(description = "MTB (Demo)", unitId = 1, sectionId = bikesSectionId, color = "#FF795548")).toInt()
                    val roadId = equipmentDao.insertEquipment(Equipment(description = "Road Bike (Demo)", unitId = 1, sectionId = bikesSectionId, color = "#FF2196F3")).toInt()
                    val gravelId = equipmentDao.insertEquipment(Equipment(description = "Gravel Bike (Demo)", unitId = 1, sectionId = bikesSectionId, color = "#FF4CAF50")).toInt()

                    val chainOpId = operationTypeDao.insertOperationType(OperationType(
                        description = "Lube Chain (Demo)",
                        color = "#FF607D8B",
                        iconIdentifier = "opacity",
                        isPredictable = true,
                        intervalValue = 200.0,
                        sectionId = bikesSectionId,
                        unitId = 1 // km
                    )).toInt()

                    addHistory(mtbId, chainOpId, 500.0, 50.0, 2.0)
                    addHistory(roadId, chainOpId, 2000.0, 300.0, 2.0)
                    addHistory(gravelId, chainOpId, 1000.0, 150.0, 2.0)
                }

                maintenanceLogDao.insertLogs(logs.sortedBy { it.date })

                // Recalculate all affected equipments
                val equipmentIds = logs.map { it.equipmentId }.distinct()
                equipmentIds.forEach { id ->
                    maintenanceManager.recalculateAccumulatedValues(id)
                }

                // Switch to "All" section so the user can immediately see the new data
                appSettingsManager.setSelectedSectionId(AppConstants.ALL_SECTIONS_ID)

                _uiEvents.send(OptionsUiEvent.DemoDataGenerated)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateSettingsFailed)
            }
        }
    }

    fun deleteDemoData() {
        viewModelScope.launch {
            try {
                val demoSections = sectionRepository.getDemoSections()
                if (demoSections.isNotEmpty()) {
                    sectionRepository.deleteSections(demoSections)
                }

                val demoEquips = equipmentDao.getDemoEquipmentList()
                if (demoEquips.isNotEmpty()) {
                    equipmentDao.deleteEquipmentList(demoEquips)
                }
                
                val demoOps = operationTypeDao.getDemoOperationTypes()
                if (demoOps.isNotEmpty()) {
                    operationTypeDao.deleteOperationTypes(demoOps)
                }
                
                _uiEvents.send(OptionsUiEvent.DemoDataDeleted)
            } catch (e: Exception) {
                _uiEvents.send(OptionsUiEvent.UpdateSettingsFailed)
            }
        }
    }

    suspend fun isPhotoUsed(uri: String): Boolean {
        if (uri.isBlank()) {
            _uiEvents.trySend(OptionsUiEvent.PhotoUriInvalid)
            return true
        }
        return try {
            maintenanceManager.isPhotoUsed(uri)
        } catch (e: Exception) {
            _uiEvents.trySend(OptionsUiEvent.DatabaseCheckFailed)
            true
        }
    }
}
