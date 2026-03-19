package com.moxmose.moxequiplog.data

import app.cash.turbine.test
import com.moxmose.moxequiplog.data.local.AppPreference
import com.moxmose.moxequiplog.data.local.AppPreferenceDao
import com.moxmose.moxequiplog.utils.UiConstants
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AppSettingsManagerTest {

    private lateinit var appPreferenceDao: AppPreferenceDao
    private val defaultUsername = "DefaultUser"

    @Before
    fun setup() {
        appPreferenceDao = mockk()
        // Stub predefinito per TUTTE le chiamate getPreferenceFlow per evitare emptyFlow()
        every { appPreferenceDao.getPreferenceFlow(any()) } returns flowOf(null)
        coEvery { appPreferenceDao.insertPreference(any()) } returns Unit
        coEvery { appPreferenceDao.deletePreference(any()) } returns Unit
    }

    @Test
    fun backgroundUri_returnsValueFromDao() = runTest {
        every { appPreferenceDao.getPreferenceFlow("background_uri") } returns flowOf("content://test")
        val manager = AppSettingsManager(appPreferenceDao, defaultUsername)
        
        manager.backgroundUri.test {
            assertEquals("content://test", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun backgroundBlur_returnsDefaultWhenNull() = runTest {
        every { appPreferenceDao.getPreferenceFlow("background_blur") } returns flowOf(null)
        val manager = AppSettingsManager(appPreferenceDao, defaultUsername)
        
        manager.backgroundBlur.test {
            assertEquals(UiConstants.DEFAULT_BACKGROUND_BLUR, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun backgroundSaturation_returnsDefaultWhenNull() = runTest {
        every { appPreferenceDao.getPreferenceFlow("background_saturation") } returns flowOf(null)
        val manager = AppSettingsManager(appPreferenceDao, defaultUsername)
        
        manager.backgroundSaturation.test {
            assertEquals(UiConstants.DEFAULT_BACKGROUND_SATURATION, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun backgroundTintEnabled_returnsDefaultWhenNull() = runTest {
        every { appPreferenceDao.getPreferenceFlow("background_tint_enabled") } returns flowOf(null)
        val manager = AppSettingsManager(appPreferenceDao, defaultUsername)
        
        manager.backgroundTintEnabled.test {
            assertEquals(UiConstants.DEFAULT_BACKGROUND_TINT_ENABLED, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun backgroundTintAlpha_returnsDefaultWhenNull() = runTest {
        every { appPreferenceDao.getPreferenceFlow("background_tint_alpha") } returns flowOf(null)
        val manager = AppSettingsManager(appPreferenceDao, defaultUsername)
        
        manager.backgroundTintAlpha.test {
            assertEquals(UiConstants.DEFAULT_BACKGROUND_TINT_ALPHA, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun backgroundImageAlpha_returnsDefaultWhenNull() = runTest {
        every { appPreferenceDao.getPreferenceFlow("background_image_alpha") } returns flowOf(null)
        val manager = AppSettingsManager(appPreferenceDao, defaultUsername)
        
        manager.backgroundImageAlpha.test {
            assertEquals(UiConstants.DEFAULT_BACKGROUND_IMAGE_ALPHA, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setBackgroundUri_callsInsert() = runTest {
        val manager = AppSettingsManager(appPreferenceDao, defaultUsername)
        manager.setBackgroundUri("new_uri")
        coVerify { appPreferenceDao.insertPreference(AppPreference("background_uri", "new_uri")) }
    }

    @Test
    fun setBackgroundUri_null_callsDelete() = runTest {
        val manager = AppSettingsManager(appPreferenceDao, defaultUsername)
        manager.setBackgroundUri(null)
        coVerify { appPreferenceDao.deletePreference("background_uri") }
    }

    @Test
    fun setBackgroundBlur_callsInsert() = runTest {
        val manager = AppSettingsManager(appPreferenceDao, defaultUsername)
        manager.setBackgroundBlur(5.0f)
        coVerify { appPreferenceDao.insertPreference(AppPreference("background_blur", "5.0")) }
    }

    @Test
    fun setBackgroundSaturation_callsInsert() = runTest {
        val manager = AppSettingsManager(appPreferenceDao, defaultUsername)
        manager.setBackgroundSaturation(1.5f)
        coVerify { appPreferenceDao.insertPreference(AppPreference("background_saturation", "1.5")) }
    }

    @Test
    fun setBackgroundTintEnabled_callsInsert() = runTest {
        val manager = AppSettingsManager(appPreferenceDao, defaultUsername)
        manager.setBackgroundTintEnabled(true)
        coVerify { appPreferenceDao.insertPreference(AppPreference("background_tint_enabled", "true")) }
    }

    @Test
    fun setBackgroundTintAlpha_callsInsert() = runTest {
        val manager = AppSettingsManager(appPreferenceDao, defaultUsername)
        manager.setBackgroundTintAlpha(0.7f)
        coVerify { appPreferenceDao.insertPreference(AppPreference("background_tint_alpha", "0.7")) }
    }

    @Test
    fun setBackgroundImageAlpha_callsInsert() = runTest {
        val manager = AppSettingsManager(appPreferenceDao, defaultUsername)
        manager.setBackgroundImageAlpha(0.4f)
        coVerify { appPreferenceDao.insertPreference(AppPreference("background_image_alpha", "0.4")) }
    }
}
