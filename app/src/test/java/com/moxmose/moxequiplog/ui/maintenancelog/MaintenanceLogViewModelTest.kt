package com.moxmose.moxequiplog.ui.maintenancelog

import androidx.compose.ui.test.junit4.createComposeRule
import app.cash.turbine.test
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.local.CategoryDao
import com.moxmose.moxequiplog.data.local.EquipmentDao
import com.moxmose.moxequiplog.data.local.MaintenanceLog
import com.moxmose.moxequiplog.data.local.MaintenanceLogDao
import com.moxmose.moxequiplog.data.local.MaintenanceLogDetails
import com.moxmose.moxequiplog.data.local.OperationTypeDao
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

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class MaintenanceLogViewModelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var maintenanceLogDao: MaintenanceLogDao
    private lateinit var equipmentDao: EquipmentDao
    private lateinit var operationTypeDao: OperationTypeDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var appSettingsManager: AppSettingsManager
    private lateinit var viewModel: MaintenanceLogViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        maintenanceLogDao = mockk(relaxed = true) {
            every { getLogsWithDetails(any()) } returns MutableStateFlow(emptyList())
        }
        equipmentDao = mockk(relaxed = true) {
            every { getAllEquipments() } returns MutableStateFlow(emptyList())
        }
        operationTypeDao = mockk(relaxed = true) {
            every { getAllOperationTypes() } returns MutableStateFlow(emptyList())
        }
        categoryDao = mockk(relaxed = true) {
            every { getAllCategories() } returns MutableStateFlow(emptyList())
        }
        appSettingsManager = mockk(relaxed = true) {
            every { defaultEquipmentId } returns MutableStateFlow(null)
            every { defaultOperationTypeId } returns MutableStateFlow(null)
        }
        viewModel = MaintenanceLogViewModel(maintenanceLogDao, equipmentDao, operationTypeDao, categoryDao, appSettingsManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun initialState_isCorrect() = runTest {
        assertEquals("", viewModel.searchQuery.value)
        assertEquals(SortProperty.DATE, viewModel.sortProperty.value)
        assertEquals(SortDirection.DESCENDING, viewModel.sortDirection.value)
        assertEquals(false, viewModel.showDismissed.value)
    }

    @Test
    fun onSearchQueryChanged_updatesState() = runTest {
        val query = "test query"
        viewModel.onSearchQueryChanged(query)
        assertEquals(query, viewModel.searchQuery.value)
    }

    @Test
    fun onSortPropertyChanged_updatesState() = runTest {
        val property = SortProperty.EQUIPMENT
        viewModel.onSortPropertyChanged(property)
        assertEquals(property, viewModel.sortProperty.value)
    }

    @Test
    fun onSortDirectionChanged_togglesState() = runTest {
        assertEquals(SortDirection.DESCENDING, viewModel.sortDirection.value)
        viewModel.onSortDirectionChanged()
        assertEquals(SortDirection.ASCENDING, viewModel.sortDirection.value)
        viewModel.onSortDirectionChanged()
        assertEquals(SortDirection.DESCENDING, viewModel.sortDirection.value)
    }

    @Test
    fun onShowDismissedToggled_togglesState() = runTest {
        assertEquals(false, viewModel.showDismissed.value)
        viewModel.onShowDismissedToggled()
        assertEquals(true, viewModel.showDismissed.value)
        viewModel.onShowDismissedToggled()
        assertEquals(false, viewModel.showDismissed.value)
    }

    @Test
    fun addLog_withValidData_callsDao() = runTest {
        viewModel.addLog(1, 1, "notes", 100, 123456789L, "#FFFFFF")
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { maintenanceLogDao.insertLog(any()) }
    }

    @Test
    fun addLog_whenDaoThrowsError_sendsAddLogFailedEvent() = runTest {
        coEvery { maintenanceLogDao.insertLog(any()) } throws RuntimeException("DB error")
        viewModel.uiEvents.test {
            viewModel.addLog(1, 1, "notes", 100, 123456789L, "#FFFFFF")
            assertEquals(MaintenanceLogViewModel.UiEvent.AddLogFailed, awaitItem())
        }
    }

    @Test
    fun updateLog_withValidData_callsDao() = runTest {
        val log = MaintenanceLog(id = 1, equipmentId = 1, operationTypeId = 1, date = 123L)
        viewModel.updateLog(log)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { maintenanceLogDao.updateLog(log) }
    }

    @Test
    fun updateLog_whenDaoThrowsError_sendsUpdateLogFailedEvent() = runTest {
        val log = MaintenanceLog(id = 1, equipmentId = 1, operationTypeId = 1, date = 123L)
        coEvery { maintenanceLogDao.updateLog(any()) } throws RuntimeException("DB error")
        viewModel.uiEvents.test {
            viewModel.updateLog(log)
            assertEquals(MaintenanceLogViewModel.UiEvent.UpdateLogFailed, awaitItem())
        }
    }

    @Test
    fun dismissLog_withValidData_callsDao() = runTest {
        val log = MaintenanceLog(id = 1, equipmentId = 1, operationTypeId = 1, date = 123L, dismissed = false)
        viewModel.dismissLog(log)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { maintenanceLogDao.updateLog(match { it.id == 1 && it.dismissed }) }
    }

    @Test
    fun dismissLog_whenDaoThrowsError_sendsDismissLogFailedEvent() = runTest {
        val log = MaintenanceLog(id = 1, equipmentId = 1, operationTypeId = 1, date = 123L)
        coEvery { maintenanceLogDao.updateLog(any()) } throws RuntimeException("DB error")
        viewModel.uiEvents.test {
            viewModel.dismissLog(log)
            assertEquals(MaintenanceLogViewModel.UiEvent.DismissLogFailed, awaitItem())
        }
    }

    @Test
    fun restoreLog_withValidData_callsDao() = runTest {
        val log = MaintenanceLog(id = 1, equipmentId = 1, operationTypeId = 1, date = 123L, dismissed = true)
        viewModel.restoreLog(log)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { maintenanceLogDao.updateLog(match { it.id == 1 && !it.dismissed }) }
    }

    @Test
    fun restoreLog_whenDaoThrowsError_sendsRestoreLogFailedEvent() = runTest {
        val log = MaintenanceLog(id = 1, equipmentId = 1, operationTypeId = 1, date = 123L)
        coEvery { maintenanceLogDao.updateLog(any()) } throws RuntimeException("DB error")
        viewModel.uiEvents.test {
            viewModel.restoreLog(log)
            assertEquals(MaintenanceLogViewModel.UiEvent.RestoreLogFailed, awaitItem())
        }
    }
}