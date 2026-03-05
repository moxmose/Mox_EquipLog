package com.moxmose.moxequiplog.ui.operations

import androidx.compose.ui.test.junit4.createComposeRule
import app.cash.turbine.test
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.ImageRepository
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.Image
import com.moxmose.moxequiplog.data.local.ImageIdentifier
import com.moxmose.moxequiplog.data.local.OperationType
import com.moxmose.moxequiplog.data.local.OperationTypeDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
class OperationTypeViewModelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var operationTypeDao: OperationTypeDao
    private lateinit var imageRepository: ImageRepository
    private lateinit var appSettingsManager: AppSettingsManager
    private lateinit var viewModel: OperationTypeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        operationTypeDao = mockk(relaxed = true) {
            every { getActiveOperationTypes() } returns MutableStateFlow(emptyList())
            every { getAllOperationTypes() } returns MutableStateFlow(emptyList())
        }
        imageRepository = mockk(relaxed = true) {
            every { getImagesByCategory("OPERATION") } returns MutableStateFlow(emptyList())
            every { allCategories } returns MutableStateFlow(emptyList())
            every { getCategoryDefaultPhoto("OPERATION") } returns flowOf(null)
            every { getCategoryDefaultIcon("OPERATION") } returns flowOf(null)
        }
        appSettingsManager = mockk(relaxed = true) {
            every { defaultOperationTypeId } returns MutableStateFlow(null)
        }
        viewModel = OperationTypeViewModel(operationTypeDao, imageRepository, appSettingsManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun activeOperationTypes_onInit_isEmpty() = runTest {
        viewModel.activeOperationTypes.test {
            assertEquals(emptyList<OperationType>(), awaitItem())
        }
    }

    @Test
    fun allOperationTypes_onInit_isEmpty() = runTest {
        viewModel.allOperationTypes.test {
            assertEquals(emptyList<OperationType>(), awaitItem())
        }
    }

    @Test
    fun operationTypeImages_onInit_isEmpty() = runTest {
        viewModel.operationTypeImages.test {
            assertEquals(emptyList<Image>(), awaitItem())
        }
    }

    @Test
    fun allCategories_onInit_isEmpty() = runTest {
        viewModel.allCategories.test {
            assertEquals(emptyList<Category>(), awaitItem())
        }
    }

    @Test
    fun addOperationType_withBlankDescription_sendsDescriptionInvalidEvent() = runTest {
        viewModel.uiEvents.test {
            viewModel.addOperationType(" ", null)
            assertEquals(OperationTypeViewModel.UiEvent.DescriptionInvalid, awaitItem())
        }
    }

    @Test
    fun addOperationType_withBlankDescription_doesNotInsertOperationType() = runTest {
        viewModel.addOperationType(" ", null)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 0) { operationTypeDao.insertOperationType(any()) }
    }

    @Test
    fun addOperationType_withValidData_callsDao() = runTest {
        val description = "New OperationType"
        val imageIdentifier = ImageIdentifier.Icon("some_icon")

        val operationCategory = Category(id = "OPERATION", name = "Operation")
        every { imageRepository.allCategories } returns MutableStateFlow(listOf(operationCategory))


        viewModel.addOperationType(description, imageIdentifier)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { operationTypeDao.insertOperationType(any()) }
    }

    @Test
    fun addOperationType_whenDaoThrowsError_sendsAddOperationTypeFailedEvent() = runTest {
        coEvery { operationTypeDao.insertOperationType(any()) } throws RuntimeException("DB error")
        viewModel.uiEvents.test {
            viewModel.addOperationType("description", null)
            assertEquals(OperationTypeViewModel.UiEvent.AddOperationTypeFailed, awaitItem())
        }
    }

    @Test
    fun updateOperationType_whenDaoThrowsError_sendsUpdateOperationTypeFailedEvent() = runTest {
        coEvery { operationTypeDao.updateOperationType(any()) } throws RuntimeException("DB error")
        viewModel.uiEvents.test {
            viewModel.updateOperationType(mockk())
            assertEquals(OperationTypeViewModel.UiEvent.UpdateOperationTypeFailed, awaitItem())
        }
    }

    @Test
    fun updateOperationTypes_withEmptyList_doesNothing() = runTest {
        viewModel.updateOperationTypes(emptyList())
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 0) { operationTypeDao.updateOperationTypes(any()) }
    }

    @Test
    fun updateOperationTypes_whenDaoThrowsError_sendsUpdateOperationTypesFailedEvent() = runTest {
        coEvery { operationTypeDao.updateOperationTypes(any()) } throws RuntimeException("DB error")
        viewModel.uiEvents.test {
            viewModel.updateOperationTypes(listOf(mockk()))
            assertEquals(OperationTypeViewModel.UiEvent.UpdateOperationTypesFailed, awaitItem())
        }
    }

    @Test
    fun dismissOperationType_whenDaoThrowsError_sendsDismissOperationTypeFailedEvent() = runTest {
        coEvery { operationTypeDao.updateOperationType(any()) } throws RuntimeException("DB error")
        viewModel.uiEvents.test {
            viewModel.dismissOperationType(mockk())
            assertEquals(OperationTypeViewModel.UiEvent.DismissOperationTypeFailed, awaitItem())
        }
    }

    @Test
    fun restoreOperationType_whenDaoThrowsError_sendsRestoreOperationTypeFailedEvent() = runTest {
        coEvery { operationTypeDao.updateOperationType(any()) } throws RuntimeException("DB error")
        viewModel.uiEvents.test {
            viewModel.restoreOperationType(mockk())
            assertEquals(OperationTypeViewModel.UiEvent.RestoreOperationTypeFailed, awaitItem())
        }
    }

    @Test
    fun addImage_whenRepositoryThrowsError_sendsAddImageFailedEvent() = runTest {
        coEvery { imageRepository.addImage(any(), any()) } throws RuntimeException("DB error")
        viewModel.uiEvents.test {
            viewModel.addImage(mockk(), "category")
            assertEquals(OperationTypeViewModel.UiEvent.AddImageFailed, awaitItem())
        }
    }

    @Test
    fun removeImage_whenRepositoryThrowsError_sendsRemoveImageFailedEvent() = runTest {
        coEvery { imageRepository.removeImage(any()) } throws RuntimeException("DB error")
        viewModel.uiEvents.test {
            viewModel.removeImage(mockk())
            assertEquals(OperationTypeViewModel.UiEvent.RemoveImageFailed, awaitItem())
        }
    }

    @Test
    fun updateImageOrder_withEmptyList_doesNothing() = runTest {
        viewModel.updateImageOrder(emptyList())
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 0) { imageRepository.updateImageOrder(any()) }
    }

    @Test
    fun updateImageOrder_whenRepositoryThrowsError_sendsUpdateImageOrderFailedEvent() = runTest {
        coEvery { imageRepository.updateImageOrder(any()) } throws RuntimeException("DB error")
        viewModel.uiEvents.test {
            viewModel.updateImageOrder(listOf(mockk()))
            assertEquals(OperationTypeViewModel.UiEvent.UpdateImageOrderFailed, awaitItem())
        }
    }

    @Test
    fun toggleImageVisibility_whenRepositoryThrowsError_sendsToggleImageVisibilityFailedEvent() = runTest {
        coEvery { imageRepository.toggleImageVisibility(any()) } throws RuntimeException("DB error")
        viewModel.uiEvents.test {
            viewModel.toggleImageVisibility(mockk())
            assertEquals(OperationTypeViewModel.UiEvent.ToggleImageVisibilityFailed, awaitItem())
        }
    }

    @Test
    fun isPhotoUsed_whenUriIsBlank_sendsUriInvalidEventAndReturnsTrue() = runTest {
        viewModel.uiEvents.test {
            val result = viewModel.isPhotoUsed(" ")
            assertTrue(result)
            assertEquals(OperationTypeViewModel.UiEvent.PhotoUriInvalid, awaitItem())
        }
    }

    @Test
    fun isPhotoUsed_whenDaoThrowsError_sendsCheckFailedEventAndReturnsTrue() = runTest {
        val uri = "test_uri"
        coEvery { operationTypeDao.countOperationTypesUsingPhoto(uri) } throws RuntimeException()
        viewModel.uiEvents.test {
            val result = viewModel.isPhotoUsed(uri)
            assertTrue(result)
            assertEquals(OperationTypeViewModel.UiEvent.DatabaseCheckFailed, awaitItem())
        }
    }

    @Test
    fun isPhotoUsed_whenPhotoIsInUse_returnsTrue() = runTest {
        val uri = "test_uri"
        coEvery { operationTypeDao.countOperationTypesUsingPhoto(uri) } returns 1
        val result = viewModel.isPhotoUsed(uri)
        assertTrue(result)
    }

    @Test
    fun isPhotoUsed_whenPhotoIsNotInUse_returnsFalse() = runTest {
        val uri = "test_uri"
        coEvery { operationTypeDao.countOperationTypesUsingPhoto(uri) } returns 0
        val result = viewModel.isPhotoUsed(uri)
        assertFalse(result)
    }
}