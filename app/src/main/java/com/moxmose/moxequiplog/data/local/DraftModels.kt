package com.moxmose.moxequiplog.data.local

import kotlinx.serialization.Serializable

@Serializable
data class EquipmentDraft(
    val equipment: Equipment,
    val isDefault: Boolean
)

@Serializable
data class OperationTypeDraft(
    val operationType: OperationType,
    val isDefault: Boolean
)
