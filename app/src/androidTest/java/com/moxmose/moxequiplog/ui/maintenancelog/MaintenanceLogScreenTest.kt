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
import com.moxmose.moxequiplog.data.local.OperationType
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
                onAddLog = { _, _, _, _, _, _, _ -> },
                onAddReminder = { _, _, _, _, _ -> },
                onRefreshReminders = {},
                onEstimateDueDate = { _, _ -> null },
                onEstimateTargetValue = { _, _ -> null },
                expandedCardId = null,
                onCardExpanded = {},
                editingCardId = null,
                onEditLog = {},
                onUpdateLog = {},
                onDeleteLog = {},
                onDismissLog = {},
                onRestoreLog = {},
                snackbarHostState = remember { SnackbarHostState() },
                defaultEquipmentId = null,
                defaultOperationTypeId = null,
                equipmentCategoryColor = null,
                operationCategoryColor = null,
                onCompleteReminder = {},
                onEditReminder = {},
                onDeleteReminder = {},
                syncCalendarByDefault = false,
                googleAccountName = null,
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
                logs = emptyList(),
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
                onAddLog = { _, _, _, _, _, _, _ -> },
                onAddReminder = { _, _, _, _, _ -> },
                onRefreshReminders = {},
                onEstimateDueDate = { _, _ -> null },
                onEstimateTargetValue = { _, _ -> null },
                expandedCardId = null,
                onCardExpanded = {},
                editingCardId = null,
                onEditLog = {},
                onUpdateLog = {},
                onDeleteLog = {},
                onDismissLog = {},
                onRestoreLog = {},
                snackbarHostState = remember { SnackbarHostState() },
                defaultEquipmentId = null,
                defaultOperationTypeId = null,
                equipmentCategoryColor = null,
                operationCategoryColor = null,
                onCompleteReminder = {},
                onEditReminder = {},
                onDeleteReminder = {},
                syncCalendarByDefault = false,
                googleAccountName = null,
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
                onDismissRequest = {},
                onConfirm = { confirmedLog.set(it) },
                measurementUnits = emptyList(),
                defaultEquipmentId = null,
                defaultOperationTypeId = null,
                initialDate = System.currentTimeMillis(),
                initialValue = "",
                initialSyncToCalendar = null,
                equipmentCategoryColor = null,
                operationCategoryColor = null
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
                measurementUnits = emptyList(),
                isExpanded = false, 
                isEditing = false, 
                onExpand = { onCardExpandedCalled.set(true) }, 
                onEdit = {}, 
                onSave = {}, 
                onDelete = {},
                onDismiss = {}, 
                onRestore = {},
                equipmentCategoryColor = null,
                operationCategoryColor = null
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
                measurementUnits = emptyList(),
                isExpanded = false, 
                isEditing = false, 
                onExpand = {}, 
                onEdit = { onEditCalled.set(true) }, 
                onSave = {}, 
                onDelete = {},
                onDismiss = {}, 
                onRestore = {},
                equipmentCategoryColor = null,
                operationCategoryColor = null
            )
        }

        // Cerco "Edit" / "Modifica"
        composeTestRule.onNodeWithContentDescription("Edit", ignoreCase = true, substring = true).performClick()

        assertTrue(onEditCalled.get())
    }
}
