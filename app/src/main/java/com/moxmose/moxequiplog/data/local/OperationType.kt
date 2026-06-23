package com.moxmose.moxequiplog.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "operation_types",
    foreignKeys = [
        ForeignKey(
            entity = Section::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MeasurementUnit::class,
            parentColumns = ["id"],
            childColumns = ["unitId"],
            onDelete = ForeignKey.SET_DEFAULT
        )
    ],
    indices = [
        androidx.room.Index(value = ["sectionId"]),
        androidx.room.Index(value = ["unitId"])
    ]
)
@Serializable
data class OperationType(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val description: String,
    @ColumnInfo(defaultValue = "0")
    val dismissed: Boolean = false,
    val color: String? = null,
    val iconIdentifier: String? = null,
    val photoUri: String? = null,
    val displayOrder: Int = 0,
    @ColumnInfo(defaultValue = "1")
    val sectionId: Int = 1,
    @ColumnInfo(defaultValue = "1")
    val unitId: Int = 1,
    @ColumnInfo(defaultValue = "0")
    val isResettable: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val isSystem: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val isPredictable: Boolean = false,
    val intervalValue: Double? = null,
    val timeoutValue: Int? = null,
    val timeoutUnit: TimeGranularity? = null,
    @ColumnInfo(defaultValue = "30")
    val visibilityHorizon: Int = 30,
    @ColumnInfo(defaultValue = "DAYS")
    val visibilityHorizonUnit: TimeGranularity = TimeGranularity.DAYS,
    @ColumnInfo(defaultValue = "0")
    val useCustomVisibilityHorizon: Boolean = false,
    val estimatedCost: Double? = null
)
