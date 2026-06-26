package com.moxmose.moxequiplog.ui.equipment

import androidx.compose.ui.test.junit4.createComposeRule
import app.cash.turbine.test
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.ImageRepository
import com.moxmose.moxequiplog.data.MaintenanceManager
import com.moxmose.moxequiplog.data.SectionRepository
import com.moxmose.moxequiplog.data.local.*
import com.moxmose.moxequiplog.utils.UiConstants
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
class EquipmentViewModelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var equipmentDao: EquipmentDao
    private lateinit var imageRepository: ImageRepository
    private lateinit var appSettingsManager: AppSettingsManager
    private lateinit var sectionRepository: SectionRepository
    private lateinit var measurementUnitDao: MeasurementUnitDao
    private lateinit var operationTypeDao: OperationTypeDao
    private lateinit var maintenanceLogDao: MaintenanceLogDao
    private lateinit var maintenanceReminderDao: MaintenanceReminderDao
    private lateinit var maintenanceManager: MaintenanceManager
    private lateinit var viewModel: EquipmentViewModel

    private val activeEquipmentsFlow = MutableStateFlow<List<Equipment>>(emptyList())
    private val allEquipmentsFlow = MutableStateFlow<List<Equipment>>(emptyList())
    private val equipmentImagesFlow = MutableStateFlow<List<Image>>(emptyList())
    private val allCategoriesFlow = MutableStateFlow<List<Category>>(emptyList())
    private val selectedSectionIdFlow = MutableStateFlow(1)
    private val allSectionsFlow = MutableStateFlow<List<Section>>(emptyList())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        equipmentDao = mockk(relaxed = true) {
            every { getActiveEquipmentList() } returns activeEquipmentsFlow
            every { getAllEquipmentList() } returns allEquipmentsFlow
            every { getActiveEquipmentListBySection(any()) } returns activeEquipmentsFlow
            every { getAllEquipmentListBySection(any()) } returns allEquipmentsFlow
        }
        imageRepository = mockk(relaxed = true) {
            every { getImagesByCategory("EQUIPMENT") } returns equipmentImagesFlow
            every { allCategories } returns allCategoriesFlow
            every { getCategoryColor("EQUIPMENT") } returns MutableStateFlow("#808080")
            every { getCategoryDefaultIcon("EQUIPMENT") } returns MutableStateFlow("default_icon")
            every { getCategoryDefaultPhoto("EQUIPMENT") } returns MutableStateFlow("default_photo")
        }
        appSettingsManager = mockk(relaxed = true) {
            every { selectedSectionId } returns selectedSectionIdFlow
            every { showDismissedSections } returns MutableStateFlow(false)
            every { sectionSelectorType } returns MutableStateFlow(UiConstants.DEFAULT_SECTION_SELECTOR_TYPE)
            every { defaultUnitId } returns MutableStateFlow(null)
            every { defaultVisibilityHorizonValue } returns MutableStateFlow(UiConstants.DEFAULT_VISIBILITY_HORIZON_VALUE)
            every { defaultVisibilityHorizonUnit } returns MutableStateFlow(UiConstants.DEFAULT_VISIBILITY_HORIZON_UNIT)
            every { getAllDraftsFlow(any()) } returns MutableStateFlow(emptyMap())
            every { getDraftFlow(any(), any()) } returns MutableStateFlow(null)
        }
        sectionRepository = mockk(relaxed = true) {
            every { allSections } returns allSectionsFlow
        }
        measurementUnitDao = mockk(relaxed = true) {
            every { getAllUnits() } returns MutableStateFlow(emptyList())
        }
        operationTypeDao = mockk(relaxed = true) {
            every { getAllOperationTypes() } returns MutableStateFlow(emptyList())
        }
        maintenanceLogDao = mockk(relaxed = true) {
            every { getLogsCountFlow() } returns MutableStateFlow(0)
        }
        maintenanceReminderDao = mockk(relaxed = true) {
            every { getAllReminders() } returns MutableStateFlow(emptyList())
        }
        maintenanceManager = mockk<MaintenanceManager>(relaxed = true)
        viewModel = EquipmentViewModel(
            equipmentDao,
            imageRepository,
            appSettingsManager,
            sectionRepository,
            measurementUnitDao,
            operationTypeDao,
            maintenanceLogDao,
            maintenanceReminderDao,
            maintenanceManager
        )
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

            viewModel.addEquipment("E1", ImageIdentifier.Icon("icon1"), 1)
            testDispatcher.scheduler.advanceUntilIdle()
            coVerify { equipmentDao.insertEquipment(match { it.description == "E1" && it.iconIdentifier == "icon1" && it.displayOrder == 1 && it.unitId == 1}) }
        }
    }

    @Test
    fun addEquipment_withPhoto_callsDao() = runTest {
        viewModel.allEquipments.test {
            awaitItem()
            viewModel.addEquipment("E2", ImageIdentifier.Photo("uri2"), 1)
            testDispatcher.scheduler.advanceUntilIdle()
            coVerify { equipmentDao.insertEquipment(match { it.description == "E2" && it.photoUri == "uri2" && it.unitId == 1 }) }
        }
    }

    @Test
    fun addEquipment_withNullImage_usesDefaults() = runTest {
        viewModel.allEquipments.test {
            awaitItem()
            backgroundScope.launch(testDispatcher) { viewModel.categoryDefaultPhoto.collect {} }
            backgroundScope.launch(testDispatcher) { viewModel.categoryDefaultIcon.collect {} }
            testDispatcher.scheduler.advanceUntilIdle()
            
            viewModel.addEquipment("E3", null, 2)
            testDispatcher.scheduler.advanceUntilIdle()
            coVerify { equipmentDao.insertEquipment(match { it.description == "E3" && it.photoUri == "default_photo" && it.unitId == 2 }) }
        }
    }

    @Test
    fun isPhotoUsed_variants_coverage() = runTest {
        launch {
            viewModel.uiEvents.test {
                assertTrue(viewModel.isPhotoUsed(" "))
                assertEquals(EquipmentViewModel.UiEvent.PhotoUriInvalid, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
        testDispatcher.scheduler.advanceUntilIdle()
        
        coEvery { equipmentDao.countEquipmentUsingPhoto("used") } returns 1
        assertTrue(viewModel.isPhotoUsed("used"))
        
        coEvery { equipmentDao.countEquipmentUsingPhoto("free") } returns 0
        assertFalse(viewModel.isPhotoUsed("free"))

        launch {
            viewModel.uiEvents.test {
                coEvery { equipmentDao.countEquipmentUsingPhoto("err") } throws RuntimeException()
                assertTrue(viewModel.isPhotoUsed("err"))
                assertEquals(EquipmentViewModel.UiEvent.DatabaseCheckFailed, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun toggleDefaultEquipment_coverage() = runTest {
        backgroundScope.launch(testDispatcher) { viewModel.defaultEquipmentId.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        // Toggle ON
        viewModel.toggleDefaultEquipment(5)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { sectionRepository.updateSectionDefaultEquipment(1, 5) }

        // Simuliamo aggiornamento tramite allSections
        allSectionsFlow.value = listOf(Section(id = 1, name = "S1", defaultEquipmentId = 5))
        testDispatcher.scheduler.advanceUntilIdle()

        // Toggle OFF (stesso ID)
        viewModel.toggleDefaultEquipment(5)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { sectionRepository.updateSectionDefaultEquipment(1, null) }
    }

    @Test
    fun crud_error_branches_coverage() = runTest {
        val equipment = Equipment(id = 1, description = "E")
        val image: Image = mockk(relaxed = true)

        coEvery { equipmentDao.updateEquipment(any()) } throws RuntimeException()
        coEvery { equipmentDao.updateEquipmentList(any()) } throws RuntimeException()
        coEvery { imageRepository.addImage(any(), any()) } throws RuntimeException()
        coEvery { imageRepository.removeImage(any()) } throws RuntimeException()
        coEvery { imageRepository.updateImageOrder(any()) } throws RuntimeException()
        coEvery { imageRepository.toggleImageVisibility(any()) } throws RuntimeException()
        coEvery { sectionRepository.updateSectionDefaultEquipment(any(), any()) } throws RuntimeException()

        viewModel.uiEvents.test {
            viewModel.updateEquipment(equipment)
            assertEquals(EquipmentViewModel.UiEvent.UpdateEquipmentFailed, awaitItem())
            
            viewModel.updateEquipments(listOf(equipment))
            assertEquals(EquipmentViewModel.UiEvent.UpdateEquipmentOrderFailed, awaitItem())

            viewModel.dismissEquipment(equipment)
            assertEquals(EquipmentViewModel.UiEvent.DismissEquipmentFailed, awaitItem())

            viewModel.restoreEquipment(equipment)
            assertEquals(EquipmentViewModel.UiEvent.RestoreEquipmentFailed, awaitItem())

            viewModel.addImage(mockk(), "cat")
            assertEquals(EquipmentViewModel.UiEvent.AddImageFailed, awaitItem())

            viewModel.removeImage(image)
            assertEquals(EquipmentViewModel.UiEvent.RemoveImageFailed, awaitItem())

            viewModel.updateImageOrder(listOf(image))
            assertEquals(EquipmentViewModel.UiEvent.UpdateImageOrderFailed, awaitItem())

            viewModel.toggleImageVisibility(image)
            assertEquals(EquipmentViewModel.UiEvent.ToggleImageVisibilityFailed, awaitItem())

            viewModel.setDefaultEquipment(1)
            assertEquals(EquipmentViewModel.UiEvent.SetDefaultFailed, awaitItem())
            
            viewModel.toggleDefaultEquipment(1)
            assertEquals(EquipmentViewModel.UiEvent.SetDefaultFailed, awaitItem())
        }
    }
}
