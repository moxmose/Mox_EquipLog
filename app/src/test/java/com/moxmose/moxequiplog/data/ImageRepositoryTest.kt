package com.moxmose.moxequiplog.data

import com.moxmose.moxequiplog.data.local.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ImageRepositoryTest {

    private lateinit var imageDao: ImageDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var appColorDao: AppColorDao
    private lateinit var appPreferenceDao: AppPreferenceDao
    private lateinit var repository: ImageRepository

    private val defaultColors = arrayOf("#FF0000;Red", "#00FF00;Green")
    private val defaultCategories = arrayOf("LOGS;Registro;#808080", "EQUIPMENT;Mezzi;#0000FF")

    @Before
    fun setup() {
        imageDao = mockk(relaxed = true)
        categoryDao = mockk(relaxed = true)
        appColorDao = mockk(relaxed = true)
        appPreferenceDao = mockk(relaxed = true)

        repository = ImageRepository(
            imageDao, categoryDao, appColorDao, appPreferenceDao,
            defaultColors, defaultCategories
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun initializeAppData_insertsMissingCategoriesAndColors() = runTest {
        every { appColorDao.getAllColors() } returns flowOf(emptyList())
        every { categoryDao.getAllCategories() } returns flowOf(emptyList())
        every { imageDao.getImagesByCategory(any()) } returns flowOf(emptyList())

        repository.initializeAppData()

        coVerify { appColorDao.insertAllColors(any()) }
        coVerify { categoryDao.insertCategory(any()) }
        coVerify { appPreferenceDao.insertPreference(match { it.key.startsWith("default_color_") }) }
    }

    @Test
    fun setCategoryDefault_updatesPreferencesCorrectly() = runTest {
        val categoryId = "TEST_CAT"

        // Caso Icona
        repository.setCategoryDefault(categoryId, ImageIdentifier.Icon("test_icon"))
        coVerify {
            appPreferenceDao.insertPreference(match { it.key == "default_icon_$categoryId" && it.value == "test_icon" })
            appPreferenceDao.deletePreference("default_photo_$categoryId")
        }

        // Caso Foto
        repository.setCategoryDefault(categoryId, ImageIdentifier.Photo("test_uri"))
        coVerify {
            appPreferenceDao.deletePreference("default_icon_$categoryId")
            appPreferenceDao.insertPreference(match { it.key == "default_photo_$categoryId" && it.value == "test_uri" })
        }
    }

    @Test
    fun updateCategoryColor_callsDao() = runTest {
        repository.updateCategoryColor("CAT1", "#FFFFFF")
        coVerify { appPreferenceDao.insertPreference(match { it.key == "default_color_CAT1" && it.value == "#FFFFFF" }) }
    }

    @Test
    fun addImage_calculatesOrderCorrectly() = runTest {
        // Usiamo coEvery perché getMaxOrder è una funzione suspend
        coEvery { imageDao.getMaxOrder("CAT") } returns 5

        repository.addImage(ImageIdentifier.Icon("new_icon"), "CAT")

        // Per la verifica di funzioni suspend con matchers complessi,
        // a volte è necessario usare un approccio più esplicito o any()
        coVerify {
            imageDao.insertImage(match {
                it.uri == "icon:new_icon" && it.displayOrder == 6 && it.category == "CAT"
            })
        }
    }
}