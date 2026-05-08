package com.moxmose.moxequiplog.utils

import com.moxmose.moxequiplog.data.local.TimeGranularity

object UiConstants {
    const val DEFAULT_FALLBACK_COLOR = "#808080"

    // Background defaults
    const val DEFAULT_BACKGROUND_BLUR = 0f
    const val DEFAULT_BACKGROUND_SATURATION = 1f
    const val DEFAULT_BACKGROUND_TINT_ENABLED = false
    const val DEFAULT_BACKGROUND_TINT_ALPHA = 0.25f
    const val DEFAULT_BACKGROUND_IMAGE_ALPHA = 1f

    // Reports defaults
    const val REPORTS_COLOR_MODE_M3 = "M3"
    const val REPORTS_COLOR_MODE_CUSTOM = "CUSTOM"
    const val DEFAULT_REPORTS_COLOR_MODE = REPORTS_COLOR_MODE_M3

    // Analisi e Grafici
    const val MAX_CHART_POINTS = 50
    const val DEFAULT_USAGE_WINDOW_VALUE = 30
    val DEFAULT_USAGE_WINDOW_UNIT = TimeGranularity.DAYS.name
    const val DEFAULT_VISIBILITY_HORIZON_VALUE = 30
    val DEFAULT_VISIBILITY_HORIZON_UNIT = TimeGranularity.DAYS.name
    const val DEFAULT_COST_ANALYSIS_WINDOW_VALUE = 24
    val DEFAULT_COST_ANALYSIS_WINDOW_UNIT = TimeGranularity.MONTHS.name

    // Cost trend defaults
    const val DEFAULT_COST_TREND_THRESHOLD = 0.05f
}
