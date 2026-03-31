package com.moxmose.moxequiplog.ui.options

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.ImageRepository
import com.moxmose.moxequiplog.data.local.*
import com.moxmose.moxequiplog.utils.AppConstants
import com.moxmose.moxequiplog.utils.UiConstants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CategoryUiState(
    val category: Category,
    val color: String,
    val defaultIconIdentifier: String?,
    val defaultPhotoUri: String?
)

@OptIn(ExperimentalCoroutinesApi::class)
class OptionsViewModel(
    private val appSettingsManager: AppSettingsManager,
    private val equipmentDao: EquipmentDao,
    private val imageRepository: ImageRepository,
    private val measurementUnitDao: MeasurementUnitDao
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
        data object DeleteUnitFailed : OptionsUiEvent()
        data object UpdateUnitFailed : OptionsUiEvent()
        data object UpdateUnitsOrderFailed : OptionsUiEvent()
        data object ToggleUnitVisibilityFailed : OptionsUiEvent()
        data object SetDefaultFailed : OptionsUiEvent()
        data object UpdateReportsSettingsFailed : OptionsUiEvent()
    }

    private val _uiEvents = Channel<OptionsUiEvent>(Channel.BUFFERED)
    val uiEvents: Flow<OptionsUiEvent> = _uiEvents.receiveAsFlow()

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

    fun addMeasurementUnit(label: String, description: String) {
        if (label.isBlank() || label.length > AppConstants.UNIT_LABEL_MAX_LENGTH) return
        viewModelScope.launch {
            try {
                val maxOrder = measurementUnits.value.maxOfOrNull { it.displayOrder } ?: -1
                measurementUnitDao.insertUnit(MeasurementUnit(label = label, description = description, displayOrder = maxOrder + 1))
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

    suspend fun isPhotoUsed(uri: String): Boolean {
        if (uri.isBlank()) {
            _uiEvents.trySend(OptionsUiEvent.PhotoUriInvalid)
            return true
        }
        return try {
            equipmentDao.countEquipmentsUsingPhoto(uri) > 0
        } catch (e: Exception) {
            _uiEvents.trySend(OptionsUiEvent.DatabaseCheckFailed)
            true
        }
    }
}
