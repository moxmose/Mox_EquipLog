package com.moxmose.moxequiplog.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "operation_types")
data class OperationType(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val description: String,
    @ColumnInfo(defaultValue = "false")
    val dismissed: Boolean = false,
    val color: String? = null,
    val iconIdentifier: String? = null,
    val photoUri: String? = null,
    val displayOrder: Int = 0
)
