package com.moxmose.moxequiplog.ui.reports

import com.moxmose.moxequiplog.data.local.EquipmentDao
import com.moxmose.moxequiplog.data.local.MaintenanceLogDao
import com.moxmose.moxequiplog.data.local.OperationTypeDao
import com.moxmose.moxequiplog.data.local.MeasurementUnitDao
import com.moxmose.moxequiplog.data.local.ReportFilterDao
import com.moxmose.moxequiplog.data.ImageRepository
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.MaintenanceManager
import com.moxmose.moxequiplog.data.local.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var appSettingsManager: AppSettingsManager
    private lateinit var viewModel: ReportsViewModel

    @Before
    fun setup() {
        stopKoin() // Assicura che Koin sia pulito prima di iniziare
        Dispatchers.setMain(testDispatcher)
        appSettingsManager = mockk(relaxed = true) {
            every { reportsColorMode } returns MutableStateFlow("auto")
            every { reportsCustomColors } returns MutableStateFlow(null)
        }
        
        val equipmentDao = mockk<EquipmentDao>(relaxed = true) {
            every { getActiveEquipments() } returns flowOf(emptyList())
            every { getAllEquipments() } returns flowOf(emptyList())
        }
        val operationTypeDao = mockk<OperationTypeDao>(relaxed = true) {
            every { getActiveOperationTypes() } returns flowOf(emptyList())
            every { getAllOperationTypes() } returns flowOf(emptyList())
        }
        val maintenanceLogDao = mockk<MaintenanceLogDao>(relaxed = true) {
            every { getLogsWithDetails(any()) } returns flowOf(emptyList())
        }
        val measurementUnitDao = mockk<MeasurementUnitDao>(relaxed = true) {
            every { getAllUnits() } returns flowOf(emptyList())
        }
        val reportFilterDao = mockk<ReportFilterDao>(relaxed = true) {
            every { getLastSession(any()) } returns flowOf(null)
            every { getSavedFilters(any()) } returns flowOf(emptyList())
        }
        val imageRepository = mockk<ImageRepository>(relaxed = true) {
            every { allColorsForReports } returns flowOf(emptyList())
            every { getCategoryColor(any()) } returns flowOf(null)
        }
        val maintenanceManager = mockk<MaintenanceManager>(relaxed = true)

        viewModel = ReportsViewModel(
            equipmentDao = equipmentDao,
            maintenanceLogDao = maintenanceLogDao,
            operationTypeDao = operationTypeDao,
            measurementUnitDao = measurementUnitDao,
            imageRepository = imageRepository,
            appSettingsManager = appSettingsManager,
            reportFilterDao = reportFilterDao,
            maintenanceManager = maintenanceManager
        )
    }

    @Test
    fun `viewModel initializes correctly`() {
        // Just verify basic state
        assertNotNull(viewModel.uiState)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
