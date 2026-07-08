package com.moxmose.moxequiplog.ui.maintenancelog

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.Equipment
import com.moxmose.moxequiplog.data.local.MaintenanceLog
import com.moxmose.moxequiplog.data.local.MaintenanceLogDetails
import com.moxmose.moxequiplog.data.local.MaintenanceReminderDetails
import com.moxmose.moxequiplog.data.local.MeasurementUnit
import com.moxmose.moxequiplog.data.local.OperationType
import com.moxmose.moxequiplog.ui.equipment.OperationStatus
import com.moxmose.moxequiplog.ui.maintenancelog.components.MaintenanceLogCard
import com.moxmose.moxequiplog.ui.maintenancelog.components.MaintenanceLogDialog
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals

import androidx.compose.ui.test.performTextInput

class MaintenanceLogScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val dummyEquipments = listOf(Equipment(id = 1, description = "Road Equipment"))
    private val dummyOps = listOf(OperationType(id = 2, description = "Oil Change"))

    @Test
    fun maintenanceLogScreen_whenLogsArePresent_displaysLogs() {
        val logs = listOf(
            MaintenanceLogDetails(
                log = MaintenanceLog(id = 1, equipmentId = 1, operationTypeId = 2, date = 0L),
                equipmentDescription = "Road Equipment",
                operationTypeDescription = "Oil Change",
                equipmentPhotoUri = null,
                equipmentIconIdentifier = null,
                operationTypePhotoUri = null,
                operationTypeIconIdentifier = null,
                equipmentDismissed = false,
                operationTypeDismissed = false
            )
        )

        composeTestRule.setContent {
            MaintenanceLogScreenContent(
                mainLogList = logs,
                allSections = emptyList(),
                selectedSectionId = 0,
                sectionSelectorType = "",
                onSectionSelected = {},
                showDismissedSections = false,
                onToggleShowDismissedSections = {},
                equipments = dummyEquipments,
                operationTypes = dummyOps,
                measurementUnits = emptyList(),
                searchQuery = "",
                onSearchQueryChange = {},
                sortProperty = SortProperty.DATE,
                onSortPropertyChange = {},
                sortDirection = SortDirection.DESCENDING,
                onSortDirectionChange = {},
                showDismissed = false,
                onShowDismissedToggle = {},
                showAddDialog = false,
                onShowAddDialogChange = {},
                onAddLog = { _, _, _, _, _, _, _, _, _ -> },
                onAddReminder = { _, _, _, _, _ -> },
                onRefreshReminders = {},
                onEstimateDueDate = { _, _ -> null },
                onEstimateTargetValue = { _, _ -> null },
                onGetOperationCostStats = { _ -> null to null },
                expandedCardId = null,
                onCardExpanded = { _ -> },
                editingCardId = null,
                allDrafts = emptyMap<Int, MaintenanceLog>(),
                onStartEdit = {},
                onCancelEdit = {},
                onUpdateDraft = {},
                onSaveEdit = {},
                onUpdateLog = { _ -> },
                onDeleteLog = { _ -> },
                onDismissLog = { _ -> },
                onRestoreLog = { _ -> },
                activeReminders = emptyList<MaintenanceReminderDetails>(),
                automaticPredictions = emptyList<Pair<Equipment, OperationStatus>>(),
                snackbarHostState = remember { SnackbarHostState() },
                defaultEquipmentId = null,
                defaultOperationTypeId = null,
                equipmentCategoryColor = null,
                operationCategoryColor = null,
                onCompleteReminder = { _ -> },
                onEditReminder = { _ -> },
                onPredictionAction = { _, _ -> },
                syncCalendarByDefault = false,
                googleAccountName = null,
                costTrendThreshold = 0.05f,
                logAddDraft = null,
                reminderAddDraft = null,
                onUpdateLogDraft = {},
                onUpdateReminderDraft = {},
                onNavigateToOptions = {}
            )
        }

        composeTestRule.onNodeWithText("Road Equipment", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Oil Change", substring = true).assertIsDisplayed()
    }

    @Test
    fun addLogFab_onClick_invokesOnShowAddDialogChange() {
        val onShowAddDialogChangeCalled = AtomicBoolean(false)

        composeTestRule.setContent {
            MaintenanceLogScreenContent(
                mainLogList = emptyList<MaintenanceLogDetails>(),
                allSections = emptyList(),
                selectedSectionId = 0,
                sectionSelectorType = "",
                onSectionSelected = {},
                showDismissedSections = false,
                onToggleShowDismissedSections = {},
                equipments = dummyEquipments,
                operationTypes = dummyOps,
                measurementUnits = emptyList(),
                searchQuery = "",
                onSearchQueryChange = {},
                sortProperty = SortProperty.DATE,
                onSortPropertyChange = {},
                sortDirection = SortDirection.DESCENDING,
                onSortDirectionChange = {},
                showDismissed = false,
                onShowDismissedToggle = {},
                showAddDialog = false,
                onShowAddDialogChange = { onShowAddDialogChangeCalled.set(it) },
                onAddLog = { _, _, _, _, _, _, _, _, _ -> },
                onAddReminder = { _, _, _, _, _ -> },
                onRefreshReminders = {},
                onEstimateDueDate = { _, _ -> null },
                onEstimateTargetValue = { _, _ -> null },
                onGetOperationCostStats = { _ -> null to null },
                expandedCardId = null,
                onCardExpanded = { _ -> },
                editingCardId = null,
                allDrafts = emptyMap<Int, MaintenanceLog>(),
                onStartEdit = {},
                onCancelEdit = {},
                onUpdateDraft = {},
                onSaveEdit = {},
                onUpdateLog = { _ -> },
                onDeleteLog = { _ -> },
                onDismissLog = { _ -> },
                onRestoreLog = { _ -> },
                activeReminders = emptyList<MaintenanceReminderDetails>(),
                automaticPredictions = emptyList<Pair<Equipment, OperationStatus>>(),
                snackbarHostState = remember { SnackbarHostState() },
                defaultEquipmentId = null,
                defaultOperationTypeId = null,
                equipmentCategoryColor = null,
                operationCategoryColor = null,
                onCompleteReminder = { _ -> },
                onEditReminder = { _ -> },
                onPredictionAction = { _, _ -> },
                syncCalendarByDefault = false,
                googleAccountName = null,
                costTrendThreshold = 0.05f,
                logAddDraft = null,
                reminderAddDraft = null,
                onUpdateLogDraft = {},
                onUpdateReminderDraft = {},
                onNavigateToOptions = {}
            )
        }

        // Cerco "Log" (Add Log / Aggiungi Log)
        composeTestRule.onNodeWithContentDescription("Log", ignoreCase = true, substring = true).performClick()

        assertTrue(onShowAddDialogChangeCalled.get())
    }

    @Test
    fun maintenanceLogDialog_onConfirm_callsOnConfirm() {
        val confirmedLog = AtomicReference<MaintenanceLog>()

        composeTestRule.setContent {
            MaintenanceLogDialog(
                equipments = dummyEquipments,
                operationTypes = dummyOps,
                measurementUnits = emptyList(),
                allSections = emptyList(),
                onDismissRequest = {},
                onConfirm = { confirmedLog.set(it) },
                onSchedule = null,
                onDeleteReminder = null,
                onEstimateDueDate = null,
                onEstimateTargetValue = null,
                onGetOperationCostStats = null,
                defaultEquipmentId = null,
                defaultOperationTypeId = null,
                initialDate = System.currentTimeMillis(),
                initialValue = "",
                initialCost = "",
                initialIsUnplanned = false,
                initialSyncToCalendar = false,
                initialHasFixedDate = true,
                isEditMode = false,
                equipmentCategoryColor = null,
                operationCategoryColor = null,
                syncCalendarByDefault = false,
                googleAccountName = null,
                costTrendThreshold = 0.05f,
                initialTab = null,
                onNavigateToOptions = {}
            )
        }

        // Selettori più generici per i placeholder del menu a tendina
        // Cerco "equip" (Equipment / Mezzo)
        composeTestRule.onNodeWithText("equip", ignoreCase = true, substring = true).performClick()
        composeTestRule.onNodeWithText("Road Equipment").performClick()
        
        // Cerco "operat" (Operation / Operazione)
        composeTestRule.onNodeWithText("operat", ignoreCase = true, substring = true).performClick()
        composeTestRule.onNodeWithText("Oil Change").performClick()

        // Click "Add" / "Aggiungi"
        composeTestRule.onNodeWithText("Add", ignoreCase = true, substring = true).performClick()

        assertEquals(1, confirmedLog.get().equipmentId)
        assertEquals(2, confirmedLog.get().operationTypeId)
    }

    @Test
    fun maintenanceLogCard_onClick_invokesOnExpand() {
        val onCardExpandedCalled = AtomicBoolean(false)
        val log = MaintenanceLogDetails(
            log = MaintenanceLog(id = 1, equipmentId = 1, operationTypeId = 2, date = 0L),
            equipmentDescription = "Road Equipment",
            operationTypeDescription = "Oil Change",
            equipmentPhotoUri = null,
            equipmentIconIdentifier = null,
            operationTypePhotoUri = null,
            operationTypeIconIdentifier = null,
            equipmentDismissed = false,
            operationTypeDismissed = false,
        )

        composeTestRule.setContent {
            MaintenanceLogCard(
                logDetail = log, 
                equipments = dummyEquipments, 
                operationTypes = dummyOps, 
                measurementUnits = emptyList(),
                allSections = emptyList(),
                isExpanded = false, 
                onExpand = { onCardExpandedCalled.set(true) }, 
                onSave = { _ -> }, 
                onDelete = { _ -> },
                onGetOperationCostStats = { _ -> null to null },
                equipmentCategoryColor = null,
                operationCategoryColor = null,
                costTrendThreshold = 0.05f
            )
        }

        composeTestRule.onNodeWithText("Road Equipment", substring = true).performClick()

        assertTrue(onCardExpandedCalled.get())
    }

    @Test
    fun editButton_onClick_invokesOnEdit() {
        val onEditCalled = AtomicBoolean(false)
        val log = MaintenanceLogDetails(
            log = MaintenanceLog(id = 1, equipmentId = 1, operationTypeId = 2, date = 0L),
            equipmentDescription = "Road Equipment",
            operationTypeDescription = "Oil Change",
            equipmentPhotoUri = null,
            equipmentIconIdentifier = null,
            operationTypePhotoUri = null,
            operationTypeIconIdentifier = null,
            equipmentDismissed = false,
            operationTypeDismissed = false,
        )

        composeTestRule.setContent {
            MaintenanceLogCard(
                logDetail = log, 
                equipments = dummyEquipments, 
                operationTypes = dummyOps, 
                measurementUnits = emptyList(),
                allSections = emptyList(),
                isExpanded = false, 
                onExpand = {}, 
                onSave = { _ -> }, 
                onDelete = { _ -> },
                onGetOperationCostStats = { _ -> null to null },
                equipmentCategoryColor = null,
                operationCategoryColor = null,
                costTrendThreshold = 0.05f,
                onStartEdit = { onEditCalled.set(true) }
            )
        }

        // Cerco "Edit" / "Modifica"
        composeTestRule.onNodeWithContentDescription("Edit", ignoreCase = true, substring = true).performClick()

        assertTrue(onEditCalled.get())
    }

    @Test
    fun searchTextField_onValueChange_invokesOnSearchQueryChange() {
        val onSearchQueryChangeCalled = AtomicReference<String>()

        composeTestRule.setContent {
            MaintenanceLogScreenContent(
                mainLogList = emptyList(),
                allSections = emptyList(),
                selectedSectionId = 0,
                sectionSelectorType = "",
                onSectionSelected = {},
                showDismissedSections = false,
                onToggleShowDismissedSections = {},
                equipments = dummyEquipments,
                operationTypes = dummyOps,
                measurementUnits = emptyList(),
                searchQuery = "",
                onSearchQueryChange = { onSearchQueryChangeCalled.set(it) },
                sortProperty = SortProperty.DATE,
                onSortPropertyChange = {},
                sortDirection = SortDirection.DESCENDING,
                onSortDirectionChange = {},
                showDismissed = false,
                onShowDismissedToggle = {},
                showAddDialog = false,
                onShowAddDialogChange = {},
                onAddLog = { _, _, _, _, _, _, _, _, _ -> },
                onAddReminder = { _, _, _, _, _ -> },
                onRefreshReminders = {},
                onEstimateDueDate = { _, _ -> null },
                onEstimateTargetValue = { _, _ -> null },
                onGetOperationCostStats = { _ -> null to null },
                expandedCardId = null,
                onCardExpanded = { _ -> },
                editingCardId = null,
                allDrafts = emptyMap(),
                onStartEdit = {},
                onCancelEdit = {},
                onUpdateDraft = {},
                onSaveEdit = {},
                onUpdateLog = { _ -> },
                onDeleteLog = { _ -> },
                onDismissLog = { _ -> },
                onRestoreLog = { _ -> },
                activeReminders = emptyList(),
                automaticPredictions = emptyList(),
                snackbarHostState = remember { SnackbarHostState() },
                defaultEquipmentId = null,
                defaultOperationTypeId = null,
                equipmentCategoryColor = null,
                operationCategoryColor = null,
                onCompleteReminder = { _ -> },
                onEditReminder = { _ -> },
                onPredictionAction = { _, _ -> },
                syncCalendarByDefault = false,
                googleAccountName = null,
                costTrendThreshold = 0.05f,
                logAddDraft = null,
                reminderAddDraft = null,
                onUpdateLogDraft = {},
                onUpdateReminderDraft = {},
                onNavigateToOptions = {}
            )
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val searchLabel = context.getString(R.string.search_logs)
        composeTestRule.onNodeWithText(searchLabel, ignoreCase = true, substring = true).performTextInput("oil")

        assertEquals("oil", onSearchQueryChangeCalled.get())
    }

    @Test
    fun sortDirectionButton_onClick_invokesOnSortDirectionChange() {
        val onSortDirectionChangeCalled = AtomicBoolean(false)

        composeTestRule.setContent {
            MaintenanceLogScreenContent(
                mainLogList = emptyList(),
                allSections = emptyList(),
                selectedSectionId = 0,
                sectionSelectorType = "",
                onSectionSelected = {},
                showDismissedSections = false,
                onToggleShowDismissedSections = {},
                equipments = dummyEquipments,
                operationTypes = dummyOps,
                measurementUnits = emptyList(),
                searchQuery = "",
                onSearchQueryChange = {},
                sortProperty = SortProperty.DATE,
                onSortPropertyChange = {},
                sortDirection = SortDirection.DESCENDING,
                onSortDirectionChange = { onSortDirectionChangeCalled.set(true) },
                showDismissed = false,
                onShowDismissedToggle = {},
                showAddDialog = false,
                onShowAddDialogChange = {},
                onAddLog = { _, _, _, _, _, _, _, _, _ -> },
                onAddReminder = { _, _, _, _, _ -> },
                onRefreshReminders = {},
                onEstimateDueDate = { _, _ -> null },
                onEstimateTargetValue = { _, _ -> null },
                onGetOperationCostStats = { _ -> null to null },
                expandedCardId = null,
                onCardExpanded = { _ -> },
                editingCardId = null,
                allDrafts = emptyMap(),
                onStartEdit = {},
                onCancelEdit = {},
                onUpdateDraft = {},
                onSaveEdit = {},
                onUpdateLog = { _ -> },
                onDeleteLog = { _ -> },
                onDismissLog = { _ -> },
                onRestoreLog = { _ -> },
                activeReminders = emptyList(),
                automaticPredictions = emptyList(),
                snackbarHostState = remember { SnackbarHostState() },
                defaultEquipmentId = null,
                defaultOperationTypeId = null,
                equipmentCategoryColor = null,
                operationCategoryColor = null,
                onCompleteReminder = { _ -> },
                onEditReminder = { _ -> },
                onPredictionAction = { _, _ -> },
                syncCalendarByDefault = false,
                googleAccountName = null,
                costTrendThreshold = 0.05f,
                logAddDraft = null,
                reminderAddDraft = null,
                onUpdateLogDraft = {},
                onUpdateReminderDraft = {},
                onNavigateToOptions = {}
            )
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sortDirectionLabel = context.getString(R.string.sort_direction)
        composeTestRule.onNodeWithContentDescription(sortDirectionLabel, ignoreCase = true, substring = true).performClick()

        assertTrue(onSortDirectionChangeCalled.get())
    }

    @Test
    fun deleteLog_onClick_showsConfirmationAndCallsOnDelete() {
        val onDeleteCalled = AtomicBoolean(false)
        val log = MaintenanceLog(id = 1, equipmentId = 1, operationTypeId = 2, date = 0L)
        val logDetail = MaintenanceLogDetails(
            log = log,
            equipmentDescription = "Road Equipment",
            operationTypeDescription = "Oil Change",
            equipmentPhotoUri = null,
            equipmentIconIdentifier = null,
            operationTypePhotoUri = null,
            operationTypeIconIdentifier = null,
            equipmentDismissed = false,
            operationTypeDismissed = false
        )

        composeTestRule.setContent {
            MaintenanceLogCard(
                logDetail = logDetail,
                equipments = dummyEquipments,
                operationTypes = dummyOps,
                measurementUnits = emptyList(),
                allSections = emptyList(),
                isExpanded = true,
                draft = log, // Simulo editing per mostrare i bottoni di azione
                onExpand = {},
                onSave = { _ -> },
                onDelete = { onDeleteCalled.set(true) },
                onGetOperationCostStats = { _ -> null to null },
                equipmentCategoryColor = null,
                operationCategoryColor = null,
                costTrendThreshold = 0.05f
            )
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val deleteLabel = context.getString(R.string.button_delete)

        // Cerco bottone delete in CommonActionButtons
        composeTestRule.onNodeWithContentDescription(deleteLabel, ignoreCase = true, substring = true).performClick()

        // Confermo nel dialogo - uso filterToOne(hasClickAction()) per disambiguare dal titolo/messaggio
        composeTestRule.onAllNodesWithText(deleteLabel, ignoreCase = true)
            .filterToOne(hasClickAction())
            .performClick()

        assertTrue(onDeleteCalled.get())
    }

    @Test
    fun maintenanceLogDialog_onSchedule_callsOnSchedule() {
        val scheduledReminder = AtomicReference<Triple<Int, Int, Long?>>()

        composeTestRule.setContent {
            MaintenanceLogDialog(
                equipments = dummyEquipments,
                operationTypes = dummyOps,
                measurementUnits = emptyList(),
                allSections = emptyList(),
                onDismissRequest = {},
                onConfirm = { },
                onSchedule = { eqId, opId, date, _, _ -> scheduledReminder.set(Triple(eqId, opId, date)) },
                defaultEquipmentId = null,
                defaultOperationTypeId = null,
                equipmentCategoryColor = null,
                operationCategoryColor = null
            )
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val plannedTabLabel = context.getString(R.string.tab_reminder)
        val equipLabel = context.getString(R.string.navigation_equipment)
        val opLabel = context.getString(R.string.navigation_operations)
        val scheduleButtonLabel = context.getString(R.string.schedule_maintenance)

        // Switch to "Planned" tab
        composeTestRule.onNodeWithText(plannedTabLabel, ignoreCase = true).performClick()

        // Select equipment and operation
        composeTestRule.onNodeWithText(equipLabel, ignoreCase = true, substring = true).performClick()
        composeTestRule.onNodeWithText("Road Equipment").performClick()
        
        composeTestRule.onNodeWithText(opLabel, ignoreCase = true, substring = true).performClick()
        composeTestRule.onNodeWithText("Oil Change").performClick()

        // Click "Schedule"
        composeTestRule.onNodeWithText(scheduleButtonLabel, ignoreCase = true).performClick()

        val result = scheduledReminder.get()
        assertEquals(1, result.first)
        assertEquals(2, result.second)
    }

    @Test
    fun showDismissedFab_onClick_invokesOnShowDismissedToggle() {
        val onShowDismissedToggleCalled = AtomicBoolean(false)

        composeTestRule.setContent {
            MaintenanceLogScreenContent(
                mainLogList = emptyList(),
                allSections = emptyList(),
                selectedSectionId = 0,
                sectionSelectorType = "",
                onSectionSelected = {},
                showDismissedSections = false,
                onToggleShowDismissedSections = {},
                equipments = dummyEquipments,
                operationTypes = dummyOps,
                measurementUnits = emptyList(),
                searchQuery = "",
                onSearchQueryChange = {},
                sortProperty = SortProperty.DATE,
                onSortPropertyChange = {},
                sortDirection = SortDirection.DESCENDING,
                onSortDirectionChange = {},
                showDismissed = false,
                onShowDismissedToggle = { onShowDismissedToggleCalled.set(true) },
                showAddDialog = false,
                onShowAddDialogChange = {},
                onAddLog = { _, _, _, _, _, _, _, _, _ -> },
                onAddReminder = { _, _, _, _, _ -> },
                onRefreshReminders = {},
                onEstimateDueDate = { _, _ -> null },
                onEstimateTargetValue = { _, _ -> null },
                onGetOperationCostStats = { _ -> null to null },
                expandedCardId = null,
                onCardExpanded = { _ -> },
                editingCardId = null,
                allDrafts = emptyMap(),
                onStartEdit = {},
                onCancelEdit = {},
                onUpdateDraft = {},
                onSaveEdit = {},
                onUpdateLog = { _ -> },
                onDeleteLog = { _ -> },
                onDismissLog = { _ -> },
                onRestoreLog = { _ -> },
                activeReminders = emptyList(),
                automaticPredictions = emptyList(),
                snackbarHostState = remember { SnackbarHostState() },
                defaultEquipmentId = null,
                defaultOperationTypeId = null,
                equipmentCategoryColor = null,
                operationCategoryColor = null,
                onCompleteReminder = { _ -> },
                onEditReminder = { _ -> },
                onPredictionAction = { _, _ -> },
                syncCalendarByDefault = false,
                googleAccountName = null,
                costTrendThreshold = 0.05f,
                logAddDraft = null,
                reminderAddDraft = null,
                onUpdateLogDraft = {},
                onUpdateReminderDraft = {},
                onNavigateToOptions = {}
            )
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val showDismissedLabel = context.getString(R.string.show_dismissed)
        composeTestRule.onNodeWithContentDescription(showDismissedLabel, ignoreCase = true, substring = true).performClick()

        assertTrue(onShowDismissedToggleCalled.get())
    }

    @Test
    fun sortProperty_onChange_invokesOnSortPropertyChange() {
        val onSortPropertyChangeCalled = AtomicReference<SortProperty>()

        composeTestRule.setContent {
            MaintenanceLogScreenContent(
                mainLogList = emptyList(),
                allSections = emptyList(),
                selectedSectionId = 0,
                sectionSelectorType = "",
                onSectionSelected = {},
                showDismissedSections = false,
                onToggleShowDismissedSections = {},
                equipments = dummyEquipments,
                operationTypes = dummyOps,
                measurementUnits = emptyList(),
                searchQuery = "",
                onSearchQueryChange = {},
                sortProperty = SortProperty.DATE,
                onSortPropertyChange = { onSortPropertyChangeCalled.set(it) },
                sortDirection = SortDirection.DESCENDING,
                onSortDirectionChange = {},
                showDismissed = false,
                onShowDismissedToggle = {},
                showAddDialog = false,
                onShowAddDialogChange = {},
                onAddLog = { _, _, _, _, _, _, _, _, _ -> },
                onAddReminder = { _, _, _, _, _ -> },
                onRefreshReminders = {},
                onEstimateDueDate = { _, _ -> null },
                onEstimateTargetValue = { _, _ -> null },
                onGetOperationCostStats = { _ -> null to null },
                expandedCardId = null,
                onCardExpanded = { _ -> },
                editingCardId = null,
                allDrafts = emptyMap(),
                onStartEdit = {},
                onCancelEdit = {},
                onUpdateDraft = {},
                onSaveEdit = {},
                onUpdateLog = { _ -> },
                onDeleteLog = { _ -> },
                onDismissLog = { _ -> },
                onRestoreLog = { _ -> },
                activeReminders = emptyList(),
                automaticPredictions = emptyList(),
                snackbarHostState = remember { SnackbarHostState() },
                defaultEquipmentId = null,
                defaultOperationTypeId = null,
                equipmentCategoryColor = null,
                operationCategoryColor = null,
                onCompleteReminder = { _ -> },
                onEditReminder = { _ -> },
                onPredictionAction = { _, _ -> },
                syncCalendarByDefault = false,
                googleAccountName = null,
                costTrendThreshold = 0.05f,
                logAddDraft = null,
                reminderAddDraft = null,
                onUpdateLogDraft = {},
                onUpdateReminderDraft = {},
                onNavigateToOptions = {}
            )
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sortByLabel = context.getString(R.string.sort_by)
        val equipLabel = context.getString(R.string.navigation_equipment)

        // Click sort icon to open menu
        composeTestRule.onNodeWithContentDescription(sortByLabel, ignoreCase = true, substring = true).performClick()

        // Select a property
        composeTestRule.onNodeWithText(equipLabel, ignoreCase = true, substring = true).performClick()

        assertEquals(SortProperty.EQUIPMENT, onSortPropertyChangeCalled.get())
    }
}
