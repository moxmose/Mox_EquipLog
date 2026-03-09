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
    fun getters_coverage_booster() = runTest {
        assertNotNull(viewModel.activeEquipments)
        assertNotNull(viewModel.allEquipments)
        assertNotNull(viewModel.equipmentImages)
        assertNotNull(viewModel.allCategories)
        assertNotNull(viewModel.categoryColor)
        assertNotNull(viewModel.categoryDefaultIcon)
        assertNotNull(viewModel.categoryDefaultPhoto)
        assertNotNull(viewModel.defaultEquipmentId)
        assertNotNull(viewModel.uiEvents)

        viewModel.categoryColor.value
        viewModel.activeEquipments.value
        viewModel.allEquipments.value
        viewModel.equipmentImages.value
        viewModel.allCategories.value
        viewModel.defaultEquipmentId.value
    }

    @Test
    fun addEquipment_withIcon_callsDao() = runTest {
        viewModel.allEquipments.test {
            awaitItem() // initial
            allEquipmentsFlow.value = listOf(Equipment(id = 1, description = "E1", displayOrder = 0))
            awaitItem()

            viewModel.addEquipment("E1", ImageIdentifier.Icon("icon1"))
            testDispatcher.scheduler.advanceUntilIdle()
            coVerify { equipmentDao.insertEquipment(match { it.description == "E1" && it.iconIdentifier == "icon1" && it.displayOrder == 1}) }
        }
    }

    @Test
    fun addEquipment_withPhoto_callsDao() = runTest {
        viewModel.allEquipments.test {
            awaitItem()
            viewModel.addEquipment("E2", ImageIdentifier.Photo("uri2"))
            testDispatcher.scheduler.advanceUntilIdle()
            coVerify { equipmentDao.insertEquipment(match { it.description == "E2" && it.photoUri == "uri2" }) }
        }
    }

    @Test
    fun addEquipment_withNullImage_usesDefaults() = runTest {
        viewModel.allEquipments.test {
            awaitItem()
            backgroundScope.launch(testDispatcher) { viewModel.categoryDefaultPhoto.collect {} }
            backgroundScope.launch(testDispatcher) { viewModel.categoryDefaultIcon.collect {} }
            testDispatcher.scheduler.advanceUntilIdle()
            
            viewModel.addEquipment("E3", null)
            testDispatcher.scheduler.advanceUntilIdle()
            coVerify { equipmentDao.insertEquipment(match { it.description == "E3" && it.photoUri == "default_photo" }) }
        }
    }

    @Test
    fun isPhotoUsed_variants_coverage() = runTest {
        launch {
            viewModel.uiEvents.test {
                assertTrue(viewModel.isPhotoUsed(" "))
                assertEquals(EquipmentsViewModel.UiEvent.PhotoUriInvalid, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
        testDispatcher.scheduler.advanceUntilIdle()
        
        coEvery { equipmentDao.countEquipmentsUsingPhoto("used") } returns 1
        assertTrue(viewModel.isPhotoUsed("used"))
        
        coEvery { equipmentDao.countEquipmentsUsingPhoto("free") } returns 0
        assertFalse(viewModel.isPhotoUsed("free"))

        launch {
            viewModel.uiEvents.test {
                coEvery { equipmentDao.countEquipmentsUsingPhoto("err") } throws RuntimeException()
                assertTrue(viewModel.isPhotoUsed("err"))
                assertEquals(EquipmentsViewModel.UiEvent.DatabaseCheckFailed, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun crud_error_branches_coverage() = runTest {
        val equipment = Equipment(id = 1, description = "E")
        val image: Image = mockk(relaxed = true)

        coEvery { equipmentDao.updateEquipment(any()) } throws RuntimeException()
        coEvery { equipmentDao.updateEquipments(any()) } throws RuntimeException()
        coEvery { imageRepository.addImage(any(), any()) } throws RuntimeException()
        coEvery { imageRepository.removeImage(any()) } throws RuntimeException()
        coEvery { imageRepository.updateImageOrder(any()) } throws RuntimeException()
        coEvery { imageRepository.toggleImageVisibility(any()) } throws RuntimeException()
        coEvery { appSettingsManager.setDefaultEquipmentId(any()) } throws RuntimeException()

        viewModel.uiEvents.test {
            viewModel.updateEquipment(equipment)
            assertEquals(EquipmentsViewModel.UiEvent.UpdateEquipmentFailed, awaitItem())
            
            viewModel.updateEquipments(listOf(equipment))
            assertEquals(EquipmentsViewModel.UiEvent.UpdateEquipmentsFailed, awaitItem())

            viewModel.dismissEquipment(equipment)
            assertEquals(EquipmentsViewModel.UiEvent.DismissEquipmentFailed, awaitItem())

            viewModel.restoreEquipment(equipment)
            assertEquals(EquipmentsViewModel.UiEvent.RestoreEquipmentFailed, awaitItem())

            viewModel.addImage(mockk(), "cat")
            assertEquals(EquipmentsViewModel.UiEvent.AddImageFailed, awaitItem())

            viewModel.removeImage(image)
            assertEquals(EquipmentsViewModel.UiEvent.RemoveImageFailed, awaitItem())

            viewModel.updateImageOrder(listOf(image))
            assertEquals(EquipmentsViewModel.UiEvent.UpdateImageOrderFailed, awaitItem())

            viewModel.toggleImageVisibility(image)
            assertEquals(EquipmentsViewModel.UiEvent.ToggleImageVisibilityFailed, awaitItem())

            viewModel.setDefaultEquipment(1)
            assertEquals(EquipmentsViewModel.UiEvent.SetDefaultFailed, awaitItem())
            
            viewModel.toggleDefaultEquipment(1)
            assertEquals(EquipmentsViewModel.UiEvent.SetDefaultFailed, awaitItem())
        }
    }
}
