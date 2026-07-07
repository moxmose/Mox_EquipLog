package com.moxmose.moxequiplog.ui.reports

import com.moxmose.moxequiplog.data.local.Equipment
import com.moxmose.moxequiplog.data.local.OperationType
import com.moxmose.moxequiplog.data.local.EquipmentDao
import com.moxmose.moxequiplog.data.local.MaintenanceLogDao
import com.moxmose.moxequiplog.data.local.OperationTypeDao
import com.moxmose.moxequiplog.data.local.MeasurementUnitDao
import com.moxmose.moxequiplog.data.local.ReportFilterDao
import com.moxmose.moxequiplog.data.ImageRepository
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.MaintenanceManager
import com.moxmose.moxequiplog.data.SectionRepository
import com.moxmose.moxequiplog.data.local.TimeGranularity
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var appSettingsManager: AppSettingsManager
    private lateinit var viewModel: ReportsViewModel

    @Before
    fun setup() {
        stopKoin()
        Dispatchers.setMain(testDispatcher)
        appSettingsManager = mockk(relaxed = true) {
            every { reportsColorMode } returns MutableStateFlow("auto")
            every { reportsCustomColors } returns MutableStateFlow(emptyList())
            every { sectionSelectorType } returns MutableStateFlow("tabs")
        }
        
        val equipmentDao = mockk<EquipmentDao>(relaxed = true) {
            every { getActiveEquipmentList() } returns MutableStateFlow(listOf(Equipment(id = 1, description = "Eq 1")))
            every { getAllEquipmentList() } returns MutableStateFlow(listOf(Equipment(id = 1, description = "Eq 1")))
        }
        val operationTypeDao = mockk<OperationTypeDao>(relaxed = true) {
            every { getActiveOperationTypes() } returns MutableStateFlow(listOf(OperationType(id = 1, description = "Op 1")))
            every { getAllOperationTypes() } returns MutableStateFlow(listOf(OperationType(id = 1, description = "Op 1")))
        }
        val maintenanceLogDao = mockk<MaintenanceLogDao>(relaxed = true) {
            every { getLogsWithDetails(any()) } returns MutableStateFlow(emptyList())
        }
        val measurementUnitDao = mockk<MeasurementUnitDao>(relaxed = true) {
            every { getAllUnits() } returns MutableStateFlow(emptyList())
        }
        val reportFilterDao = mockk<ReportFilterDao>(relaxed = true) {
            every { getLastSession(any()) } returns MutableStateFlow(null)
            every { getSavedFilters(any()) } returns MutableStateFlow(emptyList())
        }
        val imageRepository = mockk<ImageRepository>(relaxed = true) {
            every { allColorsForReports } returns MutableStateFlow(emptyList())
            every { getCategoryColor(any()) } returns MutableStateFlow("#808080")
        }
        val maintenanceManager = mockk<MaintenanceManager>(relaxed = true) {
            every { findAutoGranularity(any(), any(), any()) } returns TimeGranularity.MONTHS
            every { findBestGranularity(any(), any(), any(), any()) } returns TimeGranularity.MONTHS
        }
        val sectionRepository = mockk<SectionRepository>(relaxed = true) {
            every { allSections } returns MutableStateFlow(emptyList())
        }

        viewModel = ReportsViewModel(
            equipmentDao = equipmentDao,
            maintenanceLogDao = maintenanceLogDao,
            operationTypeDao = operationTypeDao,
            measurementUnitDao = measurementUnitDao,
            imageRepository = imageRepository,
            appSettingsManager = appSettingsManager,
            reportFilterDao = reportFilterDao,
            maintenanceManager = maintenanceManager,
            resourceProvider = mockk(relaxed = true),
            sectionRepository = sectionRepository
        )
    }

    @Test
    fun `viewModel initializes correctly`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value)
        job.cancel()
    }

    @Test
    fun `toggleShowDismissed updates uiState`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        
        val initialState = viewModel.uiState.value.showDismissed
        viewModel.toggleShowDismissed()
        advanceUntilIdle()
        
        assertTrue("showDismissed should change from $initialState, but was ${viewModel.uiState.value.showDismissed}", viewModel.uiState.value.showDismissed != initialState)
        job.cancel()
    }

    @Test
    fun `setDateRange updates uiState`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val start = 1000L
        val end = 2000L
        viewModel.setDateRange(start, end)
        advanceUntilIdle()

        assertTrue("startDate should be $start, but was ${viewModel.uiState.value.startDate}", viewModel.uiState.value.startDate == start)
        assertTrue("endDate should be $end, but was ${viewModel.uiState.value.endDate}", viewModel.uiState.value.endDate == end)
        job.cancel()
    }

    @Test
    fun `toggleEquipmentSelection updates selected ids`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val id = 42
        viewModel.toggleEquipmentSelection(id)
        advanceUntilIdle()
        assertTrue("id $id should be in selectedEquipmentIds, current is ${viewModel.uiState.value.selectedEquipmentIds}", id in viewModel.uiState.value.selectedEquipmentIds)
        
        viewModel.toggleEquipmentSelection(id)
        advanceUntilIdle()
        assertTrue("id $id should NOT be in selectedEquipmentIds", id !in viewModel.uiState.value.selectedEquipmentIds)
        job.cancel()
    }

    @Test
    fun `resetFilters restores default state`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.toggleShowDismissed()
        viewModel.setDateRange(100L, 200L)
        advanceUntilIdle()
        
        viewModel.resetFilters()
        advanceUntilIdle()
        
        assertTrue("showDismissed should be false after reset, but was ${viewModel.uiState.value.showDismissed}", !viewModel.uiState.value.showDismissed)
        assertTrue("startDate should be null after reset, but was ${viewModel.uiState.value.startDate}", viewModel.uiState.value.startDate == null)
        assertTrue("endDate should be null after reset, but was ${viewModel.uiState.value.endDate}", viewModel.uiState.value.endDate == null)
        job.cancel()
    }

    @Test
    fun `onSectionSelected updates selected section id`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val sectionId = 5
        viewModel.onSectionSelected(sectionId)
        advanceUntilIdle()
        assertTrue("selectedSectionId should be $sectionId, but was ${viewModel.uiState.value.selectedSectionId}", viewModel.uiState.value.selectedSectionId == sectionId)
        job.cancel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
