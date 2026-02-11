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
class OperationTypeDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var operationTypeDao: OperationTypeDao

    @Before
    fun setupDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        operationTypeDao = database.operationTypeDao()
    }

    @After
    fun closeDatabase() {
        database.close()
        stopKoin()
    }

    @Test
    fun insertOperationType_whenNewTypeIsAdded_updatesActiveTypesFlow() = runTest {
        val operationType = OperationType(id = 1, description = "Oil Change", displayOrder = 0)
        operationTypeDao.insertOperationType(operationType)

        operationTypeDao.getActiveOperationTypes().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("Oil Change", list[0].description)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateOperationType_withValidData_reflectsChangesInFlow() = runTest {
        val initialType = OperationType(id = 1, description = "Initial Op", displayOrder = 0)
        operationTypeDao.insertOperationType(initialType)

        operationTypeDao.getActiveOperationTypes().test {
            assertEquals("Initial Op", awaitItem().first().description)
            cancelAndIgnoreRemainingEvents()
        }

        val updatedType = initialType.copy(description = "Updated Op")
        operationTypeDao.updateOperationType(updatedType)

        operationTypeDao.getActiveOperationTypes().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("Updated Op", list[0].description)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun dismissOperationType_whenTypeIsDismissed_isRemovedFromActiveTypesFlow() = runTest {
        val operationType = OperationType(id = 1, description = "Test Op", dismissed = false, displayOrder = 0)
        operationTypeDao.insertOperationType(operationType)

        // Verify it's initially active
        operationTypeDao.getActiveOperationTypes().test {
            assertEquals(1, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }

        // Dismiss the type
        val dismissedType = operationType.copy(dismissed = true)
        operationTypeDao.updateOperationType(dismissedType)

        // Verify it's no longer in the active list
        operationTypeDao.getActiveOperationTypes().test {
            assertEquals(0, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }

        // Verify it's still in the "all types" list
        operationTypeDao.getAllOperationTypes().test {
            val allTypes = awaitItem()
            assertEquals(1, allTypes.size)
            assertTrue(allTypes.first().dismissed)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
