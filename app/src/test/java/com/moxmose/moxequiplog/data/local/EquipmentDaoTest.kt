package com.moxmose.moxequiplog.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.moxmose.moxequiplog.utils.AppConstants
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class EquipmentDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var equipmentDao: EquipmentDao
    private lateinit var measurementUnitDao: MeasurementUnitDao

    @Before
    fun setupDatabase() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        equipmentDao = database.equipmentDao()
        measurementUnitDao = database.measurementUnitDao()

        // Popolamento manuale delle unità per i test per soddisfare il vincolo FK
        AppConstants.INITIAL_MEASUREMENT_UNITS.forEach { unit ->
            measurementUnitDao.insertUnit(unit)
        }
    }

    @After
    fun closeDatabase() {
        database.close()
        stopKoin()
    }

    @Test
    fun insertEquipment_whenNewEquipmentIsAdded_updatesActiveEquipmentsFlow() = runTest {
        // unitId defaults to 1, which now exists in the DB
        val equipment = Equipment(id = 1, description = "Test Equipment", displayOrder = 0)

        equipmentDao.insertEquipment(equipment)

        equipmentDao.getActiveEquipments().test {
            val equipmentList = awaitItem()
            assertEquals(1, equipmentList.size)
            assertEquals("Test Equipment", equipmentList[0].description)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateEquipment_withValidData_reflectsChangesInFlow() = runTest {
        val initialEquipment = Equipment(id = 1, description = "Initial Name", displayOrder = 0)
        equipmentDao.insertEquipment(initialEquipment)

        equipmentDao.getActiveEquipments().test {
            assertEquals("Initial Name", awaitItem().first().description)
            cancelAndIgnoreRemainingEvents()
        }

        val updatedEquipment = initialEquipment.copy(description = "Updated Name")
        equipmentDao.updateEquipment(updatedEquipment)

        equipmentDao.getActiveEquipments().test {
            val equipmentList = awaitItem()
            assertEquals(1, equipmentList.size)
            assertEquals("Updated Name", equipmentList[0].description)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun dismissEquipment_whenEquipmentIsDismissed_isRemovedFromActiveEquipmentsFlow() = runTest {
        val equipment = Equipment(id = 1, description = "Test Equipment", dismissed = false, displayOrder = 0)
        equipmentDao.insertEquipment(equipment)

        equipmentDao.getActiveEquipments().test {
            assertEquals(1, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }

        val dismissedEquipment = equipment.copy(dismissed = true)
        equipmentDao.updateEquipment(dismissedEquipment)

        equipmentDao.getActiveEquipments().test {
            assertEquals(0, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
