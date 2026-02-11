package com.moxmose.moxequiplog.data.local

import androidx.room.Embedded

data class MaintenanceLogDetails(
    @Embedded val log: MaintenanceLog,
    val equipmentDescription: String,
    val operationTypeDescription: String,
    val equipmentPhotoUri: String?,
    val equipmentIconIdentifier: String?,
    val operationTypePhotoUri: String?,
    val operationTypeIconIdentifier: String?,
    val equipmentDismissed: Boolean,
    val operationTypeDismissed: Boolean
)
