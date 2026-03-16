package com.moxmose.moxequiplog.ui.options

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.moxmose.moxequiplog.data.local.AppColor
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.Image
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OptionsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun username_isDisplayedCorrectly() {
        val testUsername = "JohnDoe"

        composeTestRule.setContent {
            OptionsScreenContent(
                username = testUsername,
                allImages = emptyList(),
                categoriesUiState = emptyList(),
                allColors = emptyList(),
                onUsernameChange = {},
                onSetCategoryDefault = { _, _ -> },
                onAddImage = { _, _ -> },
                onRemoveImage = {},
                onUpdateImageOrder = {},
                onToggleImageVisibility = {},
                onUpdateCategoryColor = { _, _ -> },
                isPhotoUsed = { false },
                showAboutDialog = false,
                onShowAboutDialogChange = {},
                showColorPicker = null,
                onShowColorPickerChange = {},
                showImageDialog = false,
                onShowImageDialogChange = {},
                onAddColor = { _, _ -> },
                onUpdateColor = {},
                onUpdateColorsOrder = {},
                onToggleColorVisibility = {},
                snackbarHostState = remember { SnackbarHostState() }
            )
        }

        composeTestRule.onNodeWithText(testUsername).assertIsDisplayed()
    }

    @Test
    fun onUsernameChange_isCalled_whenTextIsEntered() {
        val changedUsername = AtomicReference<String>()
        val newUsername = "NewUser"

        composeTestRule.setContent {
            OptionsScreenContent(
                username = "",
                allImages = emptyList(),
                categoriesUiState = emptyList(),
                allColors = emptyList(),
                onUsernameChange = { changedUsername.set(it) },
                onSetCategoryDefault = { _, _ -> },
                onAddImage = { _, _ -> },
                onRemoveImage = {},
                onUpdateImageOrder = {},
                onToggleImageVisibility = {},
                onUpdateCategoryColor = { _, _ -> },
                isPhotoUsed = { false },
                showAboutDialog = false,
                onShowAboutDialogChange = {},
                showColorPicker = null,
                onShowColorPickerChange = {},
                showImageDialog = false,
                onShowImageDialogChange = {},
                onAddColor = { _, _ -> },
                onUpdateColor = {},
                onUpdateColorsOrder = {},
                onToggleColorVisibility = {},
                snackbarHostState = remember { SnackbarHostState() }
            )
        }

        composeTestRule.onNodeWithText("Nome Utente").performTextInput(newUsername)
        // Note: The Done icon is the one that triggers the save
        composeTestRule.onNodeWithText("Save Username", substring = true, ignoreCase = true).performClick()

        assertEquals(newUsername, changedUsername.get())
    }

    @Test
    fun aboutButton_onClick_invokesOnShowAboutDialogChange() {
        val onShowAboutDialogChangeCalled = AtomicBoolean(false)

        composeTestRule.setContent {
            OptionsScreenContent(
                username = "",
                allImages = emptyList(),
                categoriesUiState = emptyList(),
                allColors = emptyList(),
                onUsernameChange = {},
                onSetCategoryDefault = { _, _ -> },
                onAddImage = { _, _ -> },
                onRemoveImage = {},
                onUpdateImageOrder = {},
                onToggleImageVisibility = {},
                onUpdateCategoryColor = { _, _ -> },
                isPhotoUsed = { false },
                showAboutDialog = false,
                onShowAboutDialogChange = { onShowAboutDialogChangeCalled.set(it) },
                showColorPicker = null,
                onShowColorPickerChange = {},
                showImageDialog = false,
                onShowImageDialogChange = {},
                onAddColor = { _, _ -> },
                onUpdateColor = {},
                onUpdateColorsOrder = {},
                onToggleColorVisibility = {},
                snackbarHostState = remember { SnackbarHostState() }
            )
        }

        composeTestRule.onNodeWithText("About", ignoreCase = true).performClick()

        assertTrue(onShowAboutDialogChangeCalled.get())
    }

    @Test
    fun aboutDialog_onDismiss_invokesOnShowAboutDialogChange() {
        val callbackValue = AtomicReference<Boolean>()

        composeTestRule.setContent {
            OptionsScreenContent(
                username = "",
                allImages = emptyList(),
                categoriesUiState = emptyList(),
                allColors = emptyList(),
                onUsernameChange = {},
                onSetCategoryDefault = { _, _ -> },
                onAddImage = { _, _ -> },
                onRemoveImage = {},
                onUpdateImageOrder = {},
                onToggleImageVisibility = {},
                onUpdateCategoryColor = { _, _ -> },
                isPhotoUsed = { false },
                showAboutDialog = true, // Dialog is initially shown
                onShowAboutDialogChange = { callbackValue.set(it) },
                showColorPicker = null,
                onShowColorPickerChange = {},
                showImageDialog = false,
                onShowImageDialogChange = {},
                onAddColor = { _, _ -> },
                onUpdateColor = {},
                onUpdateColorsOrder = {},
                onToggleColorVisibility = {},
                snackbarHostState = remember { SnackbarHostState() }
            )
        }

        // Click the confirm button to dismiss
        composeTestRule.onNodeWithText("OK").performClick()

        // The callback should be called with `false`
        assertFalse(callbackValue.get())
    }
}
