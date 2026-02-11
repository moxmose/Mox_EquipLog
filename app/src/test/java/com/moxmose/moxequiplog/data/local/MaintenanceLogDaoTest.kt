package com.moxmose.moxequiplog.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SimpleSQLiteQuery
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
@Config(manifest = Config.NONE)
class MaintenanceLogDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var maintenanceLogDao: MaintenanceLogDao
    private lateinit var equipmentDao: EquipmentDao
    private lateinit var operationTypeDao: OperationTypeDao

    @Before
    fun setupDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        maintenanceLogDao = database.maintenanceLogDao()
        equipmentDao = database.equipmentDao()
        operationTypeDao = database.operationTypeDao()
    }

    @After
    fun closeDatabase() {
        database.close()
        stopKoin()
    }

    @Test
    fun insertLog_whenDependenciesExist_retrievesLogWithDetails() = runTest {
        val equipment = Equipment(id = 1, description = "Mountain Equipment")
        val operationType = OperationType(id = 1, description = "Clean Chain")
        equipmentDao.insertEquipment(equipment)
        operationTypeDao.insertOperationType(operationType)

        val log = MaintenanceLog(id = 1, equipmentId = 1, operationTypeId = 1, date = System.currentTimeMillis())
        maintenanceLogDao.insertLog(log)

        val query = SimpleSQLiteQuery("SELECT l.*, e.description as equipmentDescription, ot.description as operationTypeDescription, e.photoUri as equipmentPhotoUri, ot.photoUri as operationTypePhotoUri, ot.iconIdentifier as operationTypeIconIdentifier, e.dismissed as equipmentDismissed, ot.dismissed as operationTypeDismissed FROM maintenance_logs as l JOIN equipments as e ON l.equipmentId = e.id JOIN operation_types as ot ON l.operationTypeId = ot.id WHERE l.dismissed = 0")

        maintenanceLogDao.getLogsWithDetails(query).test {
            val logDetailsList = awaitItem()
            assertEquals(1, logDetailsList.size)
            val logDetails = logDetailsList[0]
            assertEquals(1, logDetails.log.id)
            assertEquals("Mountain Equipment", logDetails.equipmentDescription)
            assertEquals("Clean Chain", logDetails.operationTypeDescription)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getLogsWithDetails_withSearchQuery_returnsMatchingLogs() = runTest {
        val equipment1 = Equipment(id = 1, description = "Road Equipment")
        val equipment2 = Equipment(id = 2, description = "Mountain Equipment")
        val op1 = OperationType(id = 1, description = "Fix Brakes")
        val op2 = OperationType(id = 2, description = "Clean Chain")
        equipmentDao.insertEquipment(equipment1)
        equipmentDao.insertEquipment(equipment2)
        operationTypeDao.insertOperationType(op1)
        operationTypeDao.insertOperationType(op2)
        maintenanceLogDao.insertLog(MaintenanceLog(equipmentId = 1, operationTypeId = 1, date = System.currentTimeMillis()))
        maintenanceLogDao.insertLog(MaintenanceLog(equipmentId = 2, operationTypeId = 2, notes = "Used a specific chain cleaner", date = System.currentTimeMillis()))

        val searchTerm = "%Chain%"
        val query = SimpleSQLiteQuery("SELECT l.*, e.description as equipmentDescription, ot.description as operationTypeDescription, e.photoUri as equipmentPhotoUri, ot.photoUri as operationTypePhotoUri, ot.iconIdentifier as operationTypeIconIdentifier, e.dismissed as equipmentDismissed, ot.dismissed as operationTypeDismissed FROM maintenance_logs as l JOIN equipments as e ON l.equipmentId = e.id JOIN operation_types as ot ON l.operationTypeId = ot.id WHERE l.dismissed = 0 AND (ot.description LIKE ? OR l.notes LIKE ?)", arrayOf(searchTerm, searchTerm))

        maintenanceLogDao.getLogsWithDetails(query).test {
            val logDetailsList = awaitItem()
            assertEquals(1, logDetailsList.size)
            assertEquals("Clean Chain", logDetailsList[0].operationTypeDescription)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getLogsWithDetails_withDateSort_returnsSortedLogs() = runTest {
        val equipment = Equipment(id = 1, description = "Test Equipment")
        val op = OperationType(id = 1, description = "Test Op")
        equipmentDao.insertEquipment(equipment)
        operationTypeDao.insertOperationType(op)

        val olderLog = MaintenanceLog(id = 1, equipmentId = 1, operationTypeId = 1, date = 1000L)
        val newerLog = MaintenanceLog(id = 2, equipmentId = 1, operationTypeId = 1, date = 2000L)

        maintenanceLogDao.insertLog(olderLog)
        maintenanceLogDao.insertLog(newerLog)

        val query = SimpleSQLiteQuery("SELECT l.*, e.description as equipmentDescription, ot.description as operationTypeDescription, e.photoUri as equipmentPhotoUri, ot.photoUri as operationTypePhotoUri, ot.iconIdentifier as operationTypeIconIdentifier, e.dismissed as equipmentDismissed, ot.dismissed as operationTypeDismissed FROM maintenance_logs as l JOIN equipments as e ON l.equipmentId = e.id JOIN operation_types as ot ON l.operationTypeId = ot.id WHERE l.dismissed = 0 ORDER BY l.date DESC")

        maintenanceLogDao.getLogsWithDetails(query).test {
            val logDetailsList = awaitItem()
            assertEquals(2, logDetailsList.size)
            assertEquals(newerLog.id, logDetailsList[0].log.id)
            assertEquals(olderLog.id, logDetailsList[1].log.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun dismissLog_whenLogIsDismissed_isRemovedFromActiveLogsQuery() = runTest {
        val equipment = Equipment(id = 1, description = "Test Equipment")
        val op = OperationType(id = 1, description = "Test Op")
        equipmentDao.insertEquipment(equipment)
        operationTypeDao.insertOperationType(op)
        val log = MaintenanceLog(id = 1, equipmentId = 1, operationTypeId = 1, date = 1000L)
        maintenanceLogDao.insertLog(log)

        val activeQuery = SimpleSQLiteQuery("SELECT l.*, e.description as equipmentDescription, ot.description as operationTypeDescription, e.photoUri as equipmentPhotoUri, ot.photoUri as operationTypePhotoUri, ot.iconIdentifier as operationTypeIconIdentifier, e.dismissed as equipmentDismissed, ot.dismissed as operationTypeDismissed FROM maintenance_logs as l JOIN equipments as e ON l.equipmentId = e.id JOIN operation_types as ot ON l.operationTypeId = ot.id WHERE l.dismissed = 0")
        maintenanceLogDao.getLogsWithDetails(activeQuery).test {
            assertEquals(1, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }

        maintenanceLogDao.updateLog(log.copy(dismissed = true))

        maintenanceLogDao.getLogsWithDetails(activeQuery).test {
            assertEquals(0, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getLogs_whenDependencyIsDismissed_returnsLogWithDismissedFlag() = runTest {
        val equipment = Equipment(id = 1, description = "Mountain Equipment", dismissed = false)
        val op = OperationType(id = 1, description = "Clean Chain")
        equipmentDao.insertEquipment(equipment)
        operationTypeDao.insertOperationType(op)
        val log = MaintenanceLog(id = 1, equipmentId = 1, operationTypeId = 1, date = System.currentTimeMillis())
        maintenanceLogDao.insertLog(log)

        equipmentDao.updateEquipment(equipment.copy(dismissed = true))
        
        val query = SimpleSQLiteQuery("SELECT l.*, e.description as equipmentDescription, ot.description as operationTypeDescription, e.photoUri as equipmentPhotoUri, ot.photoUri as operationTypePhotoUri, ot.iconIdentifier as operationTypeIconIdentifier, e.dismissed as equipmentDismissed, ot.dismissed as operationTypeDismissed FROM maintenance_logs as l JOIN equipments as e ON l.equipmentId = e.id JOIN operation_types as ot ON l.operationTypeId = ot.id")
        
        maintenanceLogDao.getLogsWithDetails(query).test {
            val item = awaitItem().first()
            assertTrue(item.equipmentDismissed)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
