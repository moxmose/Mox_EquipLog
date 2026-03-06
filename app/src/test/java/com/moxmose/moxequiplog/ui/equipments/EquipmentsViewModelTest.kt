package com.moxmose.moxequiplog.ui.equipments

import androidx.compose.ui.test.junit4.createComposeRule
import app.cash.turbine.test
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.ImageRepository
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.Equipment
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
class EquipmentsViewModelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var equipmentDao: EquipmentDao
    private lateinit var imageRepository: ImageRepository
    private lateinit var appSettingsManager: AppSettingsManager
    private lateinit var viewModel: EquipmentsViewModel

    private val activeEquipmentsFlow = MutableStateFlow<List<Equipment>>(emptyList())
    private val allEquipmentsFlow = MutableStateFlow<List<Equipment>>(emptyList())
    private val equipmentImagesFlow = MutableStateFlow<List<Image>>(emptyList())
    private val defaultEquipmentIdFlow = MutableStateFlow<Int?>(null)
    private val allCategoriesFlow = MutableStateFlow<List<Category>>(emptyList())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        equipmentDao = mockk(relaxed = true) {
            every { getActiveEquipments() } returns activeEquipmentsFlow
            every { getAllEquipments() } returns allEquipmentsFlow
        }
        imageRepository = mockk(relaxed = true) {
            every { getImagesByCategory("EQUIPMENT") } returns equipmentImagesFlow
            every { allCategories } returns allCategoriesFlow
            every { getCategoryColor("EQUIPMENT") } returns MutableStateFlow("#808080")
            every { getCategoryDefaultIcon("EQUIPMENT") } returns MutableStateFlow("default_icon")
            every { getCategoryDefaultPhoto("EQUIPMENT") } returns MutableStateFlow("default_photo")
        }
        appSettingsManager = mockk(relaxed = true) {
            every { defaultEquipmentId } returns defaultEquipmentIdFlow
        }
        viewModel = EquipmentsViewModel(equipmentDao, imageRepository, appSettingsManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun categoryGetters_delegateToRepository() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.categoryColor.collect {} }
        backgroundScope.launch(testDispatcher) { viewModel.categoryDefaultIcon.collect {} }
        backgroundScope.launch(testDispatcher) { viewModel.categoryDefaultPhoto.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("#808080", viewModel.categoryColor.value)
        assertEquals("default_icon", viewModel.categoryDefaultIcon.value)
        assertEquals("default_photo", viewModel.categoryDefaultPhoto.value)
    }

    @Test
    fun allCategories_emitsData() = runTest {
        viewModel.allCategories.test {
            assertEquals(emptyList<Category>(), awaitItem())
            val list = listOf(Category("C1", "Cat 1"))
            allCategoriesFlow.value = list
            assertEquals(list, awaitItem())
        }
    }

    @Test
    fun addEquipment_withValidData_callsDao() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.allEquipments.collect {} }
        val description = "New Equipment"
        allEquipmentsFlow.value = listOf(Equipment(id = 1, description = "E1", displayOrder = 0))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.addEquipment(description, ImageIdentifier.Icon("some_icon"))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { equipmentDao.insertEquipment(match { it.description == description && it.displayOrder == 1 }) }
    }

    @Test
    fun addEquipment_whenDaoThrowsError_sendsUiEvent() = runTest {
        coEvery { equipmentDao.insertEquipment(any()) } throws RuntimeException()
        viewModel.uiEvents.test {
            viewModel.addEquipment("Valid", null)
            assertEquals(EquipmentsViewModel.UiEvent.AddEquipmentFailed, awaitItem())
        }
    }

    @Test
    fun updateEquipment_whenDaoThrowsError_sendsUiEvent() = runTest {
        coEvery { equipmentDao.updateEquipment(any()) } throws RuntimeException()
        viewModel.uiEvents.test {
            viewModel.updateEquipment(mockk())
            assertEquals(EquipmentsViewModel.UiEvent.UpdateEquipmentFailed, awaitItem())
        }
    }

    @Test
    fun updateEquipments_whenDaoThrowsError_sendsUiEvent() = runTest {
        coEvery { equipmentDao.updateEquipments(any()) } throws RuntimeException()
        viewModel.uiEvents.test {
            viewModel.updateEquipments(listOf(mockk()))
            assertEquals(EquipmentsViewModel.UiEvent.UpdateEquipmentsFailed, awaitItem())
        }
    }

    @Test
    fun dismissEquipment_whenDaoThrowsError_sendsUiEvent() = runTest {
        coEvery { equipmentDao.updateEquipment(any()) } throws RuntimeException()
        viewModel.uiEvents.test {
            viewModel.dismissEquipment(mockk())
            assertEquals(EquipmentsViewModel.UiEvent.DismissEquipmentFailed, awaitItem())
        }
    }

    @Test
    fun restoreEquipment_whenDaoThrowsError_sendsUiEvent() = runTest {
        coEvery { equipmentDao.updateEquipment(any()) } throws RuntimeException()
        viewModel.uiEvents.test {
            viewModel.restoreEquipment(mockk())
            assertEquals(EquipmentsViewModel.UiEvent.RestoreEquipmentFailed, awaitItem())
        }
    }

    @Test
    fun addImage_whenRepositoryThrowsError_sendsUiEvent() = runTest {
        coEvery { imageRepository.addImage(any(), any()) } throws RuntimeException()
        viewModel.uiEvents.test {
            viewModel.addImage(mockk(), "cat")
            assertEquals(EquipmentsViewModel.UiEvent.AddImageFailed, awaitItem())
        }
    }

    @Test
    fun removeImage_whenRepositoryThrowsError_sendsUiEvent() = runTest {
        coEvery { imageRepository.removeImage(any()) } throws RuntimeException()
        viewModel.uiEvents.test {
            viewModel.removeImage(mockk())
            assertEquals(EquipmentsViewModel.UiEvent.RemoveImageFailed, awaitItem())
        }
    }

    @Test
    fun updateImageOrder_whenRepositoryThrowsError_sendsUiEvent() = runTest {
        coEvery { imageRepository.updateImageOrder(any()) } throws RuntimeException()
        viewModel.uiEvents.test {
            viewModel.updateImageOrder(listOf(mockk()))
            assertEquals(EquipmentsViewModel.UiEvent.UpdateImageOrderFailed, awaitItem())
        }
    }

    @Test
    fun toggleImageVisibility_whenRepositoryThrowsError_sendsUiEvent() = runTest {
        coEvery { imageRepository.toggleImageVisibility(any()) } throws RuntimeException()
        viewModel.uiEvents.test {
            viewModel.toggleImageVisibility(mockk())
            assertEquals(EquipmentsViewModel.UiEvent.ToggleImageVisibilityFailed, awaitItem())
        }
    }

    @Test
    fun toggleDefaultEquipment_callsManager() = runTest {
        viewModel.toggleDefaultEquipment(10)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { appSettingsManager.setDefaultEquipmentId(10) }
    }

    @Test
    fun isPhotoUsed_returnsTrueWhenDaoThrowsError() = runTest {
        coEvery { equipmentDao.countEquipmentsUsingPhoto(any()) } throws RuntimeException()
        viewModel.uiEvents.test {
            assertTrue(viewModel.isPhotoUsed("uri"))
            assertEquals(EquipmentsViewModel.UiEvent.DatabaseCheckFailed, awaitItem())
        }
    }
}
