package com.moxmose.moxequiplog.ui.options

import androidx.compose.ui.test.junit4.createComposeRule
import app.cash.turbine.test
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.ImageRepository
import com.moxmose.moxequiplog.data.local.AppColor
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.EquipmentDao
import com.moxmose.moxequiplog.data.local.Image
import com.moxmose.moxequiplog.data.local.ImageIdentifier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
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
    private lateinit var viewModel: OptionsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        appSettingsManager = mockk(relaxed = true) {
            every { username } returns MutableStateFlow("default_user")
        }
        equipmentDao = mockk(relaxed = true)
        imageRepository = mockk(relaxed = true)
        viewModel = OptionsViewModel(appSettingsManager, equipmentDao, imageRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun onInit_initializesAppData() = runTest {
        coVerify { imageRepository.initializeAppData() }
    }

    @Test
    fun username_onInit_isDefault() = runTest {
        viewModel.username.test {
            testDispatcher.scheduler.advanceUntilIdle() // Ensure all coroutines have run
            assertEquals("default_user", expectMostRecentItem()) // Assert the most recent, stable state
        }
    }

    @Test
    fun allCategories_onInit_isEmpty() = runTest {
        viewModel.allCategories.test {
            assertEquals(emptyList<Category>(), awaitItem())
        }
    }

    @Test
    fun allImages_onInit_isEmpty() = runTest {
        viewModel.allImages.test {
            assertEquals(emptyList<Image>(), awaitItem())
        }
    }

    @Test
    fun allColors_onInit_isEmpty() = runTest {
        viewModel.allColors.test {
            assertEquals(emptyList<AppColor>(), awaitItem())
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
    fun setUsername_withBlankUsername_doesNotUpdateUsername() = runTest {
        viewModel.setUsername(" ")
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 0) { appSettingsManager.setUsername(any()) }
    }

    @Test
    fun setUsername_whenManagerThrowsError_sendsUpdateFailedEvent() = runTest {
        val newUsername = "testuser"
        coEvery { appSettingsManager.setUsername(newUsername) } throws RuntimeException("Error saving username")
        viewModel.uiEvents.test {
            viewModel.setUsername(newUsername)
            assertEquals(OptionsViewModel.OptionsUiEvent.UpdateUsernameFailed, awaitItem())
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
    fun setCategoryDefault_withBlankCategoryId_sendsCategoryIdInvalidEvent() = runTest {
        val identifier = ImageIdentifier.Icon("test_icon")
        viewModel.uiEvents.test {
            viewModel.setCategoryDefault(" ", identifier)
            assertEquals(OptionsViewModel.OptionsUiEvent.CategoryIdInvalid, awaitItem())
        }
    }

    @Test
    fun setCategoryDefault_withNullImageIdentifier_sendsNoImageSelectedEvent() = runTest {
        viewModel.uiEvents.test {
            viewModel.setCategoryDefault("test_category", null)
            assertEquals(OptionsViewModel.OptionsUiEvent.NoImageSelectedForDefault, awaitItem())
        }
    }

    @Test
    fun setCategoryDefault_whenRepositoryThrowsError_sendsSetCategoryDefaultFailedEvent() = runTest {
        val categoryId = "test_category"
        val identifier = ImageIdentifier.Icon("test_icon")
        coEvery { imageRepository.setCategoryDefault(any(), any()) } throws RuntimeException("DB error")

        viewModel.uiEvents.test {
            viewModel.setCategoryDefault(categoryId, identifier)
            assertEquals(OptionsViewModel.OptionsUiEvent.SetCategoryDefaultFailed, awaitItem())
        }
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
    fun addImage_withBlankCategory_sendsCategoryIdInvalidEvent() = runTest {
        val identifier = ImageIdentifier.Photo("test_uri")
        viewModel.uiEvents.test {
            viewModel.addImage(identifier, " ")
            assertEquals(OptionsViewModel.OptionsUiEvent.CategoryIdInvalid, awaitItem())
        }
    }

    @Test
    fun addImage_whenRepositoryThrowsError_sendsAddImageFailedEvent() = runTest {
        val identifier = ImageIdentifier.Photo("test_uri")
        val category = "test_category"
        coEvery { imageRepository.addImage(any(), any()) } throws RuntimeException("DB error")

        viewModel.uiEvents.test {
            viewModel.addImage(identifier, category)
            assertEquals(OptionsViewModel.OptionsUiEvent.AddImageFailed, awaitItem())
        }
    }

    @Test
    fun toggleImageVisibility_withValidData_callsRepository() = runTest {
        val image: Image = mockk()
        viewModel.toggleImageVisibility(image)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { imageRepository.toggleImageVisibility(image) }
    }

    @Test
    fun toggleImageVisibility_whenRepositoryThrowsError_sendsToggleImageVisibilityFailedEvent() = runTest {
        val image: Image = mockk()
        coEvery { imageRepository.toggleImageVisibility(any()) } throws RuntimeException("DB error")

        viewModel.uiEvents.test {
            viewModel.toggleImageVisibility(image)
            assertEquals(OptionsViewModel.OptionsUiEvent.ToggleImageVisibilityFailed, awaitItem())
        }
    }


    @Test
    fun removeImage_withValidData_callsRepository() = runTest {
        val image: Image = mockk()
        viewModel.removeImage(image)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { imageRepository.removeImage(image) }
    }

    @Test
    fun removeImage_whenRepositoryThrowsError_sendsUiEvent() = runTest {
        val image: Image = mockk()
        val error = RuntimeException("Simulated database error")
        coEvery { imageRepository.removeImage(image) } throws error

        val job = launch {
            viewModel.uiEvents.test {
                assertEquals(OptionsViewModel.OptionsUiEvent.RemoveImageFailed, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

        viewModel.removeImage(image)
        job.join()
    }

    @Test
    fun updateImageOrder_withEmptyList_doesNothing() = runTest {
        viewModel.updateImageOrder(emptyList())
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 0) { imageRepository.updateImageOrder(any()) }
    }

    @Test
    fun updateImageOrder_withValidData_callsRepository() = runTest {
        val imageList: List<Image> = listOf(mockk(), mockk())
        viewModel.updateImageOrder(imageList)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { imageRepository.updateImageOrder(imageList) }
    }

    @Test
    fun updateImageOrder_whenRepositoryThrowsError_sendsUpdateImageOrderFailedEvent() = runTest {
        val imageList: List<Image> = listOf(mockk())
        coEvery { imageRepository.updateImageOrder(any()) } throws RuntimeException("DB error")

        viewModel.uiEvents.test {
            viewModel.updateImageOrder(imageList)
            assertEquals(OptionsViewModel.OptionsUiEvent.UpdateImageOrderFailed, awaitItem())
        }
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
    fun updateCategoryColor_withBlankCategoryId_sendsCategoryIdInvalidEvent() = runTest {
        viewModel.uiEvents.test {
            viewModel.updateCategoryColor(" ", "#FFFFFF")
            assertEquals(OptionsViewModel.OptionsUiEvent.CategoryIdInvalid, awaitItem())
        }
    }

    @Test
    fun updateCategoryColor_withBlankColorHex_sendsColorHexInvalidEvent() = runTest {
        viewModel.uiEvents.test {
            viewModel.updateCategoryColor("test_id", " ")
            assertEquals(OptionsViewModel.OptionsUiEvent.ColorHexInvalid, awaitItem())
        }
    }

    @Test
    fun updateCategoryColor_whenRepositoryThrowsError_sendsUpdateCategoryColorFailedEvent() = runTest {
        coEvery { imageRepository.updateCategoryColor(any(), any()) } throws RuntimeException("DB Error")
        viewModel.uiEvents.test {
            viewModel.updateCategoryColor("test_id", "#FFFFFF")
            assertEquals(OptionsViewModel.OptionsUiEvent.UpdateCategoryColorFailed, awaitItem())
        }
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
    fun addColor_withBlankHex_sendsColorHexInvalidEvent() = runTest {
        viewModel.uiEvents.test {
            viewModel.addColor(" ", "test_name")
            assertEquals(OptionsViewModel.OptionsUiEvent.ColorHexInvalid, awaitItem())
        }
    }

    @Test
    fun addColor_withBlankName_sendsColorNameInvalidEvent() = runTest {
        viewModel.uiEvents.test {
            viewModel.addColor("#FFFFFF", " ")
            assertEquals(OptionsViewModel.OptionsUiEvent.ColorNameInvalid, awaitItem())
        }
    }

    @Test
    fun addColor_whenRepositoryThrowsError_sendsAddColorFailedEvent() = runTest {
        coEvery { imageRepository.addColor(any(), any()) } throws RuntimeException("DB Error")
        viewModel.uiEvents.test {
            viewModel.addColor("#FFFFFF", "test_name")
            val event = awaitItem()
            assertTrue(event is OptionsViewModel.OptionsUiEvent.AddColorFailed)
            assertEquals("test_name", event.name)
        }
    }

    @Test
    fun updateColor_whenNameIsBlank_sendsInvalidNameEvent() = runTest {
        val invalidColor: AppColor = mockk()
        every { invalidColor.name } returns " "
        viewModel.uiEvents.test {
            viewModel.updateColor(invalidColor)
            assertEquals(OptionsViewModel.OptionsUiEvent.ColorNameInvalid, awaitItem())
        }
    }

    @Test
    fun updateColor_whenRepositoryThrowsError_sendsUpdateFailedEvent() = runTest {
        val validColor: AppColor = mockk()
        every { validColor.name } returns "Valid Name"
        coEvery { imageRepository.updateColor(validColor) } throws RuntimeException()
        viewModel.uiEvents.test {
            viewModel.updateColor(validColor)
            assertEquals(OptionsViewModel.OptionsUiEvent.UpdateColorFailed, awaitItem())
        }
    }

    @Test
    fun updateColor_withValidData_callsRepository() = runTest {
        val validColor: AppColor = mockk()
        every { validColor.name } returns "Valid Name"
        viewModel.updateColor(validColor)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { imageRepository.updateColor(validColor) }
    }

    @Test
    fun updateColorsOrder_withValidData_callsRepository() = runTest {
        val colors: List<AppColor> = listOf(mockk())
        viewModel.updateColorsOrder(colors)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { imageRepository.updateColorsOrder(colors) }
    }

    @Test
    fun updateColorsOrder_withEmptyList_doesNothing() = runTest {
        viewModel.updateColorsOrder(emptyList())
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 0) { imageRepository.updateColorsOrder(any()) }
    }

    @Test
    fun updateColorsOrder_whenRepositoryThrowsError_sendsUpdateColorsOrderFailedEvent() = runTest {
        val colors: List<AppColor> = listOf(mockk())
        coEvery { imageRepository.updateColorsOrder(any()) } throws RuntimeException("DB Error")
        viewModel.uiEvents.test {
            viewModel.updateColorsOrder(colors)
            assertEquals(OptionsViewModel.OptionsUiEvent.UpdateColorsOrderFailed, awaitItem())
        }
    }

    @Test
    fun toggleColorVisibility_withValidId_callsRepository() = runTest {
        viewModel.toggleColorVisibility(1L)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { imageRepository.toggleColorVisibility(1L) }
    }

    @Test
    fun toggleColorVisibility_withInvalidId_sendsColorIdInvalidEvent() = runTest {
        viewModel.uiEvents.test {
            viewModel.toggleColorVisibility(0L)
            assertEquals(OptionsViewModel.OptionsUiEvent.ColorIdInvalid, awaitItem())
        }
    }

    @Test
    fun toggleColorVisibility_whenRepositoryThrowsError_sendsToggleColorVisibilityFailedEvent() = runTest {
        coEvery { imageRepository.toggleColorVisibility(any()) } throws RuntimeException("DB Error")
        viewModel.uiEvents.test {
            viewModel.toggleColorVisibility(1L)
            assertEquals(OptionsViewModel.OptionsUiEvent.ToggleColorVisibilityFailed, awaitItem())
        }
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
    fun deleteColor_withInvalidId_sendsColorIdInvalidEvent() = runTest {
        val color: AppColor = mockk(relaxed = true) {
            every { id } returns 0L
        }
        viewModel.uiEvents.test {
            viewModel.deleteColor(color)
            assertEquals(OptionsViewModel.OptionsUiEvent.ColorIdInvalid, awaitItem())
        }
    }

    @Test
    fun deleteColor_whenRepositoryThrowsError_sendsDeleteColorFailedEvent() = runTest {
        val color: AppColor = mockk(relaxed = true) {
            every { id } returns 1L
        }
        coEvery { imageRepository.deleteColor(any()) } throws RuntimeException("DB Error")
        viewModel.uiEvents.test {
            viewModel.deleteColor(color)
            assertEquals(OptionsViewModel.OptionsUiEvent.DeleteColorFailed, awaitItem())
        }
    }

    @Test
    fun isPhotoUsed_whenUriIsBlank_sendsUriInvalidEventAndReturnsTrue() = runTest {
        viewModel.uiEvents.test {
            val result = viewModel.isPhotoUsed(" ")
            assertTrue(result)
            assertEquals(OptionsViewModel.OptionsUiEvent.PhotoUriInvalid, awaitItem())
        }
    }

    @Test
    fun isPhotoUsed_whenDaoThrowsError_sendsCheckFailedEventAndReturnsTrue() = runTest {
        val uri = "test_uri"
        coEvery { equipmentDao.countEquipmentsUsingPhoto(uri) } throws RuntimeException()
        viewModel.uiEvents.test {
            val result = viewModel.isPhotoUsed(uri)
            assertTrue(result)
            assertEquals(OptionsViewModel.OptionsUiEvent.DatabaseCheckFailed, awaitItem())
        }
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

}