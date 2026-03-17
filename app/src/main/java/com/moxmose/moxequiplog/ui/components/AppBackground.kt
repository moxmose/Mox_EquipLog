package com.moxmose.moxequiplog.ui.components

import android.util.Log
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay

@Composable
fun AppBackground(
    backgroundUri: String?,
    blurRadius: Float,
    saturation: Float,
    tintColor: Color? = null
) {
    val context = LocalContext.current
    var startBlur by remember { mutableStateOf(false) }
    
    // Animazione fluida del raggio di sfocatura
    val animatedBlur by animateDpAsState(
        targetValue = if (startBlur) blurRadius.dp else 0.dp,
        animationSpec = tween(durationMillis = 1000), // 1 secondo di transizione
        label = "BackgroundBlurAnimation"
    )

    // Logica di attivazione: aspetta che l'immagine sia "calda"
    LaunchedEffect(backgroundUri) {
        startBlur = false
        if (!backgroundUri.isNullOrBlank()) {
            delay(500) // Mezzo secondo di immagine nitida per stabilizzare il rendering
            startBlur = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!backgroundUri.isNullOrBlank()) {
            val matrix = ColorMatrix().apply { setToSaturation(saturation) }
            
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(backgroundUri)
                    .allowHardware(false) // NECESSARIO per i filtri grafici su molti dispositivi
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = animatedBlur), // Applichiamo il raggio animato
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(matrix),
                onSuccess = { Log.d("AppBackground", "Render Success") },
                onError = { Log.e("AppBackground", "Render Error: ${it.result.throwable}") }
            )
        }
        
        // Tint overlay
        tintColor?.let {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(it.copy(alpha = 0.25f))
            )
        }
    }
}
