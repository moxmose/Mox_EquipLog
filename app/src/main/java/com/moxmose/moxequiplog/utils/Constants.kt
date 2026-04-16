package com.moxmose.moxequiplog.utils

import com.moxmose.moxequiplog.data.local.MeasurementUnit

object AppConstants {
    const val FLOW_STOP_TIMEOUT = 5000L
    const val UNIT_LABEL_MAX_LENGTH = 3
    const val UNIT_DESCRIPTION_MAX_LENGTH = 15
    const val COLOR_NAME_MAX_LENGTH = 15
    const val USERNAME_MAX_LENGTH = 20
    const val MAX_DECIMAL_PLACES = 3

    val INITIAL_MEASUREMENT_UNITS = listOf(
        MeasurementUnit(id = 1, label = "km", description = "Measure Unit", isSystem = true, isHidden = false, decimalPlaces = 0),
        MeasurementUnit(id = 2, label = "hh", description = "Hours", isSystem = true, isHidden = false, decimalPlaces = 1),
        MeasurementUnit(id = 3, label = "dy", description = "Days", isSystem = true, isHidden = false, decimalPlaces = 0),
        MeasurementUnit(id = 4, label = "un", description = "Units", isSystem = true, isHidden = false, decimalPlaces = 0),
    )

    const val SYSTEM_OPERATION_RESET_ID = 1
}

