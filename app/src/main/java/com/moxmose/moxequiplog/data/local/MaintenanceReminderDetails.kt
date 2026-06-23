package com.moxmose.moxequiplog.data.local

import androidx.room.Embedded

data class MaintenanceReminderDetails(
    @Embedded val reminder: MaintenanceReminder,
    val equipmentDescription: String,
    val operationTypeDescription: String,
    val equipmentPhotoUri: String?,
    val equipmentIconIdentifier: String?,
    val operationTypePhotoUri: String?,
    val operationTypeIconIdentifier: String?,
    val equipmentDismissed: Boolean,
    val operationTypeDismissed: Boolean,
    val unitId: Int?,
    val operationTypeEstimatedCost: Double?,
    val lastLogCost: Double?,
    val averageCost: Double?,
    val equipmentSectionId: Int? = null,
    val equipmentSectionName: String? = null,
    val equipmentSectionColor: String? = null,
    val operationSectionId: Int? = null,
    val operationSectionName: String? = null,
    val operationSectionColor: String? = null
)
