package com.moxmose.moxequiplog.ui.maintenancelog.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.MaintenanceReminderDetails
import com.moxmose.moxequiplog.data.local.MeasurementUnit
import com.moxmose.moxequiplog.ui.components.ImageIcon
import com.moxmose.moxequiplog.ui.components.SectionBadge
import com.moxmose.moxequiplog.utils.AppConstants
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RemindersDashboard(
    reminders: List<MaintenanceReminderDetails>,
    measurementUnits: List<MeasurementUnit>,
    equipmentCategoryColor: String?,
    operationCategoryColor: String?,
    onComplete: (MaintenanceReminderDetails) -> Unit,
    onEdit: (MaintenanceReminderDetails) -> Unit,
    onRefresh: () -> Unit,
    costTrendThreshold: Float
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    
    if (reminders.isEmpty()) return

    val eColor = remember(equipmentCategoryColor) {
        try { equipmentCategoryColor?.toColorInt()?.let { Color(it) } ?: Color.Gray } catch (_: Exception) { Color.Gray }
    }
    val oColor = remember(operationCategoryColor) {
        try { operationCategoryColor?.toColorInt()?.let { Color(it) } ?: Color.Gray } catch (_: Exception) { Color.Gray }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .clickable { expanded = !expanded }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.reminders_dashboard_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRefresh) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh all predictions",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        text = reminders.size.toString(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }

            if (expanded) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    reminders.forEach { reminderDetails ->
                        ReminderItem(
                            details = reminderDetails,
                            measurementUnits = measurementUnits,
                            eColor = eColor,
                            oColor = oColor,
                            onComplete = { onComplete(reminderDetails) },
                            onEdit = { onEdit(reminderDetails) },
                            costTrendThreshold = costTrendThreshold
                        )
                    }
                    Spacer(modifier = Modifier.padding(bottom = 4.dp))
                }
            }
        }
    }
}

@Composable
fun ReminderItem(
    details: MaintenanceReminderDetails,
    measurementUnits: List<MeasurementUnit>,
    eColor: Color,
    oColor: Color,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    costTrendThreshold: Float
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val unit = measurementUnits.find { it.id == details.unitId }
    val unitLabel = unit?.label ?: "Km"
    val decimalPlaces = unit?.decimalPlaces ?: 0

    val fixedDate = details.reminder.dueDate
    val presumedDate = details.reminder.presumedDate
    val effectiveDate = fixedDate ?: presumedDate

    val isOverdue = remember(effectiveDate) {
        effectiveDate != null && effectiveDate < System.currentTimeMillis()
    }
    
    val hasWarning = fixedDate != null && presumedDate != null && presumedDate < fixedDate

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ImageIcon(
                        photoUri = details.equipmentPhotoUri,
                        iconIdentifier = details.equipmentIconIdentifier,
                        modifier = Modifier.size(20.dp),
                        category = Category.EQUIPMENT,
                        borderColor = eColor,
                        contentPadding = 1.dp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = details.equipmentDescription.takeIf { it.isNotBlank() } ?: stringResource(R.string.id_no_description, details.reminder.equipmentId),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (details.equipmentSectionId != null && details.equipmentSectionId != AppConstants.DEFAULT_SECTION_ID) {
                        Spacer(modifier = Modifier.width(6.dp))
                        SectionBadge(
                            name = details.equipmentSectionName ?: "",
                            colorHex = details.equipmentSectionColor
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ImageIcon(
                        photoUri = details.operationTypePhotoUri,
                        iconIdentifier = details.operationTypeIconIdentifier,
                        modifier = Modifier.size(20.dp),
                        category = Category.OPERATION,
                        borderColor = oColor,
                        contentPadding = 1.dp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = details.operationTypeDescription.takeIf { it.isNotBlank() } ?: stringResource(R.string.id_no_description, details.reminder.operationTypeId),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (details.operationSectionId != null && details.operationSectionId != AppConstants.DEFAULT_SECTION_ID) {
                        Spacer(modifier = Modifier.width(6.dp))
                        SectionBadge(
                            name = details.operationSectionName ?: "",
                            colorHex = details.operationSectionColor
                        )
                    }
                }
                
                if (fixedDate != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isOverdue) Icons.Default.PriorityHigh else Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = (if (isOverdue) stringResource(R.string.reminder_overdue) + " - " else "") + 
                                   stringResource(R.string.due_date_label, dateFormat.format(Date(fixedDate))),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOverdue) MaterialTheme.colorScheme.error else Color.Unspecified
                        )
                    }
                }
                
                if (presumedDate != null && (fixedDate == null || hasWarning)) {
                    val presumedIsOverdue = presumedDate < System.currentTimeMillis()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when {
                                presumedIsOverdue -> Icons.Default.PriorityHigh
                                hasWarning -> Icons.Default.Warning
                                else -> Icons.Default.AccessTime
                            },
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = when {
                                presumedIsOverdue -> MaterialTheme.colorScheme.error
                                hasWarning -> Color(0xFFFF9800)
                                else -> MaterialTheme.colorScheme.secondary
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = (if (presumedIsOverdue) stringResource(R.string.reminder_overdue) + " - " else "") +
                                   (if (fixedDate == null) 
                                       stringResource(R.string.estimated_date_prefix, dateFormat.format(Date(presumedDate))) 
                                       else stringResource(R.string.likely_needed_by_prefix, dateFormat.format(Date(presumedDate)))),
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                presumedIsOverdue -> MaterialTheme.colorScheme.error
                                hasWarning -> Color(0xFFFF9800)
                                else -> MaterialTheme.colorScheme.secondary
                            }
                        )
                    }
                }
                
                if (details.reminder.dueValue != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.due_value_label, String.format(Locale.US, "%.${decimalPlaces}f", details.reminder.dueValue), unitLabel),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                val estimatedCost = details.lastLogCost ?: details.operationTypeEstimatedCost
                if (estimatedCost != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Payments,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.estimated_cost_label, String.format(Locale.US, "%.2f €", estimatedCost)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        details.averageCost?.let { avg ->
                            Spacer(Modifier.width(4.dp))
                            val (icon, color) = when {
                                estimatedCost > avg * (1 + costTrendThreshold) -> Icons.AutoMirrored.Filled.TrendingUp to Color.Red
                                estimatedCost < avg * (1 - costTrendThreshold) -> Icons.AutoMirrored.Filled.TrendingDown to Color.Green
                                else -> Icons.AutoMirrored.Filled.TrendingFlat to Color.Gray
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
            
            Row {
                IconButton(onClick = onComplete) {
                    Icon(Icons.Default.Done, contentDescription = stringResource(R.string.reminder_complete_log), tint = Color(0xFF4CAF50))
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_log), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
