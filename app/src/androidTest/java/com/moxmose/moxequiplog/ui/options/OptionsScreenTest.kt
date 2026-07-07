package com.moxmose.moxequiplog.ui.options

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.moxmose.moxequiplog.R
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

    companion object {
        private const val TEST_USERNAME = "JohnDoe"
        private const val NEW_USERNAME = "NewUser"
        private const val REPORTS_COLOR_MODE_NONE = "NONE"
        private const val DEFAULT_COST_WINDOW_VALUE = 12
    }

    private fun setOptionsContent(
        username: String = "",
        onUsernameChange: (String) -> Unit = {},
        showAboutDialog: Boolean = false,
        onShowAboutDialogChange: (Boolean) -> Unit = {},
        reportsColorMode: String = REPORTS_COLOR_MODE_NONE
    ) {
        composeTestRule.setContent {
            OptionsScreenContent(
                username = username,
                allImages = emptyList(),
                categoriesUiState = emptyList(),
                allColors = emptyList(),
                reportsColors = emptyList(),
                allSections = emptyList(),
                measurementUnits = emptyList(),
                defaultUnitId = null,
                backgroundUri = null,
                backgroundBlur = UiConstants.DEFAULT_BACKGROUND_BLUR,
                backgroundSaturation = UiConstants.DEFAULT_BACKGROUND_SATURATION,
                backgroundTintEnabled = UiConstants.DEFAULT_BACKGROUND_TINT_ENABLED,
                backgroundTintAlpha = UiConstants.DEFAULT_BACKGROUND_TINT_ALPHA,
                backgroundImageAlpha = UiConstants.DEFAULT_BACKGROUND_IMAGE_ALPHA,
                reportsColorMode = reportsColorMode,
                onUsernameChange = onUsernameChange,
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
                onCloneUnit = {},
                onToggleDefaultUnit = {},
                onAddSection = { _, _, _, _, _ -> },
                onUpdateSection = {},
                onDeleteSection = {},
                onCloneSection = {},
                onUpdateSectionsOrder = {},
                onShowColorManagerCustom = {},
                unitUsageCounts = emptyMap(),
                sectionUsageCounts = emptyMap(),
                isPhotoUsed = { false },
                showAboutDialog = showAboutDialog,
                onShowAboutDialogChange = onShowAboutDialogChange,
                onShowColorManager = { _, _ -> },
                showImageDialog = false,
                onShowImageDialogChange = {},
                onBackupDatabase = {},
                onRestoreDatabase = {},
                onTotalExport = {},
                onTotalImport = {},
                onGenerateDemoData = {},
                onDeleteDemoData = {},
                getSuggestedBackupFileName = { "" },
                getSuggestedTotalExportFileName = { "" },
                snackbarHostState = remember { SnackbarHostState() },
                googleAccountName = null,
                onGoogleAccountSelected = {},
                syncCalendarByDefault = false,
                onSyncCalendarByDefaultChange = {},
                globalUsageWindowValue = UiConstants.DEFAULT_USAGE_WINDOW_VALUE,
                globalUsageWindowUnit = UiConstants.DEFAULT_USAGE_WINDOW_UNIT,
                onSetGlobalUsageWindow = { _, _ -> },
                globalVisibilityHorizonValue = UiConstants.DEFAULT_VISIBILITY_HORIZON_VALUE,
                globalVisibilityHorizonUnit = UiConstants.DEFAULT_VISIBILITY_HORIZON_UNIT,
                onSetGlobalVisibilityHorizon = { _, _ -> },
                costAnalysisWindowValue = DEFAULT_COST_WINDOW_VALUE,
                costAnalysisWindowUnit = UiConstants.DEFAULT_COST_ANALYSIS_WINDOW_UNIT,
                onSetCostAnalysisWindow = { _, _ -> },
                costTrendThreshold = UiConstants.DEFAULT_COST_TREND_THRESHOLD,
                onSetCostTrendThreshold = {},
                onSetSectionSelectorType = {},
                onRecalculateAccumulated = {},
            )
        }
    }

    @Test
    fun username_isDisplayedCorrectly() {
        setOptionsContent(username = TEST_USERNAME)
        composeTestRule.onNodeWithText(TEST_USERNAME).assertIsDisplayed()
    }

    @Test
    fun onUsernameChange_isCalled_whenTextIsEntered() {
        val changedUsername = AtomicReference<String>()

        setOptionsContent(onUsernameChange = { changedUsername.set(it) })

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val label = context.getString(R.string.options_username_field_label)
        val saveAction = context.getString(R.string.options_save_username)

        composeTestRule.onNodeWithText(label, ignoreCase = true)
            .performScrollTo()
            .performTextInput(NEW_USERNAME)
        
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithContentDescription(saveAction, ignoreCase = true)
            .performScrollTo()
            .performClick()

        assertEquals(NEW_USERNAME, changedUsername.get())
    }

    @Test
    fun aboutButton_onClick_invokesOnShowAboutDialogChange() {
        val onShowAboutDialogChangeCalled = AtomicBoolean(false)

        setOptionsContent(onShowAboutDialogChange = { onShowAboutDialogChangeCalled.set(it) })

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val aboutLabel = context.getString(R.string.button_about)

        composeTestRule.onNodeWithText(aboutLabel, ignoreCase = true).performScrollTo().performClick()
        
        composeTestRule.waitForIdle()
        assertTrue(onShowAboutDialogChangeCalled.get())
    }

    @Test
    fun aboutDialog_onDismiss_invokesOnShowAboutDialogChange() {
        val callbackValue = AtomicReference<Boolean>()

        setOptionsContent(
            showAboutDialog = true,
            onShowAboutDialogChange = { callbackValue.set(it) }
        )

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val okLabel = context.getString(R.string.button_ok)

        composeTestRule.onNodeWithText(okLabel, ignoreCase = true).performClick()

        assertFalse(callbackValue.get())
    }
}
