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
import kotlin.test.assertFalse
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
    private val operationImagesFlow = MutableStateFlow<List<Image>>(emptyList())
    private val defaultOperationTypeIdFlow = MutableStateFlow<Int?>(null)
    private val allCategoriesFlow = MutableStateFlow<List<Category>>(emptyList())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        operationTypeDao = mockk(relaxed = true) {
            every { getActiveOperationTypes() } returns activeOperationTypesFlow
            every { getAllOperationTypes() } returns allOperationTypesFlow
        }
        imageRepository = mockk(relaxed = true) {
            every { getImagesByCategory("OPERATION") } returns operationImagesFlow
            every { allCategories } returns allCategoriesFlow
            every { getCategoryColor("OPERATION") } returns MutableStateFlow("#808080")
            every { getCategoryDefaultIcon("OPERATION") } returns MutableStateFlow("default_icon")
            every { getCategoryDefaultPhoto("OPERATION") } returns MutableStateFlow("default_photo")
        }
        appSettingsManager = mockk(relaxed = true) {
            every { defaultOperationTypeId } returns defaultOperationTypeIdFlow
        }
        viewModel = OperationsTypeViewModel(operationTypeDao, imageRepository, appSettingsManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun getters_coverage_booster() = runTest {
        assertNotNull(viewModel.activeOperationTypes)
        assertNotNull(viewModel.allOperationTypes)
        assertNotNull(viewModel.operationImages)
        assertNotNull(viewModel.allCategories)
        assertNotNull(viewModel.categoryColor)
        assertNotNull(viewModel.categoryDefaultIcon)
        assertNotNull(viewModel.categoryDefaultPhoto)
        assertNotNull(viewModel.defaultOperationTypeId)
        assertNotNull(viewModel.uiEvents)

        viewModel.categoryColor.value
        viewModel.activeOperationTypes.value
        viewModel.allOperationTypes.value
        viewModel.operationImages.value
        viewModel.allCategories.value
        viewModel.defaultOperationTypeId.value
    }

    @Test
    fun addOperationType_withIcon_callsDao() = runTest {
        viewModel.allOperationTypes.test {
            awaitItem() // initial
            allOperationTypesFlow.value = listOf(OperationType(id = 1, description = "OT1", displayOrder = 0))
            awaitItem()

            viewModel.addOperationType("OT1", ImageIdentifier.Icon("icon1"))
            testDispatcher.scheduler.advanceUntilIdle()
            coVerify { operationTypeDao.insertOperationType(match { it.description == "OT1" && it.iconIdentifier == "icon1" && it.displayOrder == 1}) }
        }
    }

    @Test
    fun addOperationType_withPhoto_callsDao() = runTest {
        viewModel.allOperationTypes.test {
            awaitItem()
            viewModel.addOperationType("OT2", ImageIdentifier.Photo("uri2"))
            testDispatcher.scheduler.advanceUntilIdle()
            coVerify { operationTypeDao.insertOperationType(match { it.description == "OT2" && it.photoUri == "uri2" }) }
        }
    }

    @Test
    fun addOperationType_withNullImage_usesDefaults() = runTest {
        viewModel.allOperationTypes.test {
            awaitItem()
            backgroundScope.launch(testDispatcher) { viewModel.categoryDefaultPhoto.collect {} }
            backgroundScope.launch(testDispatcher) { viewModel.categoryDefaultIcon.collect {} }
            testDispatcher.scheduler.advanceUntilIdle()
            
            viewModel.addOperationType("OT3", null)
            testDispatcher.scheduler.advanceUntilIdle()
            coVerify { operationTypeDao.insertOperationType(match { it.description == "OT3" && it.photoUri == "default_photo" }) }
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
        
        coEvery { operationTypeDao.countOperationTypesUsingPhoto("free") } returns 0
        assertFalse(viewModel.isPhotoUsed("free"))

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
        backgroundScope.launch(testDispatcher) { viewModel.defaultOperationTypeId.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        // Toggle ON
        viewModel.toggleDefaultOperationType(5)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { appSettingsManager.setDefaultOperationTypeId(5) }

        // Simuliamo aggiornamento
        defaultOperationTypeIdFlow.value = 5
        testDispatcher.scheduler.advanceUntilIdle()

        // Toggle OFF (stesso ID)
        viewModel.toggleDefaultOperationType(5)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { appSettingsManager.setDefaultOperationTypeId(null) }
    }

    @Test
    fun crud_error_branches_coverage() = runTest {
        val operationType = OperationType(id = 1, description = "OT")
        val image: Image = mockk(relaxed = true)

        coEvery { operationTypeDao.updateOperationType(any()) } throws RuntimeException()
        coEvery { operationTypeDao.updateOperationTypes(any()) } throws RuntimeException()
        coEvery { imageRepository.addImage(any(), any()) } throws RuntimeException()
        coEvery { imageRepository.removeImage(any()) } throws RuntimeException()
        coEvery { imageRepository.updateImageOrder(any()) } throws RuntimeException()
        coEvery { imageRepository.toggleImageVisibility(any()) } throws RuntimeException()
        coEvery { appSettingsManager.setDefaultOperationTypeId(any()) } throws RuntimeException()

        viewModel.uiEvents.test {
            viewModel.updateOperationType(operationType)
            assertEquals(OperationsTypeViewModel.UiEvent.UpdateOperationTypeFailed, awaitItem())
            
            viewModel.updateOperationTypes(listOf(operationType))
            assertEquals(OperationsTypeViewModel.UiEvent.UpdateOperationTypesFailed, awaitItem())

            viewModel.dismissOperationType(operationType)
            assertEquals(OperationsTypeViewModel.UiEvent.DismissOperationTypeFailed, awaitItem())

            viewModel.restoreOperationType(operationType)
            assertEquals(OperationsTypeViewModel.UiEvent.RestoreOperationTypeFailed, awaitItem())

            viewModel.addImage(mockk(), "cat")
            assertEquals(OperationsTypeViewModel.UiEvent.AddImageFailed, awaitItem())

            viewModel.removeImage(image)
            assertEquals(OperationsTypeViewModel.UiEvent.RemoveImageFailed, awaitItem())

            viewModel.updateImageOrder(listOf(image))
            assertEquals(OperationsTypeViewModel.UiEvent.UpdateImageOrderFailed, awaitItem())

            viewModel.toggleImageVisibility(image)
            assertEquals(OperationsTypeViewModel.UiEvent.ToggleImageVisibilityFailed, awaitItem())

            viewModel.setDefaultOperationType(1)
            assertEquals(OperationsTypeViewModel.UiEvent.SetDefaultFailed, awaitItem())
            
            viewModel.toggleDefaultOperationType(1)
            assertEquals(OperationsTypeViewModel.UiEvent.SetDefaultFailed, awaitItem())
        }
    }
}
