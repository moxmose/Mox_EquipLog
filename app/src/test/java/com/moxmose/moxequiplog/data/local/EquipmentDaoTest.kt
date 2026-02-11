package com.moxmose.moxequiplog.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
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

    @Before
    fun setupDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries() // For simplicity in tests
            .build()
        equipmentDao = database.equipmentDao()
    }

    @After
    fun closeDatabase() {
        database.close()
        stopKoin()
    }

    @Test
    fun insertEquipment_whenNewEquipmentIsAdded_updatesActiveEquipmentsFlow() = runTest {
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

        // First, verify the initial state
        equipmentDao.getActiveEquipments().test {
            assertEquals("Initial Name", awaitItem().first().description)
            cancelAndIgnoreRemainingEvents()
        }

        val updatedEquipment = initialEquipment.copy(description = "Updated Name")
        equipmentDao.updateEquipment(updatedEquipment)

        // Then, verify the updated state
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

        // 1. Verify it's initially active
        equipmentDao.getActiveEquipments().test {
            assertEquals(1, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }

        // 2. Dismiss the equipment
        val dismissedEquipment = equipment.copy(dismissed = true)
        equipmentDao.updateEquipment(dismissedEquipment)

        // 3. Verify it's no longer in the active list
        equipmentDao.getActiveEquipments().test {
            assertEquals(0, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }

        // 4. Verify it's still in the "all equipments" list
        equipmentDao.getAllEquipments().test {
            val allEquipments = awaitItem()
            assertEquals(1, allEquipments.size)
            assertTrue(allEquipments.first().dismissed)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
