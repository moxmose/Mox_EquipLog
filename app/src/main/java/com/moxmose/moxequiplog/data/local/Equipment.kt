package com.moxmose.moxequiplog.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "equipments",
    foreignKeys = [
        ForeignKey(
            entity = MeasurementUnit::class,
            parentColumns = ["id"],
            childColumns = ["unitId"],
            onDelete = ForeignKey.SET_DEFAULT
        )
    ],
    indices = [androidx.room.Index(value = ["unitId"])]
)
data class Equipment(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val description: String,
    val photoUri: String? = null,
    val iconIdentifier: String? = null,
    val displayOrder: Int = 0,
    @ColumnInfo(defaultValue = "false")
    val dismissed: Boolean = false,
    val color: String? = null,
    @ColumnInfo(defaultValue = "1")
    val unitId: Int = 1,
    @ColumnInfo(defaultValue = "false")
    val isResettable: Boolean = false,
    @ColumnInfo(defaultValue = "30")
    val usageWindow: Int = 30,
    @ColumnInfo(defaultValue = "DAYS")
    val usageWindowUnit: TimeGranularity = TimeGranularity.DAYS,
    val manualAverageValue: Double? = null,
    @ColumnInfo(defaultValue = "DAYS")
    val manualAverageUnit: TimeGranularity = TimeGranularity.DAYS,
    @ColumnInfo(defaultValue = "30")
    val visibilityHorizon: Int = 30,
    @ColumnInfo(defaultValue = "DAYS")
    val visibilityHorizonUnit: TimeGranularity = TimeGranularity.DAYS,
    @ColumnInfo(defaultValue = "false")
    val useCustomUsageWindow: Boolean = false,
    @ColumnInfo(defaultValue = "false")
    val useCustomVisibilityHorizon: Boolean = false,
    val estimatedCostPerUnit: Double? = null
)
