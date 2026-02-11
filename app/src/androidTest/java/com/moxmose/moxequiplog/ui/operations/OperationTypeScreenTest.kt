package com.moxmose.moxequiplog.ui.operations

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.moxmose.moxequiplog.data.local.OperationType
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals

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
                showDismissed = false,
                onToggleShowDismissed = {},
                showAddDialog = false,
                onShowAddDialogChange = {},
                onAddOperationType = { _, _ -> },
                onUpdateOperationTypes = {},
                onUpdateOperationType = {},
                onDismissOperationType = {},
                onRestoreOperationType = {},
                allCategories = emptyList(),
                defaultIcon = null,
                defaultPhotoUri = null,
                onAddMedia = { _, _ -> },
                onToggleMediaVisibility = { _, _ -> },
                operationCategoryColor = null,
                operationTypeMedia = emptyList(),
            )
        }

        composeTestRule.onNodeWithText("Oil Change").assertIsDisplayed()
        composeTestRule.onNodeWithText("Brake Check").assertIsDisplayed()
    }

    @Test
    fun addOperationTypeFab_onClick_invokesOnShowAddDialogChange() {
        val onShowAddDialogChangeCalled = AtomicBoolean(false)

        composeTestRule.setContent {
            OperationTypeScreenContent(
                operationTypes = emptyList(),
                showDismissed = false,
                onToggleShowDismissed = {},
                showAddDialog = false,
                onShowAddDialogChange = { onShowAddDialogChangeCalled.set(it) },
                onAddOperationType = { _, _ -> },
                onUpdateOperationTypes = {},
                onUpdateOperationType = {},
                onDismissOperationType = {},
                onRestoreOperationType = {},
                allCategories = emptyList(),
                defaultIcon = null,
                defaultPhotoUri = null,
                onAddMedia = { _, _ -> },
                onToggleMediaVisibility = { _, _ -> },
                operationCategoryColor = null,
                operationTypeMedia = emptyList()
            )
        }

        composeTestRule.onNodeWithContentDescription("Add Operation Type").performClick()

        assertTrue(onShowAddDialogChangeCalled.get())
    }

    @Test
    fun addOperationTypeDialog_onConfirm_callsOnAddOperationType() {
        val newOperationDescription = "Tire Rotation"
        val addedOperationInfo = AtomicReference<String>()

        composeTestRule.setContent {
            AddOperationTypeDialog(
                onDismissRequest = {},
                onConfirm = { desc, _ -> addedOperationInfo.set(desc) },
                defaultIcon = null,
                defaultPhotoUri = null,
                onAddMedia = { _, _ -> },
                onToggleMediaVisibility = { _, _ -> },
                operationCategoryColor = null,
                mediaLibrary = emptyList(),
                categories = emptyList()
            )
        }

        // 1. Enter text into the description field
        composeTestRule.onNodeWithText("Operation type description").performTextInput(newOperationDescription)

        // 2. Click the confirm button
        composeTestRule.onNodeWithText("Add").performClick()

        // 3. Verify the callback was invoked with the correct data
        assertEquals(newOperationDescription, addedOperationInfo.get())
    }
}
