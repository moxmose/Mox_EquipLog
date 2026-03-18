package com.moxmose.moxequiplog.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.moxmose.moxequiplog.AppDestinations
import com.moxmose.moxequiplog.ui.options.OptionsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppBackground(
    currentDestination: AppDestinations,
    viewModel: OptionsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    
    val bgUri by viewModel.backgroundUri.collectAsState()
    val blurRadius by viewModel.backgroundBlur.collectAsState()
    val saturation by viewModel.backgroundSaturation.collectAsState()
    val bgAlpha by viewModel.backgroundImageAlpha.collectAsState()
    val isTintEnabled by viewModel.backgroundTintEnabled.collectAsState()
    val tintAlpha by viewModel.backgroundTintAlpha.collectAsState()
    val categoriesUiState by viewModel.categoriesUiState.collectAsState()

    var isImageReady by remember { mutableStateOf(false) }
    
    val animatedBlur by animateDpAsState(
        targetValue = if (isImageReady) blurRadius.dp else 0.dp,
        animationSpec = tween(durationMillis = 1000),
        label = "BackgroundBlurAnimation"
    )

    LaunchedEffect(bgUri) {
        isImageReady = false
    }

    val currentSectionColor = remember(currentDestination, categoriesUiState, isTintEnabled) {
        if (!isTintEnabled) null
        else {
            val categoryId = when (currentDestination) {
                AppDestinations.LOGS -> "LOG"
                AppDestinations.EQUIPMENTS -> "EQUIPMENT"
                AppDestinations.OPERATIONS -> "OPERATION"
                else -> null
            }
            categoriesUiState.find { it.category.id == categoryId }?.color?.let {
                try { Color(android.graphics.Color.parseColor(it)) } catch (_: Exception) { null }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!bgUri.isNullOrBlank()) {
            val matrix = ColorMatrix().apply { setToSaturation(saturation) }
            
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(bgUri)
                    .allowHardware(false) 
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(bgAlpha) // Applichiamo l'opacità dell'immagine
                    .blur(radius = animatedBlur),
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(matrix),
                onSuccess = { isImageReady = true }
            )
        }
        
        currentSectionColor?.let {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(it.copy(alpha = tintAlpha))
            )
        }
    }
}
