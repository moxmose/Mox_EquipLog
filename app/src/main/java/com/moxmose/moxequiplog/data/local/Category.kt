package com.moxmose.moxequiplog.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    val id: String, // "EQUIPMENT", "OPERATION", etc.
    val name: String,
    val color: String, // Hex color
    val defaultIconIdentifier: String? = null,
    val defaultPhotoUri: String? = null
)
