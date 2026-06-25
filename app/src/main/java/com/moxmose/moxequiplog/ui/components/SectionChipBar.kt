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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.data.local.Category
import com.moxmose.moxequiplog.data.local.Section
import com.moxmose.moxequiplog.utils.AppConstants
import com.moxmose.moxequiplog.utils.UiConstants
import androidx.core.graphics.toColorInt

@Composable
fun SectionChipBar(
    sections: List<Section>,
    selectedSectionId: Int,
    onSectionSelected: (Int) -> Unit,
    showDismissed: Boolean,
    onToggleShowDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    showAllOption: Boolean = true
) {
    val filteredSections = remember(sections, showDismissed) {
        if (showDismissed) sections else sections.filter { !it.dismissed }
    }

    if (filteredSections.size <= 1 && !showDismissed) return

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onToggleShowDismissed,
            modifier = Modifier.padding(start = 12.dp).size(32.dp)
        ) {
            Icon(
                imageVector = if (showDismissed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        }

        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showAllOption) {
                item {
                    SectionChip(
                        name = stringResource(R.string.section_all),
                        isSelected = selectedSectionId == AppConstants.ALL_SECTIONS_ID,
                        onClick = { onSectionSelected(AppConstants.ALL_SECTIONS_ID) },
                        iconIdentifier = "all",
                        photoUri = null,
                        colorHex = null,
                        isDismissed = false
                    )
                }
            }

            items(filteredSections, key = { it.id }) { section ->
                val name = if (section.id == AppConstants.DEFAULT_SECTION_ID) {
                    stringResource(R.string.section_common)
                } else {
                    section.name
                }
                SectionChip(
                    name = name,
                    isSelected = selectedSectionId == section.id,
                    onClick = { onSectionSelected(section.id) },
                    iconIdentifier = section.iconIdentifier,
                    photoUri = section.photoUri,
                    colorHex = section.color,
                    isDismissed = section.dismissed
                )
            }
        }
    }
}

@Composable
private fun SectionChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    iconIdentifier: String?,
    photoUri: String?,
    colorHex: String?,
    isDismissed: Boolean
) {
    val chipColor = if (colorHex != null) {
        try { Color(colorHex.toColorInt()) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
    } else {
        MaterialTheme.colorScheme.primary
    }

    val alpha = if (isDismissed) 0.5f else 1.0f

    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(name) },
        modifier = Modifier.graphicsLayer(alpha = alpha),
        leadingIcon = {
            if (iconIdentifier == "all") {
                Icon(
                    imageVector = Icons.Default.AllInclusive,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                ImageIcon(
                    photoUri = photoUri,
                    iconIdentifier = iconIdentifier,
                    modifier = Modifier.size(18.dp),
                    category = Category.SECTIONS,
                    borderColor = null,
                    tint = if (isSelected) chipColor else chipColor.copy(alpha = 0.8f)
                )
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            labelColor = chipColor.copy(alpha = 0.8f),
            iconColor = chipColor.copy(alpha = 0.8f),
            selectedContainerColor = chipColor.copy(alpha = 0.2f),
            selectedLabelColor = chipColor,
            selectedLeadingIconColor = chipColor,
            selectedTrailingIconColor = chipColor
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = chipColor.copy(alpha = 0.4f),
            selectedBorderColor = chipColor,
            selectedBorderWidth = 2.dp
        )
    )
}

@Composable
fun UnifiedSectionSelector(
    allSections: List<Section>,
    selectedSectionId: Int,
    onSectionSelected: (Int) -> Unit,
    showDismissedSections: Boolean,
    onToggleShowDismissedSections: () -> Unit,
    modifier: Modifier = Modifier,
    showAllOption: Boolean = true,
    selectorType: String = UiConstants.DEFAULT_SECTION_SELECTOR_TYPE
) {
    if (selectorType == UiConstants.SECTION_SELECTOR_CHIPS) {
        SectionChipBar(
            sections = allSections,
            selectedSectionId = selectedSectionId,
            onSectionSelected = onSectionSelected,
            showDismissed = showDismissedSections,
            onToggleShowDismissed = onToggleShowDismissedSections,
            modifier = modifier,
            showAllOption = showAllOption
        )
    } else {
        val allWithOption = remember(allSections, showAllOption) {
            if (showAllOption) {
                val allOption = Section(
                    id = AppConstants.ALL_SECTIONS_ID,
                    name = "", // Handled by itemDescription
                    iconIdentifier = "all",
                    color = "#808080"
                )
                listOf(allOption) + allSections
            } else allSections
        }

        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val selectedSection = allWithOption.find { it.id == selectedSectionId }
            val filteredSections = remember(allWithOption, showDismissedSections, selectedSectionId) {
                val sections = if (showDismissedSections) allWithOption else allWithOption.filter { !it.dismissed || it.id == AppConstants.ALL_SECTIONS_ID }
                // Ensure selected section is always in the list even if dismissed
                if (selectedSection != null && !sections.any { it.id == selectedSection.id }) {
                    (sections + selectedSection).sortedBy { it.id }
                } else sections
            }

            SelectionDropdown(
                label = stringResource(R.string.options_manage_sections),
                selectedItem = selectedSection,
                items = filteredSections,
                allSections = allSections,
                onItemSelected = { onSectionSelected(it.id) },
                itemDescription = { 
                    if (it.id == AppConstants.ALL_SECTIONS_ID) stringResource(R.string.section_all)
                    else if (it.id == AppConstants.DEFAULT_SECTION_ID) stringResource(R.string.section_common)
                    else it.name 
                },
                itemPhotoUri = { it.photoUri },
                itemIconIdentifier = { it.iconIdentifier },
                itemSectionId = { it.id },
                category = Category.SECTIONS,
                categoryColor = Color.Gray,
                showSectionBadge = false,
                isDismissed = { it.dismissed },
                placeholder = stringResource(R.string.section_all),
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onToggleShowDismissedSections) {
                Icon(
                    imageVector = if (showDismissedSections) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
