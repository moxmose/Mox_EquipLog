package com.moxmose.moxequiplog.data

import com.moxmose.moxequiplog.data.local.*
import com.moxmose.moxequiplog.utils.UiConstants
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.first

class MaintenanceManager(
    private val maintenanceLogDao: MaintenanceLogDao,
    private val equipmentDao: EquipmentDao,
    private val operationTypeDao: OperationTypeDao
) {

    // --- PREDICTION & TREND LOGIC ---

    suspend fun calculateTrend(equipmentId: Int): Double? {
        val equipment = equipmentDao.getEquipmentByIdOneShot(equipmentId) ?: return null
        return calculateTrend(equipment)
    }

    suspend fun calculateTrend(equipment: Equipment): Double? {
        val windowMs = getWindowMs(equipment.usageWindow.toLong(), equipment.usageWindowUnit)
        val sinceDate = System.currentTimeMillis() - windowMs
        
        val logs = maintenanceLogDao.getLogsSince(equipment.id, sinceDate)
            .filter { it.value != null }
            .sortedBy { it.date }

        val manualAvg = getDailyManualAverage(equipment)
        if (logs.size < 2) return manualAvg

        var totalValueDiff = 0.0
        var totalTimeDiff = 0L
        
        for (i in 0 until logs.size - 1) {
            val current = logs[i]
            val next = logs[i+1]
            
            val diff = next.accumulatedValue - current.accumulatedValue
            if (diff >= 0) {
                totalValueDiff += diff
                totalTimeDiff += (next.date - current.date)
            }
        }

        if (totalTimeDiff <= 0) return manualAvg
        
        val msPerDay = 24 * 60 * 60 * 1000.0
        val calculatedAverage = (totalValueDiff / totalTimeDiff) * msPerDay
        
        return if (calculatedAverage > 0) calculatedAverage else manualAvg
    }

    fun getWindowMs(value: Long, unit: TimeGranularity): Long {
        return when (unit) {
            TimeGranularity.MINUTES_5 -> value * 5 * 60 * 1000L
            TimeGranularity.MINUTES_15 -> value * 15 * 60 * 1000L
            TimeGranularity.HOURS -> value * 60 * 60 * 1000L
            TimeGranularity.DAYS -> value * 24 * 60 * 60 * 1000L
            TimeGranularity.WEEKS -> value * 7 * 24 * 60 * 60 * 1000L
            TimeGranularity.MONTHS -> value * 30 * 24 * 60 * 60 * 1000L
            TimeGranularity.YEARS -> value * 365 * 24 * 60 * 60 * 1000L
        }
    }

    private fun getDailyManualAverage(equipment: Equipment): Double? {
        val value = equipment.manualAverageValue ?: return null
        return when (equipment.manualAverageUnit) {
            TimeGranularity.MINUTES_5 -> value * 12 * 24
            TimeGranularity.MINUTES_15 -> value * 4 * 24
            TimeGranularity.HOURS -> value * 24
            TimeGranularity.DAYS -> value
            TimeGranularity.WEEKS -> value / 7.0
            TimeGranularity.MONTHS -> value / 30.0
            TimeGranularity.YEARS -> value / 365.0
        }
    }

    suspend fun estimateDueDate(equipmentId: Int, targetValue: Double): Long? {
        val trend = calculateTrend(equipmentId) ?: return null
        if (trend <= 0) return null

        val lastLog = maintenanceLogDao.getLastLogBefore(equipmentId, Long.MAX_VALUE)
        val lastValue = lastLog?.value ?: 0.0
        val lastDate = lastLog?.date ?: System.currentTimeMillis()

        if (targetValue <= lastValue) return null

        val remainingValue = targetValue - lastValue
        val daysRemaining = remainingValue / trend
        
        val estimatedDate = lastDate + (daysRemaining * 24 * 60 * 60 * 1000).toLong()
        return if (estimatedDate > System.currentTimeMillis()) estimatedDate else System.currentTimeMillis() + (24 * 60 * 60 * 1000)
    }

    suspend fun estimateTargetValue(equipmentId: Int, dueDate: Long): Double? {
        val trend = calculateTrend(equipmentId) ?: return null

        val lastLog = maintenanceLogDao.getLastLogBefore(equipmentId, Long.MAX_VALUE)
        val lastValue = lastLog?.value ?: 0.0
        val lastDate = lastLog?.date ?: System.currentTimeMillis()

        val referenceDate = if (dueDate > lastDate) lastDate else System.currentTimeMillis()
        if (dueDate <= referenceDate) return lastValue

        val timeDiff = dueDate - referenceDate
        val msPerDay = 24 * 60 * 60 * 1000.0
        val days = timeDiff / msPerDay

        return lastValue + (days * trend)
    }

    fun getOperationPrediction(
        opType: OperationType,
        lastLog: MaintenanceLog,
        trend: Double?
    ): Long? {
        val datePrediction = opType.timeoutValue?.let { value ->
            opType.timeoutUnit?.let { unit ->
                val cal = Calendar.getInstance()
                cal.timeInMillis = lastLog.date
                when (unit) {
                    TimeGranularity.MINUTES_5 -> cal.add(Calendar.MINUTE, value * 5)
                    TimeGranularity.MINUTES_15 -> cal.add(Calendar.MINUTE, value * 15)
                    TimeGranularity.HOURS -> cal.add(Calendar.HOUR_OF_DAY, value)
                    TimeGranularity.DAYS -> cal.add(Calendar.DAY_OF_YEAR, value)
                    TimeGranularity.WEEKS -> cal.add(Calendar.WEEK_OF_YEAR, value)
                    TimeGranularity.MONTHS -> cal.add(Calendar.MONTH, value)
                    TimeGranularity.YEARS -> cal.add(Calendar.YEAR, value)
                }
                cal.timeInMillis
            }
        }

        val usagePrediction = if (opType.intervalValue != null && trend != null && trend > 0) {
            val lastAccumulated = lastLog.accumulatedValue
            val targetAccumulated = lastAccumulated + opType.intervalValue
            val daysToTarget = (targetAccumulated - lastAccumulated) / trend
            (lastLog.date + (daysToTarget * 24 * 60 * 60 * 1000).toLong())
        } else null

        return when {
            datePrediction != null && usagePrediction != null -> minOf(datePrediction, usagePrediction)
            datePrediction != null -> datePrediction
            usagePrediction != null -> usagePrediction
            else -> null
        }
    }

    // --- ACCUMULATED VALUES RECALCULATION ---

    suspend fun recalculateAccumulatedValues(equipmentId: Int) {
        val allLogs = maintenanceLogDao.getAllLogsForEquipment(equipmentId)
        if (allLogs.isEmpty()) return

        val updatedLogs = mutableListOf<MaintenanceLog>()
        var currentAccumulated = 0.0
        var lastValue: Double? = null

        allLogs.forEach { log ->
            val delta = when {
                log.value == null -> 0.0
                lastValue == null -> log.value
                log.value >= lastValue -> log.value - lastValue
                else -> log.value // Reset rilevato (valore sceso)
            }
            
            currentAccumulated += delta
            val updatedLog = log.copy(accumulatedValue = currentAccumulated)
            updatedLogs.add(updatedLog)
            
            lastValue = if (log.resetAfter) null else log.value
        }
        
        maintenanceLogDao.updateLogs(updatedLogs)
    }

    suspend fun recalculateAllAccumulatedValues() {
        val equipments = equipmentDao.getAllEquipments().first()
        equipments.forEach { equipment ->
            recalculateAccumulatedValues(equipment.id)
        }
    }

    // --- PHOTO USAGE CHECK ---

    suspend fun isPhotoUsed(uri: String): Boolean {
        if (uri.isBlank()) return true
        val inEquipments = equipmentDao.countEquipmentsUsingPhoto(uri) > 0
        val inOperations = operationTypeDao.countOperationTypesUsingPhoto(uri) > 0
        return inEquipments || inOperations
    }

    // --- COST ANALYSIS LOGIC ---

    suspend fun calculateCostPerUnit(equipmentId: Int, windowValue: Long, windowUnit: TimeGranularity): Double? {
        val windowMs = getWindowMs(windowValue, windowUnit)
        val sinceDate = System.currentTimeMillis() - windowMs
        
        val totalCost = maintenanceLogDao.getTotalCostForEquipmentSince(equipmentId, sinceDate) ?: 0.0
        
        val logs = maintenanceLogDao.getLogsSince(equipmentId, sinceDate)
            .filter { it.value != null }
            .sortedBy { it.date }

        if (logs.size < 2) return null

        val firstLog = logs.first()
        val lastLog = logs.last()
        val deltaValue = lastLog.accumulatedValue - firstLog.accumulatedValue

        return if (deltaValue > 0) totalCost / deltaValue else null
    }

    suspend fun getOperationCostStats(operationTypeId: Int, windowValue: Long, windowUnit: TimeGranularity): Pair<Double?, Double?> {
        val windowMs = getWindowMs(windowValue, windowUnit)
        val sinceDate = System.currentTimeMillis() - windowMs
        
        val lastCost = maintenanceLogDao.getLastLogWithCostForOperation(operationTypeId)?.cost
        val avgCost = maintenanceLogDao.getAverageCostForOperationSince(operationTypeId, sinceDate)
        
        return lastCost to avgCost
    }

    // --- GRANULARITY & PERIOD UTILS ---

    fun getPeriodFormat(granularity: TimeGranularity) = when (granularity) {
        TimeGranularity.DAYS -> SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        TimeGranularity.WEEKS -> SimpleDateFormat("'W'ww/yy", Locale.getDefault())
        TimeGranularity.MONTHS -> SimpleDateFormat("MM/yy", Locale.getDefault())
        TimeGranularity.YEARS -> SimpleDateFormat("yyyy", Locale.getDefault())
        TimeGranularity.HOURS -> SimpleDateFormat("dd/MM HH:00", Locale.getDefault())
        TimeGranularity.MINUTES_15, TimeGranularity.MINUTES_5 -> SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    }

    fun getCalendarTruncatedTo(timestamp: Long, granularity: TimeGranularity): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        when (granularity) {
            TimeGranularity.MINUTES_5 -> {
                val min = calendar.get(Calendar.MINUTE)
                calendar.set(Calendar.MINUTE, (min / 5) * 5)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            TimeGranularity.MINUTES_15 -> {
                val min = calendar.get(Calendar.MINUTE)
                calendar.set(Calendar.MINUTE, (min / 15) * 15)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            TimeGranularity.HOURS -> { calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0) }
            TimeGranularity.DAYS -> { calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0) }
            TimeGranularity.WEEKS -> { 
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0) 
            }
            TimeGranularity.MONTHS -> { calendar.set(Calendar.DAY_OF_MONTH, 1); calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0) }
            TimeGranularity.YEARS -> { calendar.set(Calendar.DAY_OF_YEAR, 1); calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0) }
        }
        return calendar.timeInMillis
    }

    // --- CHARTING & AGGREGATION UTILS ---

    fun aggregateData(points: List<ChartPoint>, granularity: TimeGranularity?, isDelta: Boolean = false, isCount: Boolean = false): List<ChartPoint> {
        if (points.isEmpty()) return emptyList()
        
        var result = performAggregation(points, granularity, isDelta, isCount)

        if (result.size > UiConstants.MAX_CHART_POINTS) {
            result = smoothData(result, UiConstants.MAX_CHART_POINTS)
        }

        return result
    }

    fun findBestGranularity(points: List<ChartPoint>, requested: TimeGranularity, isDelta: Boolean, isCount: Boolean): TimeGranularity {
        if (points.size < 2) return requested
        
        var current = requested
        while (current != TimeGranularity.HOURS) {
            val testAggregation = performAggregation(points, current, isDelta, isCount)
            if (testAggregation.size >= 2) return current
            
            current = when (current) {
                TimeGranularity.YEARS -> TimeGranularity.MONTHS
                TimeGranularity.MONTHS -> TimeGranularity.WEEKS
                TimeGranularity.WEEKS -> TimeGranularity.DAYS
                TimeGranularity.DAYS -> TimeGranularity.HOURS
                TimeGranularity.HOURS -> TimeGranularity.MINUTES_15
                else -> TimeGranularity.MINUTES_5
            }
        }
        return current
    }

    fun findAutoGranularity(points: List<ChartPoint>, isDelta: Boolean, isCount: Boolean): TimeGranularity? {
        if (points.isEmpty()) return null
        
        val granularities = listOf(TimeGranularity.YEARS, TimeGranularity.MONTHS, TimeGranularity.WEEKS, TimeGranularity.DAYS, TimeGranularity.HOURS, TimeGranularity.MINUTES_15, TimeGranularity.MINUTES_5)
        
        for (gran in granularities) {
            val aggregated = performAggregation(points, gran, isDelta, isCount)
            if (aggregated.size in 2..25) return gran
        }
        
        return if (points.size > 1) TimeGranularity.HOURS else null
    }

    private fun performAggregation(points: List<ChartPoint>, granularity: TimeGranularity?, isDelta: Boolean, isCount: Boolean): List<ChartPoint> {
        if (granularity == null) return points.sortedBy { it.date }
        
        val grouped = points.groupBy { getCalendarTruncatedTo(it.date, granularity) }

        return grouped.map { (timestamp, groupedPoints) -> 
            val value = when {
                isCount -> groupedPoints.size.toFloat()
                isDelta -> groupedPoints.sumOf { it.value.toDouble() }.toFloat()
                else -> groupedPoints.maxOf { it.value }
            }
            ChartPoint(timestamp, value) 
        }.sortedBy { it.date }
    }

    private fun smoothData(points: List<ChartPoint>, maxPoints: Int): List<ChartPoint> {
        if (points.size <= maxPoints) return points
        val chunkSize = (points.size.toDouble() / maxPoints).toInt().coerceAtLeast(1)
        return points.chunked(chunkSize).map { chunk ->
            val avgDate = chunk.map { it.date }.average().toLong()
            val avgValue = chunk.map { it.value }.average().toFloat()
            ChartPoint(avgDate, avgValue)
        }
    }
}
