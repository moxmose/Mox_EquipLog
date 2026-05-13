package com.moxmose.moxequiplog.data

import com.moxmose.moxequiplog.data.local.ChartPoint
import com.moxmose.moxequiplog.data.local.Equipment
import com.moxmose.moxequiplog.data.local.EquipmentDao
import com.moxmose.moxequiplog.data.local.MaintenanceLog
import com.moxmose.moxequiplog.data.local.MaintenanceLogDao
import com.moxmose.moxequiplog.data.local.OperationType
import com.moxmose.moxequiplog.data.local.OperationTypeDao
import com.moxmose.moxequiplog.data.local.TimeGranularity
import com.moxmose.moxequiplog.utils.AppConstants
import com.moxmose.moxequiplog.utils.UiConstants
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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
        
        // Escludiamo i log con lo stesso timestamp per evitare divisioni per zero
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
            val timeDiff = next.date - current.date
            if (diff >= 0 && timeDiff > 0) {
                totalValueDiff += diff
                totalTimeDiff += timeDiff
            }
        }

        if (totalTimeDiff <= 0) return manualAvg
        
        val calculatedAverage = (totalValueDiff.toDouble() / totalTimeDiff) * AppConstants.MS_PER_DAY
        
        return if (calculatedAverage > 0) calculatedAverage else manualAvg
    }

    fun getWindowMs(value: Long, unit: TimeGranularity): Long {
        return when (unit) {
            TimeGranularity.MINUTES_5 -> value * 5 * 60 * 1000L
            TimeGranularity.MINUTES_15 -> value * 15 * 60 * 1000L
            TimeGranularity.HOURS -> value * 60 * 60 * 1000L
            TimeGranularity.DAYS -> value * AppConstants.MS_PER_DAY
            TimeGranularity.WEEKS -> value * 7 * AppConstants.MS_PER_DAY
            TimeGranularity.MONTHS -> value * 30 * AppConstants.MS_PER_DAY
            TimeGranularity.YEARS -> value * 365 * AppConstants.MS_PER_DAY
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

        val lastLog = maintenanceLogDao.getLastValueLogForEquipment(equipmentId)
        val lastValue = lastLog?.value ?: 0.0
        val lastDate = lastLog?.date ?: System.currentTimeMillis()

        val remainingValue = targetValue - lastValue
        val daysRemaining = remainingValue / trend
        
        return lastDate + (daysRemaining * AppConstants.MS_PER_DAY).toLong()
    }

    suspend fun estimateTargetValue(equipmentId: Int, dueDate: Long): Double? {
        val trend = calculateTrend(equipmentId) ?: return null

        val lastLog = maintenanceLogDao.getLastValueLogForEquipment(equipmentId)
        val lastValue = lastLog?.value ?: 0.0
        val lastDate = lastLog?.date ?: System.currentTimeMillis()

        // Calcoliamo i giorni di differenza (positivi o negativi) rispetto all'ultima lettura
        val timeDiff = dueDate - lastDate
        val days = timeDiff.toDouble() / AppConstants.MS_PER_DAY

        return lastValue + (days * trend)
    }

    suspend fun getOperationPrediction(
        equipmentId: Int,
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
            val lastValueLog = maintenanceLogDao.getLastValueLogForEquipment(equipmentId)
            
            // Il target si calcola sempre rispetto a quando è stata fatta l'ultima manutenzione specifica
            // (lastLog è l'ultimo log di tipo opType.id per questo equipaggiamento)
            val targetAccumulated = lastLog.accumulatedValue + opType.intervalValue
            
            // Il consumo attuale (accumulato) dell'equipaggiamento al momento dell'ultimo log di valore (fallout o altro)
            val currentAccumulated = lastValueLog?.accumulatedValue ?: lastLog.accumulatedValue
            
            val remainingValue = targetAccumulated - currentAccumulated
            
            // Se siamo già oltre la soglia, la data stimata DEVE essere nel passato
            // daysRemaining sarà negativo, portando la referenceDate all'indietro
            val daysRemaining = remainingValue / trend
            
            // Data di riferimento: quando è stata rilevata l'ultima lettura km (es. 130km il 04/05)
            val referenceDate = lastValueLog?.date ?: lastLog.date
            
            referenceDate + (daysRemaining * AppConstants.MS_PER_DAY).toLong()
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
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = timestamp
        
        // Resetting all fields below the granularity to ensure identical timestamps for grouping
        cal.set(Calendar.MILLISECOND, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        
        when (granularity) {
            TimeGranularity.YEARS -> {
                cal.set(Calendar.MONTH, Calendar.JANUARY)
                cal.set(Calendar.DAY_OF_MONTH, 1)
            }
            TimeGranularity.MONTHS -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
            }
            TimeGranularity.WEEKS -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            }
            TimeGranularity.DAYS -> {
                // Already truncated by resetting hours/mins/secs
            }
            TimeGranularity.HOURS -> {
                // Restore hours, minutes are already 0
                val calOriginal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calOriginal.timeInMillis = timestamp
                cal.set(Calendar.HOUR_OF_DAY, calOriginal.get(Calendar.HOUR_OF_DAY))
            }
            TimeGranularity.MINUTES_15 -> {
                val calOriginal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calOriginal.timeInMillis = timestamp
                val min = calOriginal.get(Calendar.MINUTE)
                cal.set(Calendar.HOUR_OF_DAY, calOriginal.get(Calendar.HOUR_OF_DAY))
                cal.set(Calendar.MINUTE, (min / 15) * 15)
            }
            TimeGranularity.MINUTES_5 -> {
                val calOriginal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calOriginal.timeInMillis = timestamp
                val min = calOriginal.get(Calendar.MINUTE)
                cal.set(Calendar.HOUR_OF_DAY, calOriginal.get(Calendar.HOUR_OF_DAY))
                cal.set(Calendar.MINUTE, (min / 5) * 5)
            }
        }
        return cal.timeInMillis
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
        // Se l'utente ha richiesto una granularità specifica, la onoriamo sempre.
        // La logica di fallback deve intervenire solo se non c'è una richiesta esplicita (auto-mode).
        return requested
    }

    fun findAutoGranularity(points: List<ChartPoint>, isDelta: Boolean, isCount: Boolean): TimeGranularity? {
        if (points.isEmpty()) return null
        
        val granularities = listOf(
            TimeGranularity.YEARS,
            TimeGranularity.MONTHS,
            TimeGranularity.WEEKS,
            TimeGranularity.DAYS,
            TimeGranularity.HOURS,
            TimeGranularity.MINUTES_15,
            TimeGranularity.MINUTES_5
        )
        
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
                else -> {
                    // Per la granularità MONTHS e YEARS, prendiamo l'ultimo valore disponibile del periodo
                    // per evitare oscillazioni se ci sono più log nel periodo (es. due rifornimenti nello stesso mese)
                    if (granularity == TimeGranularity.MONTHS || granularity == TimeGranularity.YEARS) {
                        groupedPoints.maxBy { it.date }.value
                    } else {
                        groupedPoints.maxOf { it.value }
                    }
                }
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
