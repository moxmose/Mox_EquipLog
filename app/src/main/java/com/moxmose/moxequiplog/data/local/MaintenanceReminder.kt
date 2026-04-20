package com.moxmose.moxequiplog.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "maintenance_reminders",
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
        Index(value = ["equipmentId"]),
        Index(value = ["operationTypeId"])
    ]
)
data class MaintenanceReminder(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val equipmentId: Int,
    val operationTypeId: Int,
    val dueDate: Long? = null,
    val dueValue: Double? = null,
    val calendarEventId: String? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
