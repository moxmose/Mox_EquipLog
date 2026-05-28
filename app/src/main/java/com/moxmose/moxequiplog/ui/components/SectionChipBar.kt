package com.moxmose.moxequiplog.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.Section
import com.moxmose.moxequiplog.ui.options.EquipmentIconProvider
import com.moxmose.moxequiplog.utils.AppConstants
import androidx.core.graphics.toColorInt

@Composable
fun SectionChipBar(
    sections: List<Section>,
    selectedSectionId: Int,
    onSectionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showAllOption: Boolean = true
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showAllOption) {
            item {
                SectionChip(
                    name = stringResource(R.string.section_all),
                    isSelected = selectedSectionId == AppConstants.ALL_SECTIONS_ID,
                    onClick = { onSectionSelected(AppConstants.ALL_SECTIONS_ID) },
                    iconIdentifier = "all",
                    colorHex = null
                )
            }
        }

        items(sections, key = { it.id }) { section ->
            SectionChip(
                name = section.name,
                isSelected = selectedSectionId == section.id,
                onClick = { onSectionSelected(section.id) },
                iconIdentifier = section.iconIdentifier,
                colorHex = section.color
            )
        }
    }
}

@Composable
private fun SectionChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    iconIdentifier: String?,
    colorHex: String?
) {
    val chipColor = if (colorHex != null) {
        try { Color(colorHex.toColorInt()) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
    } else {
        MaterialTheme.colorScheme.primary
    }

    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(name) },
        leadingIcon = {
            val icon = if (iconIdentifier == "all") {
                Icons.Default.AllInclusive
            } else {
                EquipmentIconProvider.getIcon(iconIdentifier)
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = chipColor.copy(alpha = 0.2f),
            selectedLabelColor = chipColor,
            selectedLeadingIconColor = chipColor,
            selectedTrailingIconColor = chipColor
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            selectedBorderColor = chipColor,
            selectedBorderWidth = 2.dp
        )
    )
}
