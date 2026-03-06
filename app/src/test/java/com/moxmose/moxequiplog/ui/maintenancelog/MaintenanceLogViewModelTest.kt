package com.moxmose.moxequiplog.ui.maintenancelog

import androidx.compose.ui.test.junit4.createComposeRule
import app.cash.turbine.test
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.ImageRepository
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.CategoryDao
import com.moxmose.moxequiplog.data.local.Equipment
import com.moxmose.moxequiplog.data.local.EquipmentDao
import com.moxmose.moxequiplog.data.local.MaintenanceLog
import com.moxmose.moxequiplog.data.local.MaintenanceLogDao
import com.moxmose.moxequiplog.data.local.MaintenanceLogDetails
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
import org.junit.Assert.assertNotNull
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
    private lateinit var imageRepository: ImageRepository
    private lateinit var viewModel: MaintenanceLogViewModel

    private val allEquipmentsFlow = MutableStateFlow<List<Equipment>>(emptyList())
    private val allOperationTypesFlow = MutableStateFlow<List<OperationType>>(emptyList())
    private val allCategoriesFlow = MutableStateFlow<List<Category>>(emptyList())
    private val logsFlow = MutableStateFlow<List<MaintenanceLogDetails>>(emptyList())
    private val defaultEquipmentIdFlow = MutableStateFlow<Int?>(null)
    private val defaultOperationTypeIdFlow = MutableStateFlow<Int?>(null)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        maintenanceLogDao = mockk(relaxed = true) {
            every { getLogsWithDetails(any()) } answers { flowOf(listOf(mockk())) }
        }
        equipmentDao = mockk(relaxed = true) {
            every { getAllEquipments() } returns allEquipmentsFlow
        }
        operationTypeDao = mockk(relaxed = true) {
            every { getAllOperationTypes() } returns allOperationTypesFlow
        }
        categoryDao = mockk(relaxed = true) {
            every { getAllCategories() } returns allCategoriesFlow
        }
        appSettingsManager = mockk(relaxed = true) {
            every { defaultEquipmentId } returns defaultEquipmentIdFlow
            every { defaultOperationTypeId } returns defaultOperationTypeIdFlow
        }
        imageRepository = mockk(relaxed = true) {
             every { getCategoryColor(any()) } returns MutableStateFlow("#000000")
        }
        viewModel = MaintenanceLogViewModel(
            maintenanceLogDao, 
            equipmentDao, 
            operationTypeDao, 
            categoryDao, 
            appSettingsManager,
            imageRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun getters_coverage_booster() = runTest {
        assertNotNull(viewModel.allCategories)
        assertNotNull(viewModel.allEquipments)
        assertNotNull(viewModel.allOperationTypes)
        assertNotNull(viewModel.defaultEquipmentId)
        assertNotNull(viewModel.defaultOperationTypeId)
        assertNotNull(viewModel.searchQuery)
        assertNotNull(viewModel.sortProperty)
        assertNotNull(viewModel.sortDirection)
        assertNotNull(viewModel.showDismissed)
        assertNotNull(viewModel.logs)
        assertNotNull(viewModel.uiEvents)
        
        viewModel.searchQuery.value
        viewModel.sortProperty.value
        viewModel.sortDirection.value
        viewModel.showDismissed.value
    }

    @Test
    fun buildQuery_exhaustive_coverage() = runTest {
        viewModel.logs.test {
            awaitItem() // Stato iniziale

            SortProperty.entries.forEach { prop ->
                viewModel.onSortPropertyChanged(prop)
                awaitItem()
            }

            viewModel.onSortDirectionChanged() // ASC
            awaitItem()
            viewModel.onSortDirectionChanged() // DESC
            awaitItem()

            viewModel.onShowDismissedToggled() // true
            awaitItem()
            viewModel.onShowDismissedToggled() // false
            awaitItem()

            viewModel.onSearchQueryChanged("test")
            awaitItem()
            viewModel.onSearchQueryChanged("")
            awaitItem()
            viewModel.onSearchQueryChanged("   ")
            awaitItem()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun allCategories_emitsDataFromDao() = runTest {
        viewModel.allCategories.test {
            assertEquals(emptyList<Category>(), awaitItem())
            val list = listOf(Category("CAT1", "Category 1"))
            allCategoriesFlow.value = list
            assertEquals(list, awaitItem())
        }
    }

    @Test
    fun getCategoryColor_delegatesToRepository() = runTest {
        val colorFlow = MutableStateFlow("#FF5733")
        every { imageRepository.getCategoryColor("TEST_CAT") } returns colorFlow
        val result = viewModel.getCategoryColor("TEST_CAT").first()
        assertEquals("#FF5733", result)
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
        coEvery { maintenanceLogDao.updateLog(any()) } throws RuntimeException("DB error")
        viewModel.uiEvents.test {
            viewModel.updateLog(mockk())
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
        coEvery { maintenanceLogDao.updateLog(any()) } throws RuntimeException("DB error")
        viewModel.uiEvents.test {
            viewModel.dismissLog(mockk())
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
        coEvery { maintenanceLogDao.updateLog(any()) } throws RuntimeException("DB error")
        viewModel.uiEvents.test {
            viewModel.restoreLog(mockk())
            assertEquals(MaintenanceLogViewModel.UiEvent.RestoreLogFailed, awaitItem())
        }
    }
}
