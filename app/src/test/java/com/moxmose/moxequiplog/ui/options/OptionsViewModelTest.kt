package com.moxmose.moxequiplog.ui.options

import androidx.compose.ui.test.junit4.createComposeRule
import app.cash.turbine.test
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.ImageRepository
import com.moxmose.moxequiplog.data.local.*
import com.moxmose.moxequiplog.utils.UiConstants
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class OptionsViewModelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var appSettingsManager: AppSettingsManager
    private lateinit var equipmentDao: EquipmentDao
    private lateinit var imageRepository: ImageRepository
    private lateinit var measurementUnitDao: MeasurementUnitDao
    private lateinit var viewModel: OptionsViewModel

    private val allCategoriesFlow = MutableStateFlow<List<Category>>(emptyList())
    private val allImagesFlow = MutableStateFlow<List<Image>>(emptyList())
    private val allColorsFlow = MutableStateFlow<List<AppColor>>(emptyList())
    
    private val backgroundUriFlow = MutableStateFlow<String?>(null)
    private val backgroundBlurFlow = MutableStateFlow(0f)
    private val backgroundSaturationFlow = MutableStateFlow(1f)
    private val backgroundTintEnabledFlow = MutableStateFlow(false)
    private val backgroundTintAlphaFlow = MutableStateFlow(0.25f)
    private val backgroundImageAlphaFlow = MutableStateFlow(1f)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        appSettingsManager = mockk(relaxed = true) {
            every { username } returns MutableStateFlow("default_user")
            every { backgroundUri } returns backgroundUriFlow
            every { backgroundBlur } returns backgroundBlurFlow
            every { backgroundSaturation } returns backgroundSaturationFlow
            every { backgroundTintEnabled } returns backgroundTintEnabledFlow
            every { backgroundTintAlpha } returns backgroundTintAlphaFlow
            every { backgroundImageAlpha } returns backgroundImageAlphaFlow
            every { defaultUnitId } returns MutableStateFlow(null)
            every { reportsColorMode } returns MutableStateFlow(UiConstants.DEFAULT_REPORTS_COLOR_MODE)
            every { reportsCustomColors } returns MutableStateFlow(null)
        }
        equipmentDao = mockk(relaxed = true)
        
        measurementUnitDao = mockk(relaxed = true)
        every { measurementUnitDao.getAllUnits() } returns MutableStateFlow(emptyList())
        coEvery { measurementUnitDao.countUnits() } returns 1 // Avoid auto-population during test
        
        imageRepository = mockk(relaxed = true) {
            every { allImages } returns allImagesFlow
            every { allCategories } returns allCategoriesFlow
            every { allColors } returns allColorsFlow
            every { allColorsForReports } returns allColorsFlow
            every { getCategoryColor(any()) } returns MutableStateFlow(UiConstants.DEFAULT_FALLBACK_COLOR)
            every { getCategoryDefaultIcon(any()) } returns MutableStateFlow(null)
            every { getCategoryDefaultPhoto(any()) } returns MutableStateFlow(null)
        }
        
        viewModel = OptionsViewModel(appSettingsManager, equipmentDao, imageRepository, measurementUnitDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun categoriesUiState_withData_emitsCorrectState() = runTest {
        val category = Category("CAT1", "Category 1")
        
        every { imageRepository.getCategoryColor("CAT1") } returns MutableStateFlow("#FF0000")
        every { imageRepository.getCategoryDefaultIcon("CAT1") } returns MutableStateFlow("icon1")
        every { imageRepository.getCategoryDefaultPhoto("CAT1") } returns MutableStateFlow("photo1")

        viewModel.categoriesUiState.test {
            assertEquals(emptyList<CategoryUiState>(), awaitItem())

            allCategoriesFlow.value = listOf(category)

            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("CAT1", result[0].category.id)
            assertEquals("#FF0000", result[0].color)
            assertEquals("icon1", result[0].defaultIconIdentifier)
            assertEquals("photo1", result[0].defaultPhotoUri)
            
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setBackgroundUri_callsManager() = runTest {
        val uri = "content://image"
        viewModel.setBackgroundUri(uri)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { appSettingsManager.setBackgroundUri(uri) }
    }

    @Test
    fun setBackgroundBlur_callsManager() = runTest {
        val blur = 10f
        viewModel.setBackgroundBlur(blur)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { appSettingsManager.setBackgroundBlur(blur) }
    }

    @Test
    fun setBackgroundSaturation_callsManager() = runTest {
        val saturation = 1.5f
        viewModel.setBackgroundSaturation(saturation)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { appSettingsManager.setBackgroundSaturation(saturation) }
    }

    @Test
    fun setBackgroundTintEnabled_callsManager() = runTest {
        viewModel.setBackgroundTintEnabled(true)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { appSettingsManager.setBackgroundTintEnabled(true) }
    }

    @Test
    fun setBackgroundTintAlpha_callsManager() = runTest {
        val alpha = 0.5f
        viewModel.setBackgroundTintAlpha(alpha)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { appSettingsManager.setBackgroundTintAlpha(alpha) }
    }

    @Test
    fun setBackgroundImageAlpha_callsManager() = runTest {
        val alpha = 0.8f
        viewModel.setBackgroundImageAlpha(alpha)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { appSettingsManager.setBackgroundImageAlpha(alpha) }
    }

    @Test
    fun resetBackgroundSettings_callsManagerWithDefaults() = runTest {
        viewModel.resetBackgroundSettings()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { appSettingsManager.setBackgroundBlur(UiConstants.DEFAULT_BACKGROUND_BLUR) }
        coVerify { appSettingsManager.setBackgroundSaturation(UiConstants.DEFAULT_BACKGROUND_SATURATION) }
        coVerify { appSettingsManager.setBackgroundTintEnabled(UiConstants.DEFAULT_BACKGROUND_TINT_ENABLED) }
        coVerify { appSettingsManager.setBackgroundTintAlpha(UiConstants.DEFAULT_BACKGROUND_TINT_ALPHA) }
        coVerify { appSettingsManager.setBackgroundImageAlpha(UiConstants.DEFAULT_BACKGROUND_IMAGE_ALPHA) }
    }

    @Test
    fun backgroundSettings_emitCorrectValues() = runTest {
        viewModel.backgroundUri.test {
            assertEquals(null, awaitItem())
            backgroundUriFlow.value = "test_uri"
            assertEquals("test_uri", awaitItem())
        }
        
        viewModel.backgroundBlur.test {
            assertEquals(UiConstants.DEFAULT_BACKGROUND_BLUR, awaitItem())
            backgroundBlurFlow.value = 5f
            assertEquals(5f, awaitItem())
        }
    }

    @Test
    fun username_onInit_isDefault() = runTest {
        viewModel.username.test {
            assertEquals("", awaitItem())
            assertEquals("default_user", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setUsername_withValidUsername_callsManager() = runTest {
        val newUsername = "testuser"
        coEvery { appSettingsManager.setUsername(newUsername) } returns Unit

        viewModel.setUsername(newUsername)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { appSettingsManager.setUsername(newUsername) }
    }

    @Test
    fun setUsername_withBlankUsername_sendsUsernameInvalidEvent() = runTest {
        viewModel.uiEvents.test {
            viewModel.setUsername(" ")
            assertEquals(OptionsViewModel.OptionsUiEvent.UsernameInvalid, awaitItem())
        }
    }
    
    @Test
    fun setCategoryDefault_withValidData_callsRepository() = runTest {
        val categoryId = "test_category"
        val identifier = ImageIdentifier.Icon("test_icon")

        viewModel.setCategoryDefault(categoryId, identifier)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { imageRepository.setCategoryDefault(categoryId, identifier) }
    }

    @Test
    fun setCategoryDefault_withNullImageIdentifier_callsRepositoryWithNull() = runTest {
        val categoryId = "test_category"
        viewModel.setCategoryDefault(categoryId, null)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { imageRepository.setCategoryDefault(categoryId, null) }
    }
    
    @Test
    fun addImage_withValidData_callsRepository() = runTest {
        val identifier = ImageIdentifier.Photo("test_uri")
        val category = "test_category"

        viewModel.addImage(identifier, category)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { imageRepository.addImage(identifier, category) }
    }

    @Test
    fun toggleImageVisibility_callsRepository() = runTest {
        val image: Image = mockk()
        viewModel.toggleImageVisibility(image)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { imageRepository.toggleImageVisibility(image) }
    }

    @Test
    fun removeImage_callsRepository() = runTest {
        val image: Image = mockk()
        viewModel.removeImage(image)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { imageRepository.removeImage(image) }
    }

    @Test
    fun updateImageOrder_withValidList_callsRepository() = runTest {
        val images = listOf<Image>(mockk())
        viewModel.updateImageOrder(images)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { imageRepository.updateImageOrder(images) }
    }

    @Test
    fun updateCategoryColor_withValidData_callsRepository() = runTest {
        val categoryId = "any_valid_category_id"
        val colorHex = "#00FFBB"
        viewModel.updateCategoryColor(categoryId, colorHex)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { imageRepository.updateCategoryColor(categoryId, colorHex) }
    }

    @Test
    fun addColor_withValidData_callsRepository() = runTest {
        val hex = "#00FFBB"
        val name = "ColorName"
        viewModel.addColor(hex, name)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { imageRepository.addColor(hex, name) }
    }

    @Test
    fun updateColor_withValidData_callsRepository() = runTest {
        val color = AppColor(1L, "#FF0000", "Red", false, 0, false)
        viewModel.updateColor(color)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { imageRepository.updateColor(color) }
    }

    @Test
    fun updateColorsOrder_withValidList_callsRepository() = runTest {
        val colors = listOf<AppColor>(mockk())
        viewModel.updateColorsOrder(colors)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { imageRepository.updateColorsOrder(colors) }
    }

    @Test
    fun toggleColorVisibility_withValidId_callsRepository() = runTest {
        viewModel.toggleColorVisibility(1L)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { imageRepository.toggleColorVisibility(1L) }
    }

    @Test
    fun deleteColor_withValidData_callsRepository() = runTest {
        val color: AppColor = mockk(relaxed = true) {
            every { id } returns 1L
        }
        viewModel.deleteColor(color)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { imageRepository.deleteColor(color) }
    }

    @Test
    fun isPhotoUsed_whenPhotoIsInUse_returnsTrue() = runTest {
        val uri = "test_uri"
        coEvery { equipmentDao.countEquipmentsUsingPhoto(uri) } returns 1
        val result = viewModel.isPhotoUsed(uri)
        assertTrue(result)
    }
    
    @Test
    fun isPhotoUsed_whenPhotoIsNotInUse_returnsFalse() = runTest {
        val uri = "test_uri"
        coEvery { equipmentDao.countEquipmentsUsingPhoto(uri) } returns 0
        val result = viewModel.isPhotoUsed(uri)
        assertFalse(result)
    }

    @Test
    fun allImages_emitsDataFromRepository() = runTest {
        viewModel.allImages.test {
            assertEquals(emptyList<Image>(), awaitItem())
            val list = listOf<Image>(mockk())
            allImagesFlow.value = list
            assertEquals(list, awaitItem())
        }
    }

    @Test
    fun allColors_emitsDataFromRepository() = runTest {
        viewModel.allColors.test {
            assertEquals(emptyList<AppColor>(), awaitItem())
            val list = listOf<AppColor>(mockk())
            allColorsFlow.value = list
            assertEquals(list, awaitItem())
        }
    }
}
