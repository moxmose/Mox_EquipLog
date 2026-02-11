package com.moxmose.moxequiplog.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey


@Entity(
    tableName = "maintenance_logs",
    foreignKeys = [
        ForeignKey(
            entity = Equipment::class,
            parentColumns = ["id"],
            childColumns = ["equipmentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = OperationType::class,
            parentColumns = ["id"],
            childColumns = ["operationTypeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["equipmentId"]),
        androidx.room.Index(value = ["operationTypeId"])
    ]
)
data class MaintenanceLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "equipmentId")
    val equipmentId: Int,
    @ColumnInfo(name = "operationTypeId")
    val operationTypeId: Int,
    val notes: String? = null,
    val kilometers: Int? = null,
    val date: Long,
    @ColumnInfo(defaultValue = "false")
    val dismissed: Boolean = false,
    val color: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
