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
    val operationTypeDismissed: Boolean,
    val operationTypeIsResettable: Boolean = false,
    val operationTypeIsSystem: Boolean = false,
    val operationTypeHasValue: Boolean = true,
    val equipmentUnitId: Int = 1,
    val operationTypeUnitId: Int = 1,
    val previousLogValue: Double? = null,
    val previousLogIsSystem: Boolean = false,
    val equipmentSectionId: Int? = null,
    val equipmentSectionName: String? = null,
    val equipmentSectionColor: String? = null,
    val operationSectionId: Int? = null,
    val operationSectionName: String? = null,
    val operationSectionColor: String? = null
)
