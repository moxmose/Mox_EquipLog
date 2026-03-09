package com.moxmose.moxequiplog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotInterested
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.moxmose.moxequiplog.ui.options.EquipmentIconProvider

@Composable
fun ImageIcon(
    photoUri: String?,
    iconIdentifier: String?,
    modifier: Modifier = Modifier,
    category: String? = "",
    borderColor: Color? = null,
    contentPadding: Dp = 0.dp // Padding variabile per non influenzare altri contesti
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
            .then(
                if (borderColor != null) Modifier.border(2.dp, borderColor, CircleShape)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
            when {
                photoUri != null -> {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                iconIdentifier != null -> {
                    Icon(
                        imageVector = EquipmentIconProvider.getIcon(iconIdentifier, category ?: ""),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                else -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.NotInterested,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
