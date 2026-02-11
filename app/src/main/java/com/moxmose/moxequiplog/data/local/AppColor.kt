package com.moxmose.moxequiplog.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_colors")
data class AppColor(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hexValue: String,
    val name: String,
    val isDefault: Boolean = false,
    val displayOrder: Int = 0,
    val hidden: Boolean = false
)
