package com.moxmose.moxequiplog.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "equipments")
data class Equipment(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val description: String,
    val photoUri: String? = null,
    val iconIdentifier: String? = null,
    val displayOrder: Int = 0,
    @ColumnInfo(defaultValue = "false")
    val dismissed: Boolean = false,
    val color: String? = null
)
