package com.moxmose.moxequiplog.ui.options

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.MediaRepository
import com.moxmose.moxequiplog.data.local.AppColor
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.EquipmentDao
import com.moxmose.moxequiplog.data.local.Media
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class OptionsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var appSettingsManager: AppSettingsManager
    private lateinit var equipmentDao: EquipmentDao
    private lateinit var mediaRepository: MediaRepository
    private lateinit var viewModel: OptionsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val testContext: Context = ApplicationProvider.getApplicationContext()
        testDataStore = PreferenceDataStoreFactory.create(
            produceFile = { testContext.preferencesDataStoreFile("test_settings") }
        )
        appSettingsManager = AppSettingsManager(testContext)
        equipmentDao = mockk(relaxed = true)
        mediaRepository = mockk(relaxed = true)
        viewModel = OptionsViewModel(appSettingsManager, equipmentDao, mediaRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun username_onInit_isEmpty() = runTest {
        viewModel.username.test {
            assertEquals("", awaitItem())
        }
    }

    @Test
    fun allCategories_onInit_isEmpty() = runTest {
        viewModel.allCategories.test {
            assertEquals(emptyList<Category>(), awaitItem())
        }
    }

    @Test
    fun allColors_onInit_isEmpty() = runTest {
        viewModel.allColors.test {
            assertEquals(emptyList<AppColor>(), awaitItem())
        }
    }

    @Test
    fun setUsername_withValidUsername_updatesUsernameFlow() = runTest {
        val newUsername = "testuser"
        viewModel.username.test {
            assertEquals("", awaitItem()) // Initial value
            viewModel.setUsername(newUsername)
            assertEquals(newUsername, awaitItem())
        }
    }

    @Test
    fun setCategoryDefault_withValidData_callsRepository() = runTest {
        val categoryId = "test_category"
        val iconId = "test_icon"
        val photoUri = "test_uri"

        // Esegui la funzione sul ViewModel
        viewModel.setCategoryDefault(categoryId, iconId, photoUri)

        // Esegui le coroutine in sospeso
        testDispatcher.scheduler.advanceUntilIdle()

        // Verifica che il metodo corrispondente sul repository sia stato chiamato con i parametri corretti
        coVerify { mediaRepository.setCategoryDefault(categoryId, iconId, photoUri) }
    }

    @Test
    fun addMedia_withValidData_callsRepository() = runTest {
        val uri = "test_uri"
        val category = "test_category"

        // Chiama la funzione sul ViewModel
        viewModel.addMedia(uri, category)

        // Fai avanzare lo scheduler per eseguire la coroutine in sospeso
        testDispatcher.scheduler.advanceUntilIdle()

        // Verifica che il metodo del repository sia stato chiamato con i parametri corretti
        coVerify { mediaRepository.addMedia(uri, category) }
    }

    @Test
    fun toggleMediaVisibility_withValidData_callsRepository() = runTest {
        val uri = "test_uri"
        val category = "test_category"

        // Chiama la funzione sul ViewModel
        viewModel.toggleMediaVisibility(uri, category)

        // Fai avanzare lo scheduler per eseguire la coroutine
        testDispatcher.scheduler.advanceUntilIdle()

        // Verifica che il ViewModel abbia delegato la chiamata al repository
        coVerify { mediaRepository.toggleMediaVisibility(uri, category) }
    }

    @Test
    fun removeMedia_withValidData_callsRepository() = runTest {
        val uri = "test_uri"
        val category = "test_category"

        // Chiama la funzione sul ViewModel
        viewModel.removeMedia(uri, category)

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mediaRepository.removeMedia(uri, category) }
    }

    @Test
    fun removeMedia_whenRepositoryThrowsError_sendsUiEvent() = runTest {
        val uri = "test_uri"
        val category = "test_category"
        val error = RuntimeException("Simulated database error")

        // Istruisci il mock a lanciare un'''eccezione quando viene chiamato
        coEvery { mediaRepository.removeMedia(uri, category) } throws error

        // Avvia una coroutine per ascoltare gli eventi in modo non bloccante
        val job = launch {
            viewModel.uiEvents.test {
                assertEquals(OptionsViewModel.OptionsUiEvent.RemoveMediaFailed, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

        // Chiama la funzione che causa l'''errore
        viewModel.removeMedia(uri, category)

        // Attendi che il test nel launch finisca
        job.join()
    }

    @Test
    fun updateMediaOrder_withEmptyList_doesNothing() = runTest {
        viewModel.updateMediaOrder(emptyList())

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { mediaRepository.updateMediaOrder(any()) }
    }

    @Test
    fun updateMediaOrder_withValidData_callsRepository() = runTest {
        val mediaList: List<Media> = listOf(mockk(), mockk())

        // Chiama la funzione sul ViewModel
        viewModel.updateMediaOrder(mediaList)

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mediaRepository.updateMediaOrder(mediaList)}
    }

    @Test
    fun updateCategoryColor_withValidData_callsRepository() = runTest {
        val categoryId = "any_valid_category_id"
        val colorHex = "#00FFBB"

        // Chiama la funzione sul ViewModel
        viewModel.updateCategoryColor(categoryId, colorHex)

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mediaRepository.updateCategoryColor(categoryId, colorHex) }
    }

    @Test
    fun addColor_withValidData_callsRepository() = runTest {
        val hex = "#00FFBB"
        val name = "ColorName"

        // Chiama la funzione sul ViewModel
        viewModel.addColor(hex, name)

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mediaRepository.addColor(hex, name) }
    }

    @Test
    fun updateColor_whenNameIsBlank_sendsInvalidNameEvent() = runTest {
        val invalidColor: AppColor = mockk()
        every { invalidColor.name } returns " "

        viewModel.uiEvents.test {
            viewModel.updateColor(invalidColor)
            assertEquals(OptionsViewModel.OptionsUiEvent.ColorNameInvalid, awaitItem())
        }
    }

    @Test
    fun updateColor_whenRepositoryThrowsError_sendsUpdateFailedEvent() = runTest {
        val validColor: AppColor = mockk()
        every { validColor.name } returns "Valid Name"
        coEvery { mediaRepository.updateColor(validColor) } throws RuntimeException()

        viewModel.uiEvents.test {
            viewModel.updateColor(validColor)
            assertEquals(OptionsViewModel.OptionsUiEvent.UpdateColorFailed, awaitItem())
        }
    }

    @Test
    fun updateColor_withValidData_callsRepository() = runTest {
        val validColor: AppColor = mockk()
        every { validColor.name } returns "Valid Name"

        viewModel.updateColor(validColor)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mediaRepository.updateColor(validColor) }
    }

    @Test
    fun isPhotoUsed_whenUriIsBlank_sendsUriInvalidEventAndReturnsTrue() = runTest {
        viewModel.uiEvents.test {
            val result = viewModel.isPhotoUsed(" ")
            assertTrue(result)
            assertEquals(OptionsViewModel.OptionsUiEvent.PhotoUriInvalid, awaitItem())
        }
    }

    @Test
    fun isPhotoUsed_whenDaoThrowsError_sendsCheckFailedEventAndReturnsTrue() = runTest {
        val uri = "test_uri"
        coEvery { equipmentDao.countEquipmentsUsingPhoto(uri) } throws RuntimeException()

        viewModel.uiEvents.test {
            val result = viewModel.isPhotoUsed(uri)
            assertTrue(result)
            assertEquals(OptionsViewModel.OptionsUiEvent.DatabaseCheckFailed, awaitItem())
        }
    }

    @Test
    fun isPhotoUsed_whenPhotoIsInUse_returnsTrue() = runTest {
        val uri = "test_uri"
        coEvery { equipmentDao.countEquipmentsUsingPhoto(uri) } returns 1

        val result = viewModel.isPhotoUsed(uri)

        assertTrue(result)
    }

    @Test
    fun isPhotoUsed_whenPhotoIsNotInUse_returnsFalse() = runTest {
        val uri = "test_uri"
        coEvery { equipmentDao.countEquipmentsUsingPhoto(uri) } returns 0

        val result = viewModel.isPhotoUsed(uri)

        assertFalse(result)
    }
}
