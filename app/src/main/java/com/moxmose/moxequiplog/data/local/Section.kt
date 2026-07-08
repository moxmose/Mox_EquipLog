package com.moxmose.moxequiplog.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sections")
data class Section(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val iconIdentifier: String? = null,
    val photoUri: String? = null,
    val color: String? = null,
    val displayOrder: Int = 0,
    val dismissed: Boolean = false,
    
    @ColumnInfo(defaultValue = "1")
    val defaultUnitId: Int = 1,
    @ColumnInfo(defaultValue = "30")
    val defaultUsageWindow: Int = 30,
    @ColumnInfo(defaultValue = "DAYS")
    val defaultUsageWindowUnit: TimeGranularity = TimeGranularity.DAYS,
    @ColumnInfo(defaultValue = "30")
    val defaultVisibilityHorizon: Int = 30,
    @ColumnInfo(defaultValue = "DAYS")
    val defaultVisibilityHorizonUnit: TimeGranularity = TimeGranularity.DAYS,

    val defaultEquipmentId: Int? = null,
    val defaultOperationTypeId: Int? = null
)
