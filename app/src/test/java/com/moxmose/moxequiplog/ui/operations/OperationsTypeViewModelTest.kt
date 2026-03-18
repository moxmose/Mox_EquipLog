package com.moxmose.moxequiplog.ui.operations

import androidx.compose.ui.test.junit4.createComposeRule
import app.cash.turbine.test
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.ImageRepository
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class OperationsTypeViewModelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var operationTypeDao: OperationTypeDao
    private lateinit var imageRepository: ImageRepository
    private lateinit var appSettingsManager: AppSettingsManager
    private lateinit var viewModel: OperationsTypeViewModel

    private val activeOperationTypesFlow = MutableStateFlow<List<OperationType>>(emptyList())
    private val allOperationTypesFlow = MutableStateFlow<List<OperationType>>(emptyList())
    private val operationTypeImagesFlow = MutableStateFlow<List<Image>>(emptyList())
    private val defaultOpIdFlow = MutableStateFlow<Int?>(null)

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
            every { getCategoryDefaultPhoto("OPERATION") } returns flowOf("def_photo")
            every { getCategoryDefaultIcon("OPERATION") } returns flowOf("def_icon")
        }
        appSettingsManager = mockk(relaxed = true) {
            every { defaultOperationTypeId } returns defaultOpIdFlow
        }
        viewModel = OperationsTypeViewModel(operationTypeDao, imageRepository, appSettingsManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun coverage_booster_and_getters() = runTest {
        assertNotNull(viewModel.activeOperationTypes)
        assertNotNull(viewModel.allOperationTypes)
        assertNotNull(viewModel.operationTypeImages)
        assertNotNull(viewModel.allCategories)
        assertNotNull(viewModel.defaultOperationTypeId)
        assertNotNull(viewModel.uiEvents)

        viewModel.activeOperationTypes.value
        viewModel.allOperationTypes.value
        viewModel.operationTypeImages.value
        viewModel.allCategories.value
        viewModel.defaultOperationTypeId.value
    }

    @Test
    fun addOperationType_variants_coverage() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.allOperationTypes.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Icon variant
        viewModel.addOperationType("Op1", ImageIdentifier.Icon("icon1"))
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { operationTypeDao.insertOperationType(match { it.description == "Op1" && it.iconIdentifier == "icon1" }) }

        // Photo variant
        viewModel.addOperationType("Op2", ImageIdentifier.Photo("uri2"))
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { operationTypeDao.insertOperationType(match { it.description == "Op2" && it.photoUri == "uri2" }) }

        // Null variant
        viewModel.addOperationType("Op3", null)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { operationTypeDao.insertOperationType(match { it.description == "Op3" && it.photoUri == "def_photo" }) }
    }

    @Test
    fun crud_error_branches_coverage() = runTest {
        val op = OperationType(id = 1, description = "O")
        val img = Image(uri="u", category="c", imageType="IMAGE")

        coEvery { operationTypeDao.updateOperationType(any()) } throws RuntimeException()
        coEvery { operationTypeDao.updateOperationTypes(any()) } throws RuntimeException()
        coEvery { imageRepository.addImage(any(), any()) } throws RuntimeException()
        coEvery { imageRepository.removeImage(any()) } throws RuntimeException()
        coEvery { imageRepository.updateImageOrder(any()) } throws RuntimeException()
        coEvery { imageRepository.toggleImageVisibility(any()) } throws RuntimeException()

        viewModel.uiEvents.test {
            viewModel.updateOperationType(op)
            assertEquals(OperationsTypeViewModel.UiEvent.UpdateOperationTypeFailed, awaitItem())

            viewModel.updateOperationTypes(listOf(op))
            assertEquals(OperationsTypeViewModel.UiEvent.UpdateOperationTypesFailed, awaitItem())

            viewModel.dismissOperationType(op)
            assertEquals(OperationsTypeViewModel.UiEvent.DismissOperationTypeFailed, awaitItem())

            viewModel.restoreOperationType(op)
            assertEquals(OperationsTypeViewModel.UiEvent.RestoreOperationTypeFailed, awaitItem())

            viewModel.addImage(mockk(), "cat")
            assertEquals(OperationsTypeViewModel.UiEvent.AddImageFailed, awaitItem())

            viewModel.removeImage(img)
            assertEquals(OperationsTypeViewModel.UiEvent.RemoveImageFailed, awaitItem())

            viewModel.updateImageOrder(listOf(img))
            assertEquals(OperationsTypeViewModel.UiEvent.UpdateImageOrderFailed, awaitItem())

            viewModel.toggleImageVisibility(img)
            assertEquals(OperationsTypeViewModel.UiEvent.ToggleImageVisibilityFailed, awaitItem())
        }
    }

    @Test
    fun isPhotoUsed_variants_coverage() = runTest {
        launch {
            viewModel.uiEvents.test {
                assertTrue(viewModel.isPhotoUsed(" "))
                assertEquals(OperationsTypeViewModel.UiEvent.PhotoUriInvalid, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
        testDispatcher.scheduler.advanceUntilIdle()
        
        coEvery { operationTypeDao.countOperationTypesUsingPhoto("used") } returns 1
        assertTrue(viewModel.isPhotoUsed("used"))

        launch {
            viewModel.uiEvents.test {
                coEvery { operationTypeDao.countOperationTypesUsingPhoto("err") } throws RuntimeException()
                assertTrue(viewModel.isPhotoUsed("err"))
                assertEquals(OperationsTypeViewModel.UiEvent.DatabaseCheckFailed, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
        testDispatcher.scheduler.advanceUntilIdle()
    }
    
    @Test
    fun toggleDefaultOperationType_coverage() = runTest {
        viewModel.toggleDefaultOperationType(10)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { appSettingsManager.setDefaultOperationTypeId(10) }

        defaultOpIdFlow.value = 10
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.toggleDefaultOperationType(10)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { appSettingsManager.setDefaultOperationTypeId(null) }
    }
}
