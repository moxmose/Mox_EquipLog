package com.moxmose.moxequiplog.ui.options

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object EquipmentIconProvider {

    val equipmentIcons = mapOf(
        "build" to Icons.Filled.Build,
        "car" to Icons.Filled.DirectionsCar,
        "moto" to Icons.Filled.TwoWheeler,
        "scooter" to Icons.Filled.ElectricScooter,
        "bike" to Icons.Filled.PedalBike,
        "pc" to Icons.Filled.Computer,
        "drone" to Icons.Filled.Flight,
        "boat" to Icons.Filled.DirectionsBoat,
        "other" to Icons.Filled.QuestionMark
    )

    val operationIcons = mapOf(
        "maintenance" to Icons.Filled.Build, // General maintenance
        "oil" to Icons.Filled.WaterDrop, // Oil change
        "tire" to Icons.Filled.Settings, // Tire change/check
        "battery" to Icons.Filled.BatteryChargingFull, // Battery check/change
        "cleaning" to Icons.Filled.CleanHands, // Cleaning
        "check" to Icons.Filled.Checklist, // General check
        "upgrade" to Icons.Filled.Upgrade, // Upgrade
        "repair" to Icons.Filled.Construction, // Repair
        "other" to Icons.Filled.QuestionMark
    )

    fun getIcon(identifier: String?, category: String = "EQUIPMENT"): ImageVector {
        if (identifier == null || identifier == "none") {
            return Icons.Default.NotInterested
        }
        val iconSet = if (category == "OPERATION") operationIcons else equipmentIcons
        return iconSet[identifier] ?: Icons.AutoMirrored.Filled.List // Fallback
    }

    fun getIconsForCategory(category: String): Map<String, ImageVector> {
        return if (category == "OPERATION") operationIcons else equipmentIcons
    }
}
