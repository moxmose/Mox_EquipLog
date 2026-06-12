package com.moxmose.moxequiplog.ui.maintenancelog.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.Equipment
import com.moxmose.moxequiplog.ui.components.ImageIcon
import com.moxmose.moxequiplog.ui.equipment.OperationStatus
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PredictionsDashboard(
    predictions: List<Pair<Equipment, OperationStatus>>,
    equipmentCategoryColor: String?,
    operationCategoryColor: String?,
    onPredictionClick: (Equipment, OperationStatus) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    
    if (predictions.isEmpty()) return

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
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
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
                    Icons.Default.AutoGraph,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Forecasted Maintenance",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ) {
                    Text(
                        text = predictions.size.toString(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            if (expanded) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    predictions.forEach { (equipment, status) ->
                        PredictionItem(
                            equipment = equipment,
                            status = status,
                            eColor = eColor,
                            oColor = oColor,
                            onClick = { onPredictionClick(equipment, status) }
                        )
                    }
                    Spacer(modifier = Modifier.padding(bottom = 4.dp))
                }
            }
        }
    }
}

@Composable
fun PredictionItem(
    equipment: Equipment,
    status: OperationStatus,
    eColor: Color,
    oColor: Color,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val isOverdue = status.isOverdue

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
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
                        photoUri = equipment.photoUri,
                        iconIdentifier = equipment.iconIdentifier,
                        modifier = Modifier.size(20.dp),
                        category = Category.EQUIPMENT,
                        borderColor = eColor,
                        contentPadding = 1.dp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = equipment.description.takeIf { it.isNotBlank() } ?: stringResource(R.string.id_no_description, equipment.id),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ImageIcon(
                        photoUri = status.operation.photoUri,
                        iconIdentifier = status.operation.iconIdentifier,
                        modifier = Modifier.size(20.dp),
                        category = Category.OPERATION,
                        borderColor = oColor,
                        contentPadding = 1.dp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = status.operation.description.takeIf { it.isNotBlank() } ?: stringResource(R.string.id_no_description, status.operation.id),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isOverdue) Icons.Default.PriorityHigh else Icons.Default.AutoGraph,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = (if (isOverdue) stringResource(R.string.reminder_overdue) + " - " else "") + 
                               "Estimated: " + (status.nextPresumedDate?.let { dateFormat.format(Date(it)) } ?: "N/A"),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOverdue) MaterialTheme.colorScheme.error else Color.Unspecified
                    )
                }
            }
            
            IconButton(onClick = onClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Log", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
