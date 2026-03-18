package com.moxmose.moxequiplog.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    val id: String, // "EQUIPMENT", "OPERATION", etc.
    val name: String
) {
    companion object {
        const val LOGS = "LOG"
        const val EQUIPMENT = "EQUIPMENT"
        const val OPERATION = "OPERATION"
    }
}
