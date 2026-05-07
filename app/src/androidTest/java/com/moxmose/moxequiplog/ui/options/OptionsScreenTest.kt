package com.moxmose.moxequiplog.ui.options

import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.AppColor
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.Image
import com.moxmose.moxequiplog.data.local.MeasurementUnit
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
                onSetBackgroundUri = {},
                onSetBackgroundBlur = {},
                onSetBackgroundSaturation = {},
                onSetBackgroundTintEnabled = {},
                onSetBackgroundTintAlpha = {},
                onSetBackgroundImageAlpha = {},
                onResetBackgroundSettings = {},
                onSetReportsColorMode = {},
                onAddUnit = { _, _, _ -> },
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
                onBackupDatabase = {},
                onRestoreDatabase = {},
                onTotalExport = {},
                getSuggestedBackupFileName = { "" },
                getSuggestedTotalExportFileName = { "" },
                snackbarHostState = remember { SnackbarHostState() },
                googleAccountName = null,
                onGoogleAccountSelected = {},
                syncCalendarByDefault = false,
                onSyncCalendarByDefaultChange = {},
                globalUsageWindowValue = 30,
                globalUsageWindowUnit = "DAYS",
                onSetGlobalUsageWindow = { _, _ -> },
                globalVisibilityHorizonValue = 30,
                globalVisibilityHorizonUnit = "DAYS",
                onSetGlobalVisibilityHorizon = { _, _ -> },
                costAnalysisWindowValue = 12,
                costAnalysisWindowUnit = "MONTHS",
                onSetCostAnalysisWindow = { _, _ -> },
                onRecalculateAccumulated = {}
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
                onSetBackgroundUri = {},
                onSetBackgroundBlur = {},
                onSetBackgroundSaturation = {},
                onSetBackgroundTintEnabled = {},
                onSetBackgroundTintAlpha = {},
                onSetBackgroundImageAlpha = {},
                onResetBackgroundSettings = {},
                onSetReportsColorMode = {},
                onAddUnit = { _, _, _ -> },
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
                onBackupDatabase = {},
                onRestoreDatabase = {},
                onTotalExport = {},
                getSuggestedBackupFileName = { "" },
                getSuggestedTotalExportFileName = { "" },
                snackbarHostState = remember { SnackbarHostState() },
                googleAccountName = null,
                onGoogleAccountSelected = {},
                syncCalendarByDefault = false,
                onSyncCalendarByDefaultChange = {},
                globalUsageWindowValue = 30,
                globalUsageWindowUnit = "DAYS",
                onSetGlobalUsageWindow = { _, _ -> },
                globalVisibilityHorizonValue = 30,
                globalVisibilityHorizonUnit = "DAYS",
                onSetGlobalVisibilityHorizon = { _, _ -> },
                costAnalysisWindowValue = 12,
                costAnalysisWindowUnit = "MONTHS",
                onSetCostAnalysisWindow = { _, _ -> },
                onRecalculateAccumulated = {}
            )
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val label = context.getString(R.string.options_username_field_label)
        val saveAction = context.getString(R.string.options_save_username)

        composeTestRule.onNodeWithText(label, ignoreCase = true).performScrollTo().performTextInput(newUsername)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(saveAction, ignoreCase = true).performScrollTo().performClick()

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
                onSetBackgroundUri = {},
                onSetBackgroundBlur = {},
                onSetBackgroundSaturation = {},
                onSetBackgroundTintEnabled = {},
                onSetBackgroundTintAlpha = {},
                onSetBackgroundImageAlpha = {},
                onResetBackgroundSettings = {},
                onSetReportsColorMode = {},
                onAddUnit = { _, _, _ -> },
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
                onBackupDatabase = {},
                onRestoreDatabase = {},
                onTotalExport = {},
                getSuggestedBackupFileName = { "" },
                getSuggestedTotalExportFileName = { "" },
                snackbarHostState = remember { SnackbarHostState() },
                googleAccountName = null,
                onGoogleAccountSelected = {},
                syncCalendarByDefault = false,
                onSyncCalendarByDefaultChange = {},
                globalUsageWindowValue = 30,
                globalUsageWindowUnit = "DAYS",
                onSetGlobalUsageWindow = { _, _ -> },
                globalVisibilityHorizonValue = 30,
                globalVisibilityHorizonUnit = "DAYS",
                onSetGlobalVisibilityHorizon = { _, _ -> },
                costAnalysisWindowValue = 12,
                costAnalysisWindowUnit = "MONTHS",
                onSetCostAnalysisWindow = { _, _ -> },
                onRecalculateAccumulated = {}
            )
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val aboutLabel = context.getString(R.string.button_about)

        composeTestRule.onNodeWithText(aboutLabel, ignoreCase = true).performScrollTo().performClick()
        
        composeTestRule.waitForIdle()
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
                onSetBackgroundUri = {},
                onSetBackgroundBlur = {},
                onSetBackgroundSaturation = {},
                onSetBackgroundTintEnabled = {},
                onSetBackgroundTintAlpha = {},
                onSetBackgroundImageAlpha = {},
                onResetBackgroundSettings = {},
                onSetReportsColorMode = {},
                onAddUnit = { _, _, _ -> },
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
                onBackupDatabase = {},
                onRestoreDatabase = {},
                onTotalExport = {},
                getSuggestedBackupFileName = { "" },
                getSuggestedTotalExportFileName = { "" },
                snackbarHostState = remember { SnackbarHostState() },
                googleAccountName = null,
                onGoogleAccountSelected = {},
                syncCalendarByDefault = false,
                onSyncCalendarByDefaultChange = {},
                globalUsageWindowValue = 30,
                globalUsageWindowUnit = "DAYS",
                onSetGlobalUsageWindow = { _, _ -> },
                globalVisibilityHorizonValue = 30,
                globalVisibilityHorizonUnit = "DAYS",
                onSetGlobalVisibilityHorizon = { _, _ -> },
                costAnalysisWindowValue = 12,
                costAnalysisWindowUnit = "MONTHS",
                onSetCostAnalysisWindow = { _, _ -> },
                onRecalculateAccumulated = {}
            )
        }

        composeTestRule.onNodeWithText("OK").performClick()

        assertFalse(callbackValue.get())
    }
}
