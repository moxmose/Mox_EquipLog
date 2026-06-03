package com.moxmose.moxequiplog.data.local

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
    val dismissed: Boolean = false
)
