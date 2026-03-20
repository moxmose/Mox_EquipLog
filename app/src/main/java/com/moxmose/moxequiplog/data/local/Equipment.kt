package com.moxmose.moxequiplog.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

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
    @ColumnInfo(defaultValue = "1") // Assumiamo 1 come ID per "km"
    val unitId: Int = 1
)
