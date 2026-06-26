package com.moxmose.moxequiplog.ui.maintenancelog

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.moxmose.moxequiplog.data.local.Equipment
import com.moxmose.moxequiplog.data.local.MaintenanceLog
import com.moxmose.moxequiplog.data.local.MaintenanceLogDetails
import com.moxmose.moxequiplog.data.local.MaintenanceReminderDetails
import com.moxmose.moxequiplog.data.local.MeasurementUnit
import com.moxmose.moxequiplog.data.local.OperationType
import com.moxmose.moxequiplog.ui.maintenancelog.components.MaintenanceLogCard
import com.moxmose.moxequiplog.ui.maintenancelog.components.MaintenanceLogDialog
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals

class MaintenanceLogScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val dummyEquipments = listOf(Equipment(id = 1, description = "Road Equipment"))
    private val dummyOps = listOf(OperationType(id = 1, description = "Oil Change"))

    @Test
    fun maintenanceLogScreen_whenLogsArePresent_displaysLogs() {
        val logs = listOf(
            MaintenanceLogDetails(
                log = MaintenanceLog(id = 1, equipmentId = 1, operationTypeId = 1, date = 0L),
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
                logs = logs,
                allSections = emptyList(),
                selectedSectionId = 0,
                onSectionSelected = {},
                showDismissedSections = false,
                onToggleShowDismissedSections = {},
                equipments = dummyEquipments,
                operationTypes = dummyOps,
                measurementUnits = emptyList<MeasurementUnit>(),
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
                onStartEdit = {},
                onCancelEdit = {},
                onUpdateDraft = {},
                onSaveEdit = {},
                onUpdateLog = { _ -> },
                onDeleteLog = { _ -> },
                onDismissLog = { _ -> },
                onRestoreLog = { _ -> },
                activeReminders = emptyList<MaintenanceReminderDetails>(),
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
                logs = emptyList<MaintenanceLogDetails>(),
                allSections = emptyList(),
                selectedSectionId = 0,
                onSectionSelected = {},
                showDismissedSections = false,
                onToggleShowDismissedSections = {},
                equipments = dummyEquipments,
                operationTypes = dummyOps,
                measurementUnits = emptyList<MeasurementUnit>(),
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
                onStartEdit = {},
                onCancelEdit = {},
                onUpdateDraft = {},
                onSaveEdit = {},
                onUpdateLog = { _ -> },
                onDeleteLog = { _ -> },
                onDismissLog = { _ -> },
                onRestoreLog = { _ -> },
                activeReminders = emptyList<MaintenanceReminderDetails>(),
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
                measurementUnits = emptyList<MeasurementUnit>(),
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
        assertEquals(1, confirmedLog.get().operationTypeId)
    }

    @Test
    fun maintenanceLogCard_onClick_invokesOnExpand() {
        val onCardExpandedCalled = AtomicBoolean(false)
        val log = MaintenanceLogDetails(
            log = MaintenanceLog(id = 1, equipmentId = 1, operationTypeId = 1, date = 0L),
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
                logDetail = log, 
                equipments = dummyEquipments, 
                operationTypes = dummyOps, 
                measurementUnits = emptyList<MeasurementUnit>(),
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
            log = MaintenanceLog(id = 1, equipmentId = 1, operationTypeId = 1, date = 0L),
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
                logDetail = log, 
                equipments = dummyEquipments, 
                operationTypes = dummyOps, 
                measurementUnits = emptyList<MeasurementUnit>(),
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
}
