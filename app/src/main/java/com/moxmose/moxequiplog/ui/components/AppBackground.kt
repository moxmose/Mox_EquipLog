package com.moxmose.moxequiplog.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun AppBackground(
    backgroundUri: String?,
    blurRadius: Float,
    saturation: Float,
    tintColor: Color? = null
) {
    Log.d("AppBackground", "Drawing background. URI: $backgroundUri, Blur: $blurRadius, Tint: $tintColor")
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (!backgroundUri.isNullOrBlank()) {
            val matrix = ColorMatrix().apply { setToSaturation(saturation) }
            AsyncImage(
                model = backgroundUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = blurRadius.dp),
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(matrix),
                onSuccess = { Log.d("AppBackground", "Image loaded successfully") },
                onError = { Log.e("AppBackground", "Failed to load image: ${it.result.throwable}") }
            )
        }
        
        // Applichiamo il tint sopra l'immagine (o come sfondo se l'immagine manca)
        tintColor?.let {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(it.copy(alpha = 0.25f)) // Tint leggero
            )
        }
    }
}
