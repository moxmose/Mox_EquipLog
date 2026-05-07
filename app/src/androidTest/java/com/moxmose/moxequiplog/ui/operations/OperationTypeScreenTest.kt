package com.moxmose.moxequiplog.ui.operations

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.moxmose.moxequiplog.data.local.ImageIdentifier
import com.moxmose.moxequiplog.data.local.OperationType
import com.moxmose.moxequiplog.data.local.TimeGranularity
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OperationTypeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun operationTypeScreenContent_whenListIsNotEmpty_displaysTypes() {
        val operationTypes = listOf(
            OperationType(id = 1, description = "Oil Change"),
            OperationType(id = 2, description = "Brake Check")
        )

        composeTestRule.setContent {
            OperationTypeScreenContent(
                operationTypes = operationTypes,
                operationTypeImages = emptyList(),
                allCategories = emptyList(),
                defaultIcon = null,
                defaultPhotoUri = null,
                showDismissed = false,
                onToggleShowDismissed = {},
                showAddDialog = false,
                onShowAddDialogChange = {},
                onAddOperationType = { _, _, _, _, _, _, _, _, _, _ -> },
                onUpdateOperationTypes = {},
                onUpdateOperationType = {},
                onDismissOperationType = {},
                onRestoreOperationType = {},
                onAddImage = { _, _ -> },
                onToggleImageVisibility = { _ -> },
                operationCategoryColor = "#808080",
                snackbarHostState = remember { SnackbarHostState() },
                defaultOperationTypeId = null,
                onToggleDefault = {},
                categoryColors = emptyMap(),
                categoryDefaultIcons = emptyMap(),
                categoryDefaultPhotos = emptyMap(),
                operationStatuses = emptyMap(),
                onAffectedAction = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Oil Change", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Brake Check", substring = true).assertIsDisplayed()
    }

    @Test
    fun addOperationTypeFab_onClick_invokesOnShowAddDialogChange() {
        val onShowAddDialogChangeCalled = AtomicBoolean(false)

        composeTestRule.setContent {
            OperationTypeScreenContent(
                operationTypes = emptyList(),
                operationTypeImages = emptyList(),
                allCategories = emptyList(),
                defaultIcon = null,
                defaultPhotoUri = null,
                showDismissed = false,
                onToggleShowDismissed = {},
                showAddDialog = false,
                onShowAddDialogChange = { onShowAddDialogChangeCalled.set(it) },
                onAddOperationType = { _, _, _, _, _, _, _, _, _, _ -> },
                onUpdateOperationTypes = {},
                onUpdateOperationType = {},
                onDismissOperationType = {},
                onRestoreOperationType = {},
                onAddImage = { _, _ -> },
                onToggleImageVisibility = { _ -> },
                operationCategoryColor = "#808080",
                snackbarHostState = remember { SnackbarHostState() },
                defaultOperationTypeId = null,
                onToggleDefault = {},
                categoryColors = emptyMap(),
                categoryDefaultIcons = emptyMap(),
                categoryDefaultPhotos = emptyMap(),
                operationStatuses = emptyMap(),
                onAffectedAction = { _, _ -> }
            )
        }

        // Cerco "Operation" per coprire "Add Operation Type" e "Aggiungi Tipo Operazione"
        composeTestRule.onNodeWithContentDescription("Operation", substring = true, ignoreCase = true).performClick()

        assertTrue(onShowAddDialogChangeCalled.get())
    }

    @Test
    fun addOperationTypeDialog_onConfirm_callsOnAddOperationType() {
        val newOperationDescription = "Tire Rotation"
        val addedOperationInfo = AtomicReference<Triple<String, ImageIdentifier?, Double?>>()

        composeTestRule.setContent {
            AddOperationTypeDialog(
                onDismissRequest = {},
                onConfirm = { desc, identifier, _, _, _, _, _, _, _, cost -> addedOperationInfo.set(Triple(desc, identifier, cost)) },
                imageLibrary = emptyList(),
                categories = emptyList(),
                categoryColors = emptyMap(),
                categoryDefaultIcons = emptyMap(),
                categoryDefaultPhotos = emptyMap(),
                defaultIcon = null,
                defaultPhotoUri = null,
                onAddImage = { _, _ -> },
                onToggleImageVisibility = { _ -> },
                operationCategoryColor = "#808080"
            )
        }

        // Cerco "descrip" per Description/Descrizione
        composeTestRule.onNodeWithText("descrip", substring = true, ignoreCase = true).performTextInput(newOperationDescription)
        
        // Cerco "cost" per Cost/Costo
        composeTestRule.onNodeWithText("cost", substring = true, ignoreCase = true).performTextInput("150.0")

        // Cerco "Add" o "Aggiungi"
        composeTestRule.onNodeWithText("Add", ignoreCase = true).performClick()

        assertEquals(newOperationDescription, addedOperationInfo.get().first)
        assertNull(addedOperationInfo.get().second)
        assertEquals(150.0, addedOperationInfo.get().third)
    }
}
