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
import com.moxmose.moxequiplog.utils.UiConstants
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
                reportsColors = emptyList(),
                measurementUnits = emptyList(),
                defaultUnitId = null,
                backgroundUri = null,
                backgroundBlur = UiConstants.DEFAULT_BACKGROUND_BLUR,
                backgroundSaturation = UiConstants.DEFAULT_BACKGROUND_SATURATION,
                backgroundTintEnabled = UiConstants.DEFAULT_BACKGROUND_TINT_ENABLED,
                backgroundTintAlpha = UiConstants.DEFAULT_BACKGROUND_TINT_ALPHA,
                backgroundImageAlpha = UiConstants.DEFAULT_BACKGROUND_IMAGE_ALPHA,
                reportsColorMode = "NONE",
                onUsernameChange = {},
                onSetCategoryDefault = { _, _ -> },
                onAddImage = { _, _ -> },
                onRemoveImage = {},
                onUpdateImageOrder = {},
                onToggleImageVisibility = {},
                onUpdateCategoryColor = { _, _ -> },
                onSetBackgroundUri = {},
                onSetBackgroundBlur = {},
                onSetBackgroundSaturation = {},
                onSetBackgroundTintEnabled = {},
                onSetBackgroundTintAlpha = {},
                onSetBackgroundImageAlpha = {},
                onResetBackgroundSettings = {},
                onSetReportsColorMode = {},
                onAddUnit = { _, _ -> },
                onUpdateUnit = {},
                onToggleUnitVisibility = {},
                onUpdateUnitsOrder = {},
                onDeleteUnit = {},
                onToggleDefaultUnit = {},
                isPhotoUsed = { false },
                showAboutDialog = false,
                onShowAboutDialogChange = {},
                onShowColorManager = { _, _ -> },
                showImageDialog = false,
                onShowImageDialogChange = {},
                onAddColor = { _, _ -> },
                onUpdateColor = {},
                onUpdateColorsOrder = {},
                onUpdateReportColorsOrder = {},
                onToggleColorVisibility = {},
                onToggleReportColorVisibility = {},
                onBackupDatabase = {},
                onRestoreDatabase = {},
                getSuggestedBackupFileName = { "" },
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
                reportsColors = emptyList(),
                measurementUnits = emptyList(),
                defaultUnitId = null,
                backgroundUri = null,
                backgroundBlur = UiConstants.DEFAULT_BACKGROUND_BLUR,
                backgroundSaturation = UiConstants.DEFAULT_BACKGROUND_SATURATION,
                backgroundTintEnabled = UiConstants.DEFAULT_BACKGROUND_TINT_ENABLED,
                backgroundTintAlpha = UiConstants.DEFAULT_BACKGROUND_TINT_ALPHA,
                backgroundImageAlpha = UiConstants.DEFAULT_BACKGROUND_IMAGE_ALPHA,
                reportsColorMode = "NONE",
                onUsernameChange = { changedUsername.set(it) },
                onSetCategoryDefault = { _, _ -> },
                onAddImage = { _, _ -> },
                onRemoveImage = {},
                onUpdateImageOrder = {},
                onToggleImageVisibility = {},
                onUpdateCategoryColor = { _, _ -> },
                onSetBackgroundUri = {},
                onSetBackgroundBlur = {},
                onSetBackgroundSaturation = {},
                onSetBackgroundTintEnabled = {},
                onSetBackgroundTintAlpha = {},
                onSetBackgroundImageAlpha = {},
                onResetBackgroundSettings = {},
                onSetReportsColorMode = {},
                onAddUnit = { _, _ -> },
                onUpdateUnit = {},
                onToggleUnitVisibility = {},
                onUpdateUnitsOrder = {},
                onDeleteUnit = {},
                onToggleDefaultUnit = {},
                isPhotoUsed = { false },
                showAboutDialog = false,
                onShowAboutDialogChange = {},
                onShowColorManager = { _, _ -> },
                showImageDialog = false,
                onShowImageDialogChange = {},
                onAddColor = { _, _ -> },
                onUpdateColor = {},
                onUpdateColorsOrder = {},
                onUpdateReportColorsOrder = {},
                onToggleColorVisibility = {},
                onToggleReportColorVisibility = {},
                onBackupDatabase = {},
                onRestoreDatabase = {},
                getSuggestedBackupFileName = { "" },
                snackbarHostState = remember { SnackbarHostState() }
            )
        }

        composeTestRule.onNodeWithText("Nome Utente").performTextInput(newUsername)
        composeTestRule.onNodeWithText("Salva Nome Utente", substring = true, ignoreCase = true).performClick()

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
                reportsColors = emptyList(),
                measurementUnits = emptyList(),
                defaultUnitId = null,
                backgroundUri = null,
                backgroundBlur = UiConstants.DEFAULT_BACKGROUND_BLUR,
                backgroundSaturation = UiConstants.DEFAULT_BACKGROUND_SATURATION,
                backgroundTintEnabled = UiConstants.DEFAULT_BACKGROUND_TINT_ENABLED,
                backgroundTintAlpha = UiConstants.DEFAULT_BACKGROUND_TINT_ALPHA,
                backgroundImageAlpha = UiConstants.DEFAULT_BACKGROUND_IMAGE_ALPHA,
                reportsColorMode = "NONE",
                onUsernameChange = {},
                onSetCategoryDefault = { _, _ -> },
                onAddImage = { _, _ -> },
                onRemoveImage = {},
                onUpdateImageOrder = {},
                onToggleImageVisibility = {},
                onUpdateCategoryColor = { _, _ -> },
                onSetBackgroundUri = {},
                onSetBackgroundBlur = {},
                onSetBackgroundSaturation = {},
                onSetBackgroundTintEnabled = {},
                onSetBackgroundTintAlpha = {},
                onSetBackgroundImageAlpha = {},
                onResetBackgroundSettings = {},
                onSetReportsColorMode = {},
                onAddUnit = { _, _ -> },
                onUpdateUnit = {},
                onToggleUnitVisibility = {},
                onUpdateUnitsOrder = {},
                onDeleteUnit = {},
                onToggleDefaultUnit = {},
                isPhotoUsed = { false },
                showAboutDialog = false,
                onShowAboutDialogChange = { onShowAboutDialogChangeCalled.set(it) },
                onShowColorManager = { _, _ -> },
                showImageDialog = false,
                onShowImageDialogChange = {},
                onAddColor = { _, _ -> },
                onUpdateColor = {},
                onUpdateColorsOrder = {},
                onUpdateReportColorsOrder = {},
                onToggleColorVisibility = {},
                onToggleReportColorVisibility = {},
                onBackupDatabase = {},
                onRestoreDatabase = {},
                getSuggestedBackupFileName = { "" },
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
                reportsColors = emptyList(),
                measurementUnits = emptyList(),
                defaultUnitId = null,
                backgroundUri = null,
                backgroundBlur = UiConstants.DEFAULT_BACKGROUND_BLUR,
                backgroundSaturation = UiConstants.DEFAULT_BACKGROUND_SATURATION,
                backgroundTintEnabled = UiConstants.DEFAULT_BACKGROUND_TINT_ENABLED,
                backgroundTintAlpha = UiConstants.DEFAULT_BACKGROUND_TINT_ALPHA,
                backgroundImageAlpha = UiConstants.DEFAULT_BACKGROUND_IMAGE_ALPHA,
                reportsColorMode = "NONE",
                onUsernameChange = {},
                onSetCategoryDefault = { _, _ -> },
                onAddImage = { _, _ -> },
                onRemoveImage = {},
                onUpdateImageOrder = {},
                onToggleImageVisibility = {},
                onUpdateCategoryColor = { _, _ -> },
                onSetBackgroundUri = {},
                onSetBackgroundBlur = {},
                onSetBackgroundSaturation = {},
                onSetBackgroundTintEnabled = {},
                onSetBackgroundTintAlpha = {},
                onSetBackgroundImageAlpha = {},
                onResetBackgroundSettings = {},
                onSetReportsColorMode = {},
                onAddUnit = { _, _ -> },
                onUpdateUnit = {},
                onToggleUnitVisibility = {},
                onUpdateUnitsOrder = {},
                onDeleteUnit = {},
                onToggleDefaultUnit = {},
                isPhotoUsed = { false },
                showAboutDialog = true,
                onShowAboutDialogChange = { callbackValue.set(it) },
                onShowColorManager = { _, _ -> },
                showImageDialog = false,
                onShowImageDialogChange = {},
                onAddColor = { _, _ -> },
                onUpdateColor = {},
                onUpdateColorsOrder = {},
                onUpdateReportColorsOrder = {},
                onToggleColorVisibility = {},
                onToggleReportColorVisibility = {},
                onBackupDatabase = {},
                onRestoreDatabase = {},
                getSuggestedBackupFileName = { "" },
                snackbarHostState = remember { SnackbarHostState() }
            )
        }

        composeTestRule.onNodeWithText("OK").performClick()

        assertFalse(callbackValue.get())
    }
}
