package com.moxmose.moxequiplog.data.local

import androidx.room.Entity

@Entity(tableName = "media", primaryKeys = ["uri", "category"])
data class Media(
    val uri: String,
    val category: String, // E.g., "EQUIPMENT", "OPERATIONS"
    val mediaType: String, // "ICON" or "IMAGE"
    val displayOrder: Int = 0,
    val hidden: Boolean = false
)
