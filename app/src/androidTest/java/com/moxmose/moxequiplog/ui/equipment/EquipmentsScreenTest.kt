package com.moxmose.moxequiplog.ui.equipment

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.Equipment
import com.moxmose.moxequiplog.data.local.Image
import com.moxmose.moxequiplog.data.local.ImageIdentifier
import com.moxmose.moxequiplog.data.local.MeasurementUnit
import com.moxmose.moxequiplog.ui.equipment.components.AddEquipmentDialog
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertNull


class EquipmentsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun equipmentsScreenContent_whenEquipmentsListIsNotEmpty_displaysEquipments() {
        val equipments = listOf(
            Equipment(id = 1, description = "Road Equipment")
        )

        composeTestRule.setContent {
            EquipmentScreenContent(
                equipments = equipments,
                equipmentImages = emptyList<Image>(),
                allCategories = emptyList<Category>(),
                allSections = emptyList(),
                selectedSectionId = 0,
                onSectionSelected = {},
                showDismissedSections = false,
                onToggleShowDismissedSections = {},
                measurementUnits = emptyList<MeasurementUnit>(),
                defaultUnitId = null,
                defaultIcon = null,
                defaultPhotoUri = null,
                equipmentCategoryColor = null,
                showDismissed = false,
                onToggleShowDismissed = {},
                showAddDialog = false,
                onShowAddDialogChange = {},
                onCloneEquipment = {},
                onAddEquipment = { _, _, _, _, _, _, _, _, _, _, _, _ -> },
                onUpdateEquipments = { _ -> },
                onUpdateEquipment = { _ -> },
                onDeleteEquipment = { _ -> },
                onDismissEquipment = { _ -> },
                onRestoreEquipment = { _ -> },
                onAddImage = { _, _ -> },
                onToggleImageVisibility = { _ -> },
                snackbarHostState = remember { SnackbarHostState() },
                defaultEquipmentId = null,
                onToggleDefault = { _ -> },
                categoryColors = emptyMap<String, String>(),
                categoryDefaultIcons = emptyMap<String, String?>(),
                categoryDefaultPhotos = emptyMap<String, String?>(),
                equipmentStatuses = emptyMap<Int, EquipmentStatus>(),
                onUpdateAddDraft = {},
                onStartEdit = {},
                onCancelEdit = {},
                onToggleDefaultInDraft = {},
                onUpdateDraft = {},
                onSaveEdit = {},
                onPredictionAction = { _, _ -> },
                onPlannedAction = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Road Equipment", substring = true).assertIsDisplayed()
    }

    @Test
    fun addEquipmentFab_onClick_invokesOnShowAddDialogChange() {
        val onShowAddDialogChangeCalled = AtomicBoolean(false)

        composeTestRule.setContent {
            EquipmentScreenContent(
                equipments = emptyList<Equipment>(),
                equipmentImages = emptyList<Image>(),
                allCategories = emptyList<Category>(),
                allSections = emptyList(),
                selectedSectionId = 0,
                onSectionSelected = {},
                showDismissedSections = false,
                onToggleShowDismissedSections = {},
                measurementUnits = emptyList<MeasurementUnit>(),
                defaultUnitId = null,
                defaultIcon = null,
                defaultPhotoUri = null,
                equipmentCategoryColor = null,
                showDismissed = false,
                onToggleShowDismissed = {},
                showAddDialog = false,
                onShowAddDialogChange = { onShowAddDialogChangeCalled.set(it) },
                onCloneEquipment = {},
                onAddEquipment = { _, _, _, _, _, _, _, _, _, _, _, _ -> },
                onUpdateEquipments = { _ -> },
                onUpdateEquipment = { _ -> },
                onDeleteEquipment = { _ -> },
                onDismissEquipment = { _ -> },
                onRestoreEquipment = { _ -> },
                onAddImage = { _, _ -> },
                onToggleImageVisibility = { _ -> },
                snackbarHostState = remember { SnackbarHostState() },
                defaultEquipmentId = null,
                onToggleDefault = { _ -> },
                categoryColors = emptyMap<String, String>(),
                categoryDefaultIcons = emptyMap<String, String?>(),
                categoryDefaultPhotos = emptyMap<String, String?>(),
                equipmentStatuses = emptyMap<Int, EquipmentStatus>(),
                onUpdateAddDraft = {},
                onStartEdit = {},
                onCancelEdit = {},
                onToggleDefaultInDraft = {},
                onUpdateDraft = {},
                onSaveEdit = {},
                onPredictionAction = { _, _ -> },
                onPlannedAction = { _, _ -> }
            )
        }

        // Cerco il FAB usando substring e ignorando il case per coprire "Add Equipment" e "Aggiungi Mezzo"
        composeTestRule.onNodeWithContentDescription("Equipment", substring = true, ignoreCase = true).performClick()

        assertTrue(onShowAddDialogChangeCalled.get())
    }

    @Test
    fun addEquipmentDialog_onConfirm_callsOnAddEquipment() {
        val newEquipmentDescription = "New Gravel Equipment"
        val addedEquipmentInfo = AtomicReference<Triple<String, ImageIdentifier?, Int>>()

        composeTestRule.setContent {
            AddEquipmentDialog(
                onDismissRequest = {},
                onConfirm = { desc, identifier, unitId, _, _, _, _, _, _, _, _, _ ->
                    addedEquipmentInfo.set(Triple(desc, identifier, unitId)) 
                },
                defaultIcon = null,
                defaultPhotoUri = null,
                imageLibrary = emptyList<Image>(),
                categories = emptyList<Category>(),
                measurementUnits = emptyList<MeasurementUnit>(),
                allSections = emptyList(),
                selectedSectionId = 1,
                showDismissedSections = false,
                defaultUnitId = null,
                equipmentCategoryColor = null,
                categoryColors = emptyMap<String, String>(),
                categoryDefaultIcons = emptyMap<String, String?>(),
                categoryDefaultPhotos = emptyMap<String, String?>(),
                onAddImage = { _, _ -> },
                onToggleImageVisibility = { _ -> }
            )
        }

        // Cerco il campo descrizione usando substring ("Description" o "Descrizione")
        composeTestRule.onNodeWithText("descrip", substring = true, ignoreCase = true).performTextInput(newEquipmentDescription)

        // Cerco il pulsante conferma ("Add" o "Aggiungi")
        composeTestRule.onNodeWithText("Add", ignoreCase = true).performClick()

        assertEquals(newEquipmentDescription, addedEquipmentInfo.get().first)
        assertNull(addedEquipmentInfo.get().second)
    }
}
