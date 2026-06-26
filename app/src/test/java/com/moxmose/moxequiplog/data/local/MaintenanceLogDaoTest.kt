package com.moxmose.moxequiplog.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.moxmose.moxequiplog.utils.AppConstants
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
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
    private lateinit var measurementUnitDao: MeasurementUnitDao
    private lateinit var sectionDao: SectionDao

    @Before
    fun setupDatabase() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
            maintenanceLogDao = database.maintenanceLogDao()
            equipmentDao = database.equipmentDao()
            operationTypeDao = database.operationTypeDao()
            measurementUnitDao = database.measurementUnitDao()
            sectionDao = database.sectionDao()

            // Popolamento manuale delle unità e sezioni necessario per Equipment/OperationType FK
            AppConstants.INITIAL_MEASUREMENT_UNITS.forEach { unit ->
                measurementUnitDao.insertUnit(unit)
            }
            sectionDao.insertSection(Section(id = AppConstants.DEFAULT_SECTION_ID, name = AppConstants.DEFAULT_SECTION_NAME))
        }
    }

    @After
    fun closeDatabase() {
        database.close()
        stopKoin()
    }

    @Test
    fun insertLog_whenDependenciesExist_retrievesLogWithDetails() = runTest {
        val equipment = Equipment(description = "Mountain Equipment")
        val operationType = OperationType(description = "Clean Chain")
        val eqId = equipmentDao.insertEquipment(equipment).toInt()
        val opId = operationTypeDao.insertOperationType(operationType).toInt()

        val log = MaintenanceLog(id = 1, equipmentId = eqId, operationTypeId = opId, date = System.currentTimeMillis())
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
        val equipment1 = Equipment(description = "Road Equipment")
        val equipment2 = Equipment(description = "Mountain Equipment")
        val op1 = OperationType(description = "Fix Brakes")
        val op2 = OperationType(description = "Clean Chain")
        val eq1Id = equipmentDao.insertEquipment(equipment1).toInt()
        val eq2Id = equipmentDao.insertEquipment(equipment2).toInt()
        val op1Id = operationTypeDao.insertOperationType(op1).toInt()
        val op2Id = operationTypeDao.insertOperationType(op2).toInt()
        maintenanceLogDao.insertLog(MaintenanceLog(equipmentId = eq1Id, operationTypeId = op1Id, date = System.currentTimeMillis()))
        maintenanceLogDao.insertLog(MaintenanceLog(equipmentId = eq2Id, operationTypeId = op2Id, notes = "Used a specific chain cleaner", date = System.currentTimeMillis()))

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
        val equipment = Equipment(description = "Test Equipment")
        val op = OperationType(description = "Test Op")
        val eqId = equipmentDao.insertEquipment(equipment).toInt()
        val opId = operationTypeDao.insertOperationType(op).toInt()

        val olderLog = MaintenanceLog(id = 1, equipmentId = eqId, operationTypeId = opId, date = 1000L)
        val newerLog = MaintenanceLog(id = 2, equipmentId = eqId, operationTypeId = opId, date = 2000L)

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
        val equipment = Equipment(description = "Test Equipment")
        val op = OperationType(description = "Test Op")
        val eqId = equipmentDao.insertEquipment(equipment).toInt()
        val opId = operationTypeDao.insertOperationType(op).toInt()
        val log = MaintenanceLog(id = 1, equipmentId = eqId, operationTypeId = opId, date = 1000L)
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
        val equipment = Equipment(description = "Mountain Equipment", dismissed = false)
        val op = OperationType(description = "Clean Chain")
        val eqId = equipmentDao.insertEquipment(equipment).toInt()
        val opId = operationTypeDao.insertOperationType(op).toInt()
        val log = MaintenanceLog(id = 1, equipmentId = eqId, operationTypeId = opId, date = System.currentTimeMillis())
        maintenanceLogDao.insertLog(log)

        val insertedEquipment = equipment.copy(id = eqId, dismissed = true)
        equipmentDao.updateEquipment(insertedEquipment)
        
        val query = SimpleSQLiteQuery("SELECT l.*, e.description as equipmentDescription, ot.description as operationTypeDescription, e.photoUri as equipmentPhotoUri, ot.photoUri as operationTypePhotoUri, ot.iconIdentifier as operationTypeIconIdentifier, e.dismissed as equipmentDismissed, ot.dismissed as operationTypeDismissed FROM maintenance_logs as l JOIN equipments as e ON l.equipmentId = e.id JOIN operation_types as ot ON l.operationTypeId = ot.id")
        
        maintenanceLogDao.getLogsWithDetails(query).test {
            val item = awaitItem().first()
            assertTrue(item.equipmentDismissed)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
