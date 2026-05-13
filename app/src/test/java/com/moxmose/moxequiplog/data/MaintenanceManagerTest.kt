package com.moxmose.moxequiplog.data

import com.moxmose.moxequiplog.data.local.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.*

class MaintenanceManagerTest {

    private lateinit var maintenanceLogDao: MaintenanceLogDao
    private lateinit var equipmentDao: EquipmentDao
    private lateinit var operationTypeDao: OperationTypeDao
    private lateinit var maintenanceManager: MaintenanceManager

    @Before
    fun setup() {
        maintenanceLogDao = mockk()
        equipmentDao = mockk()
        operationTypeDao = mockk()
        maintenanceManager = MaintenanceManager(maintenanceLogDao, equipmentDao, operationTypeDao)
    }

    @Test
    fun `findAutoGranularity returns null when points are empty`() {
        val result = maintenanceManager.findAutoGranularity(emptyList(), false, false)
        assertNull(result)
    }

    @Test
    fun `findAutoGranularity returns HOURS when points are 2 hours apart`() {
        val cal = Calendar.getInstance().apply { 
            set(2023, 0, 1, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val time1 = cal.timeInMillis
        val time2 = time1 + (2 * 60 * 60 * 1000) // 2 hours apart, same day
        
        val points = listOf(
            ChartPoint(time1, 10f),
            ChartPoint(time2, 20f)
        )
        
        val result = maintenanceManager.findAutoGranularity(points, false, false)
        assertEquals(TimeGranularity.HOURS, result)
    }

    @Test
    fun `findAutoGranularity returns DAYS for points spread across few days in same week`() {
        // Wed Jan 4 2023 to Fri Jan 6 2023 -> Same week (usually)
        val baseDate = Calendar.getInstance().apply { set(2023, 0, 4) }.timeInMillis
        val points = listOf(
            ChartPoint(baseDate, 10f),
            ChartPoint(baseDate + 86400000L * 2, 20f) // 2 days apart
        )
        
        val result = maintenanceManager.findAutoGranularity(points, false, false)
        assertEquals(TimeGranularity.DAYS, result)
    }

    @Test
    fun `getOperationPrediction with timeout only returns date prediction`() = runBlocking {
        val lastLog = MaintenanceLog(id = 1, equipmentId = 1, operationTypeId = 1, date = 1000L)
        val opType = OperationType(id = 1, description = "OT", timeoutValue = 10, timeoutUnit = TimeGranularity.DAYS)
        
        val result = maintenanceManager.getOperationPrediction(1, opType, lastLog, null)
        
        val expected = 1000L + (10L * 24 * 60 * 60 * 1000L)
        assertEquals(expected, result)
    }

    @Test
    fun `getOperationPrediction with interval and trend returns usage prediction`() = runBlocking {
        // Trend 1.0 per day, interval 50 units -> 50 days
        val lastLog = MaintenanceLog(id = 1, equipmentId = 1, operationTypeId = 1, date = 0L, accumulatedValue = 100.0)
        val opType = OperationType(id = 1, description = "OT", intervalValue = 50.0)
        
        coEvery { maintenanceLogDao.getLastValueLogForEquipment(1) } returns lastLog
        
        val result = maintenanceManager.getOperationPrediction(1, opType, lastLog, 1.0)
        
        val expected = 50L * 24 * 60 * 60 * 1000L
        assertEquals(expected, result)
    }

    @Test
    fun `getOperationPrediction with both returns earliest date`() = runBlocking {
        // Timeout: 10 days (earlier)
        // Interval: 50 days (later)
        val lastLog = MaintenanceLog(id = 1, equipmentId = 1, operationTypeId = 1, date = 0L, accumulatedValue = 100.0)
        val opType = OperationType(id = 1, description = "OT", timeoutValue = 10, timeoutUnit = TimeGranularity.DAYS, intervalValue = 50.0)
        
        coEvery { maintenanceLogDao.getLastValueLogForEquipment(1) } returns lastLog
        
        val result = maintenanceManager.getOperationPrediction(1, opType, lastLog, 1.0)
        
        val expected = 10L * 24 * 60 * 60 * 1000L
        assertEquals(expected, result)
    }

    @Test
    fun `getOperationPrediction with negative or zero trend ignores usage`() = runBlocking {
        val lastLog = MaintenanceLog(id = 1, equipmentId = 1, operationTypeId = 1, date = 0L, accumulatedValue = 100.0)
        val opType = OperationType(id = 1, description = "OT", intervalValue = 50.0)
        
        assertNull(maintenanceManager.getOperationPrediction(1, opType, lastLog, 0.0))
        assertNull(maintenanceManager.getOperationPrediction(1, opType, lastLog, -1.0))
    }
}
