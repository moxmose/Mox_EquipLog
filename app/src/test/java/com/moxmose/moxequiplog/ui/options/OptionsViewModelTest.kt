package com.moxmose.moxequiplog.ui.options

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
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
    fun setUsername_withValidUsername_updatesUsernameFlow() = runTest {
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
    fun setCategoryDefault_withValidData_callsRepository() = runTest {
        val categoryId = "test_category"
        val identifier = ImageIdentifier.Icon("test_icon")

        viewModel.setCategoryDefault(categoryId, identifier)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { imageRepository.setCategoryDefault(categoryId, identifier) }
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
    fun toggleImageVisibility_withValidData_callsRepository() = runTest {
        val image: Image = mockk()
        viewModel.toggleImageVisibility(image)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { imageRepository.toggleImageVisibility(image) }
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