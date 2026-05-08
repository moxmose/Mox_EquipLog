package com.moxmose.moxequiplog.data.local

import kotlinx.serialization.Serializable

@Serializable
enum class TimeGranularity(val key: String) {
    MINUTES_5("MINUTES_5"),
    MINUTES_15("MINUTES_15"),
    HOURS("HOURS"),
    DAYS("DAYS"),
    WEEKS("WEEKS"),
    MONTHS("MONTHS"),
    YEARS("YEARS")
}
