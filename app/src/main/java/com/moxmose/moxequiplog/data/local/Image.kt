package com.moxmose.moxequiplog.data.local

import androidx.room.Entity

@Entity(tableName = "images", primaryKeys = ["uri", "category"])
data class Image(
    val uri: String,
    val category: String, // E.g., "EQUIPMENT", "OPERATIONS"
    val imageType: String, // "ICON" or "IMAGE"
    val displayOrder: Int = 0,
    val hidden: Boolean = false
)
