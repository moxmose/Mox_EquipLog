package com.moxmose.moxequiplog.ui.equipments

import androidx.compose.ui.test.junit4.createComposeRule
import app.cash.turbine.test
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
    private lateinit var viewModel: EquipmentsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        equipmentDao = mockk(relaxed = true) {
            every { getActiveEquipments() } returns MutableStateFlow(emptyList())
            every { getAllEquipments() } returns MutableStateFlow(emptyList())
        }
        imageRepository = mockk(relaxed = true) {
            every { getImagesByCategory("EQUIPMENT") } returns MutableStateFlow(emptyList())
            every { allCategories } returns MutableStateFlow(emptyList())
        }
        viewModel = EquipmentsViewModel(equipmentDao, imageRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun activeEquipments_onInit_isEmpty() = runTest {
        viewModel.activeEquipments.test {
            assertEquals(emptyList<Equipment>(), awaitItem())
        }
    }

    @Test
    fun allEquipments_onInit_isEmpty() = runTest {
        viewModel.allEquipments.test {
            assertEquals(emptyList<Equipment>(), awaitItem())
        }
    }

    @Test
    fun equipmentImages_onInit_isEmpty() = runTest {
        viewModel.equipmentImages.test {
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
    fun addEquipment_withBlankDescription_sendsDescriptionInvalidEvent() = runTest {
        viewModel.uiEvents.test {
            viewModel.addEquipment(" ", null)
            assertEquals(EquipmentsViewModel.UiEvent.DescriptionInvalid, awaitItem())
        }
    }

    @Test
    fun addEquipment_withBlankDescription_doesNotInsertEquipment() = runTest {
        viewModel.addEquipment(" ", null)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 0) { equipmentDao.insertEquipment(any()) }
    }

    @Test
    fun addEquipment_withValidData_callsDao() = runTest {
        val description = "New Equipment"
        val imageIdentifier = ImageIdentifier.Icon("some_icon")

        val equipmentCategory = Category(id = "EQUIPMENT", name = "Equipment", color = "#FFFFFF", defaultIconIdentifier = "default_icon")
        every { imageRepository.allCategories } returns MutableStateFlow(listOf(equipmentCategory))


        viewModel.addEquipment(description, imageIdentifier)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { equipmentDao.insertEquipment(any()) }
    }

    @Test
    fun addEquipment_whenDaoThrowsError_sendsAddEquipmentFailedEvent() = runTest {
        coEvery { equipmentDao.insertEquipment(any()) } throws RuntimeException("DB error")
        viewModel.uiEvents.test {
            viewModel.addEquipment("description", null)
            assertEquals(EquipmentsViewModel.UiEvent.AddEquipmentFailed, awaitItem())
        }
    }

    @Test
    fun updateEquipment_whenDaoThrowsError_sendsUpdateEquipmentFailedEvent() = runTest {
        coEvery { equipmentDao.updateEquipment(any()) } throws RuntimeException("DB error")
        viewModel.uiEvents.test {
            viewModel.updateEquipment(mockk())
            assertEquals(EquipmentsViewModel.UiEvent.UpdateEquipmentFailed, awaitItem())
        }
    }

    @Test
    fun updateEquipments_withEmptyList_doesNothing() = runTest {
        viewModel.updateEquipments(emptyList())
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 0) { equipmentDao.updateEquipments(any()) }
    }

    @Test
    fun updateEquipments_whenDaoThrowsError_sendsUpdateEquipmentsFailedEvent() = runTest {
        coEvery { equipmentDao.updateEquipments(any()) } throws RuntimeException("DB error")
        viewModel.uiEvents.test {
            viewModel.updateEquipments(listOf(mockk()))
            assertEquals(EquipmentsViewModel.UiEvent.UpdateEquipmentsFailed, awaitItem())
        }
    }

    @Test
    fun dismissEquipment_whenDaoThrowsError_sendsDismissEquipmentFailedEvent() = runTest {
        coEvery { equipmentDao.updateEquipment(any()) } throws RuntimeException("DB error")
        viewModel.uiEvents.test {
            viewModel.dismissEquipment(mockk())
            assertEquals(EquipmentsViewModel.UiEvent.DismissEquipmentFailed, awaitItem())
        }
    }

    @Test
    fun restoreEquipment_whenDaoThrowsError_sendsRestoreEquipmentFailedEvent() = runTest {
        coEvery { equipmentDao.updateEquipment(any()) } throws RuntimeException("DB error")
        viewModel.uiEvents.test {
            viewModel.restoreEquipment(mockk())
            assertEquals(EquipmentsViewModel.UiEvent.RestoreEquipmentFailed, awaitItem())
        }
    }

    @Test
    fun addImage_whenRepositoryThrowsError_sendsAddImageFailedEvent() = runTest {
        coEvery { imageRepository.addImage(any(), any()) } throws RuntimeException("DB error")
        viewModel.uiEvents.test {
            viewModel.addImage(mockk(), "category")
            assertEquals(EquipmentsViewModel.UiEvent.AddImageFailed, awaitItem())
        }
    }

    @Test
    fun removeImage_whenRepositoryThrowsError_sendsRemoveImageFailedEvent() = runTest {
        coEvery { imageRepository.removeImage(any()) } throws RuntimeException("DB error")
        viewModel.uiEvents.test {
            viewModel.removeImage(mockk())
            assertEquals(EquipmentsViewModel.UiEvent.RemoveImageFailed, awaitItem())
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
            assertEquals(EquipmentsViewModel.UiEvent.UpdateImageOrderFailed, awaitItem())
        }
    }

    @Test
    fun toggleImageVisibility_whenRepositoryThrowsError_sendsToggleImageVisibilityFailedEvent() = runTest {
        coEvery { imageRepository.toggleImageVisibility(any()) } throws RuntimeException("DB error")
        viewModel.uiEvents.test {
            viewModel.toggleImageVisibility(mockk())
            assertEquals(EquipmentsViewModel.UiEvent.ToggleImageVisibilityFailed, awaitItem())
        }
    }

    @Test
    fun isPhotoUsed_whenUriIsBlank_sendsUriInvalidEventAndReturnsTrue() = runTest {
        viewModel.uiEvents.test {
            val result = viewModel.isPhotoUsed(" ")
            assertTrue(result)
            assertEquals(EquipmentsViewModel.UiEvent.PhotoUriInvalid, awaitItem())
        }
    }

    @Test
    fun isPhotoUsed_whenDaoThrowsError_sendsCheckFailedEventAndReturnsTrue() = runTest {
        val uri = "test_uri"
        coEvery { equipmentDao.countEquipmentsUsingPhoto(uri) } throws RuntimeException()
        viewModel.uiEvents.test {
            val result = viewModel.isPhotoUsed(uri)
            assertTrue(result)
            assertEquals(EquipmentsViewModel.UiEvent.DatabaseCheckFailed, awaitItem())
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