package com.moxmose.moxequiplog

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.lifecycleScope
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.ImageRepository
import com.moxmose.moxequiplog.ui.components.AppBackground
import com.moxmose.moxequiplog.ui.equipments.EquipmentsScreen
import com.moxmose.moxequiplog.ui.maintenancelog.MaintenanceLogScreen
import com.moxmose.moxequiplog.ui.operations.OperationTypeScreen
import com.moxmose.moxequiplog.ui.options.OptionsScreen
import com.moxmose.moxequiplog.ui.options.OptionsViewModel
import com.moxmose.moxequiplog.ui.theme.MoxEquipLogTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    private val imageRepository: ImageRepository by inject()
    private val appSettingsManager: AppSettingsManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lifecycleScope.launch {
            imageRepository.initializeAppData()
        }

        enableEdgeToEdge()
        setContent {
            MoxEquipLogTheme {
                MoxEquipLogApp(appSettingsManager)
            }
        }
    }
}

@Composable
fun MoxEquipLogApp(
    appSettingsManager: AppSettingsManager,
    optionsViewModel: OptionsViewModel = koinViewModel()
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.LOGS) }
    
    val categoriesUiState by optionsViewModel.categoriesUiState.collectAsState()
    val isTintEnabled by appSettingsManager.backgroundTintEnabled.collectAsState()
    val bgUri by appSettingsManager.backgroundUri.collectAsState()
    val bgBlur by appSettingsManager.backgroundBlur.collectAsState()
    val bgSaturation by appSettingsManager.backgroundSaturation.collectAsState()

    // TEST REFRESH LOG: Stampa lo stato dopo 5 secondi per vedere se Room si è svegliata
    LaunchedEffect(Unit) {
        delay(5000)
        Log.d("AppBackground", "5s Heartbeat - URI in Manager: $bgUri")
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

    Box(modifier = Modifier.fillMaxSize()) {
        AppBackground(
            backgroundUri = bgUri,
            blurRadius = bgBlur,
            saturation = bgSaturation,
            tintColor = currentSectionColor
        )
        
        NavigationSuiteScaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            navigationSuiteItems = {
                AppDestinations.entries.forEach {
                    item(
                        icon = { Icon(it.icon, contentDescription = stringResource(it.labelRes)) },
                        label = { Text(text = stringResource(it.labelRes), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        selected = it == currentDestination,
                        onClick = { if (it.enabled) currentDestination = it },
                        enabled = it.enabled,
                        alwaysShowLabel = false
                    )
                }
            }
        ) { 
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Transparent
            ) {
                when (currentDestination) {
                    AppDestinations.LOGS -> MaintenanceLogScreen()
                    AppDestinations.EQUIPMENTS -> EquipmentsScreen()
                    AppDestinations.OPERATIONS -> OperationTypeScreen()
                    AppDestinations.REPORTS -> Greeting(name = "Reports")
                    AppDestinations.OPTIONS -> OptionsScreen()
                }
            }
        }
    }
}

enum class AppDestinations(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val enabled: Boolean = true
) {
    LOGS(R.string.navigation_logs, Icons.Default.Home),
    EQUIPMENTS(R.string.navigation_equipments, Icons.AutoMirrored.Filled.List),
    OPERATIONS(R.string.navigation_operations, Icons.Default.Build),
    REPORTS(R.string.navigation_reports, Icons.Default.Assessment, false),
    OPTIONS(R.string.navigation_options, Icons.Default.Settings),
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
