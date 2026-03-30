package com.moxmose.moxequiplog.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "report_filters")
data class ReportFilter(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String?,
    val reportType: String,
    val isLastSession: Boolean = false,
    val filterJson: String,
    val timestamp: Long = System.currentTimeMillis()
)
