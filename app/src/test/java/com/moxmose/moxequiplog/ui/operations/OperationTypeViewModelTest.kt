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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
class OperationTypeViewModelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var operationTypeDao: OperationTypeDao
    private lateinit var imageRepository: ImageRepository
    private lateinit var appSettingsManager: AppSettingsManager
    private lateinit var viewModel: OperationTypeViewModel

    private val activeOperationTypesFlow = MutableStateFlow<List<OperationType>>(emptyList())
    private val allOperationTypesFlow = MutableStateFlow<List<OperationType>>(emptyList())
    private val operationTypeImagesFlow = MutableStateFlow<List<Image>>(emptyList())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        operationTypeDao = mockk(relaxed = true) {
            every { getActiveOperationTypes() } returns activeOperationTypesFlow
            every { getAllOperationTypes() } returns allOperationTypesFlow
        }
        imageRepository = mockk(relaxed = true) {
            every { getImagesByCategory("OPERATION") } returns operationTypeImagesFlow
            every { allCategories } returns MutableStateFlow(emptyList())
            every { getCategoryColor(any()) } returns MutableStateFlow("#000000")
            every { getCategoryDefaultPhoto(any()) } returns flowOf(null)
            every { getCategoryDefaultIcon(any()) } returns flowOf(null)
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
    fun getCategoryMethods_delegateToRepository() = runTest {
        val colorFlow = MutableStateFlow("#123456")
        every { imageRepository.getCategoryColor("OP") } returns colorFlow
        assertEquals("#123456", viewModel.getCategoryColor("OP").first())
        
        val iconFlow = MutableStateFlow("icon")
        every { imageRepository.getCategoryDefaultIcon("OP") } returns iconFlow
        assertEquals("icon", viewModel.getCategoryDefaultIcon("OP").first())
    }

    @Test
    fun activeOperationTypes_emitsData() = runTest {
        viewModel.activeOperationTypes.test {
            assertEquals(emptyList<OperationType>(), awaitItem())
            val list = listOf(OperationType(id = 1, description = "Test"))
            activeOperationTypesFlow.value = list
            assertEquals(list, awaitItem())
        }
    }

    @Test
    fun addOperationType_withValidData_callsDao() = runTest {
        // Attiviamo il flow perché addOperationType ne legge il .value
        backgroundScope.launch(testDispatcher) { viewModel.allOperationTypes.collect {} }
        
        val description = "New"
        val imageIdentifier = ImageIdentifier.Icon("icon")
        
        // Prepariamo i dati esistenti per il calcolo dell'ordine (max 5 + 1 = 6)
        allOperationTypesFlow.value = listOf(OperationType(id = 1, description = "E1", displayOrder = 5))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.addOperationType(description, imageIdentifier)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { operationTypeDao.insertOperationType(match { it.description == description && it.displayOrder == 6 }) }
    }

    @Test
    fun addOperationType_whenDaoThrowsError_sendsUiEvent() = runTest {
        coEvery { operationTypeDao.insertOperationType(any()) } throws RuntimeException()
        viewModel.uiEvents.test {
            viewModel.addOperationType("Valid", null)
            assertEquals(OperationTypeViewModel.UiEvent.AddOperationTypeFailed, awaitItem())
        }
    }

    @Test
    fun updateOperationType_whenDaoThrowsError_sendsUiEvent() = runTest {
        coEvery { operationTypeDao.updateOperationType(any()) } throws RuntimeException()
        viewModel.uiEvents.test {
            viewModel.updateOperationType(mockk())
            assertEquals(OperationTypeViewModel.UiEvent.UpdateOperationTypeFailed, awaitItem())
        }
    }

    @Test
    fun updateOperationTypes_whenDaoThrowsError_sendsUiEvent() = runTest {
        coEvery { operationTypeDao.updateOperationTypes(any()) } throws RuntimeException()
        viewModel.uiEvents.test {
            viewModel.updateOperationTypes(listOf(mockk()))
            assertEquals(OperationTypeViewModel.UiEvent.UpdateOperationTypesFailed, awaitItem())
        }
    }

    @Test
    fun dismissOperationType_whenDaoThrowsError_sendsUiEvent() = runTest {
        coEvery { operationTypeDao.updateOperationType(any()) } throws RuntimeException()
        viewModel.uiEvents.test {
            viewModel.dismissOperationType(mockk())
            assertEquals(OperationTypeViewModel.UiEvent.DismissOperationTypeFailed, awaitItem())
        }
    }

    @Test
    fun restoreOperationType_whenDaoThrowsError_sendsUiEvent() = runTest {
        coEvery { operationTypeDao.updateOperationType(any()) } throws RuntimeException()
        viewModel.uiEvents.test {
            viewModel.restoreOperationType(mockk())
            assertEquals(OperationTypeViewModel.UiEvent.RestoreOperationTypeFailed, awaitItem())
        }
    }

    @Test
    fun addImage_whenRepositoryThrowsError_sendsUiEvent() = runTest {
        coEvery { imageRepository.addImage(any(), any()) } throws RuntimeException()
        viewModel.uiEvents.test {
            viewModel.addImage(mockk(), "cat")
            assertEquals(OperationTypeViewModel.UiEvent.AddImageFailed, awaitItem())
        }
    }

    @Test
    fun removeImage_whenRepositoryThrowsError_sendsUiEvent() = runTest {
        coEvery { imageRepository.removeImage(any()) } throws RuntimeException()
        viewModel.uiEvents.test {
            viewModel.removeImage(mockk())
            assertEquals(OperationTypeViewModel.UiEvent.RemoveImageFailed, awaitItem())
        }
    }

    @Test
    fun updateImageOrder_whenRepositoryThrowsError_sendsUiEvent() = runTest {
        coEvery { imageRepository.updateImageOrder(any()) } throws RuntimeException()
        viewModel.uiEvents.test {
            viewModel.updateImageOrder(listOf(mockk()))
            assertEquals(OperationTypeViewModel.UiEvent.UpdateImageOrderFailed, awaitItem())
        }
    }

    @Test
    fun toggleImageVisibility_whenRepositoryThrowsError_sendsUiEvent() = runTest {
        coEvery { imageRepository.toggleImageVisibility(any()) } throws RuntimeException()
        viewModel.uiEvents.test {
            viewModel.toggleImageVisibility(mockk())
            assertEquals(OperationTypeViewModel.UiEvent.ToggleImageVisibilityFailed, awaitItem())
        }
    }

    @Test
    fun isPhotoUsed_whenDaoThrowsError_sendsUiEventAndReturnsTrue() = runTest {
        coEvery { operationTypeDao.countOperationTypesUsingPhoto(any()) } throws RuntimeException()
        viewModel.uiEvents.test {
            assertTrue(viewModel.isPhotoUsed("uri"))
            assertEquals(OperationTypeViewModel.UiEvent.DatabaseCheckFailed, awaitItem())
        }
    }
    
    @Test
    fun toggleDefaultOperationType_callsManager() = runTest {
        viewModel.toggleDefaultOperationType(10)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { appSettingsManager.setDefaultOperationTypeId(10) }
    }
}
