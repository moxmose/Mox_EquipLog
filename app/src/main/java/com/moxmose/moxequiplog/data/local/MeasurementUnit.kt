package com.moxmose.moxequiplog.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurement_units")
data class MeasurementUnit(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val label: String, // es. "km", "hh", "lt"
    val description: String, // es. "Kilometers", "Hours", "Liters"
    val isSystem: Boolean = false, // Se true, non può essere eliminata
    val isHidden: Boolean = false, // Se true, non viene visualizzata nella UI
    val displayOrder: Int = 0,
    val decimalPlaces: Int = 0 // 0 = intero, 1-3 = decimali
)
