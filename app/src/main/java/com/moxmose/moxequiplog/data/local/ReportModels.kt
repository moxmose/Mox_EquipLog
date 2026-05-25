package com.moxmose.moxequiplog.data.local

import kotlinx.serialization.Serializable

@Serializable
data class ChartPoint(
    val date: Long,
    val value: Float,
    val label: String? = null
)

data class PieChartPoint(
    val label: String,
    val value: Float,
    val color: String? = null,
    val id: Int = -1
)

data class HeatmapPoint(
    val x: Int, // e.g. Day of week
    val y: Int, // e.g. Hour or Month
    val value: Int
)

data class BenchmarkData(
    val equipmentName: String,
    val totalValue: Float,
    val avgInterval: Float,
    val count: Int,
    val periodLabel: String? = null
)

data class PredictionDetails(
    val equipmentId: Int,
    val equipmentDescription: String,
    val operationTypeId: Int,
    val operationTypeDescription: String,
    val predictedDate: Long,
    val equipmentPhotoUri: String? = null,
    val equipmentIconIdentifier: String? = null,
    val operationTypePhotoUri: String? = null,
    val operationTypeIconIdentifier: String? = null
)
