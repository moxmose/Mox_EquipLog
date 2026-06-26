package com.moxmose.moxequiplog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.moxmose.moxequiplog.data.AppSettingsManager
import com.moxmose.moxequiplog.data.ImageRepository
import com.moxmose.moxequiplog.data.MaintenanceManager
import com.moxmose.moxequiplog.ui.components.AppBackground
import com.moxmose.moxequiplog.ui.equipment.EquipmentScreen
import com.moxmose.moxequiplog.ui.maintenancelog.MaintenanceLogScreen
import com.moxmose.moxequiplog.ui.operations.OperationTypeScreen
import com.moxmose.moxequiplog.ui.options.DemoScenario
import com.moxmose.moxequiplog.ui.options.OptionsScreen
import com.moxmose.moxequiplog.ui.options.OptionsViewModel
import com.moxmose.moxequiplog.ui.reports.ReportsScreen
import com.moxmose.moxequiplog.ui.theme.MoxEquipLogTheme
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    private val imageRepository: ImageRepository by inject()
    private val appSettingsManager: AppSettingsManager by inject()
    private val maintenanceManager: MaintenanceManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lifecycleScope.launch {
            imageRepository.initializeAppData()
        }

        enableEdgeToEdge()
        setContent {
            MoxEquipLogTheme {
                val showWelcome by appSettingsManager.showWelcomeAlert.collectAsStateWithLifecycle(initialValue = null)
                val isAppEmpty by maintenanceManager.isAppEmpty().collectAsStateWithLifecycle(initialValue = null)
                
                MoxEquipLogApp(
                    showWelcome = showWelcome,
                    isAppEmpty = isAppEmpty,
                    onDismissWelcome = { dontShowAgain ->
                        lifecycleScope.launch {
                            if (dontShowAgain) appSettingsManager.setShowWelcomeAlert(false)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoxEquipLogApp(
    showWelcome: Boolean?,
    isAppEmpty: Boolean?,
    onDismissWelcome: (Boolean) -> Unit,
    optionsViewModel: OptionsViewModel = koinViewModel()
) {
    var currentDestination by rememberSaveable { 
        mutableStateOf(AppDestinations.LOGS) 
    }

    // Redirect based on whether the app is empty (has no equipment/logs)
    var initialRedirectDone by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(isAppEmpty) {
        if (isAppEmpty != null && !initialRedirectDone) {
            currentDestination = if (isAppEmpty) AppDestinations.OPTIONS else AppDestinations.LOGS
            initialRedirectDone = true
        }
    }

    // Show welcome alert only if it's not dismissed
    var welcomeVisible by remember(showWelcome) { mutableStateOf(showWelcome == true) }
    var showDemoSelection by remember { mutableStateOf(false) }

    if (showDemoSelection) {
        DemoSelectionDialog(
            onDismiss = { showDemoSelection = false },
            onScenarioSelected = { scenario ->
                optionsViewModel.generateDemoData(scenario)
                showDemoSelection = false
                welcomeVisible = false
                onDismissWelcome(false) // Dismiss welcome but don't "never show again" unless they want to
            }
        )
    }

    if (welcomeVisible) {
        var dontShowAgain by remember { mutableStateOf(false) }
        
        BasicAlertDialog(onDismissRequest = { 
            onDismissWelcome(dontShowAgain)
            welcomeVisible = false 
        }) {
            Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(text = stringResource(R.string.about_dialog_title), style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = stringResource(R.string.welcome_dialog_content), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(checked = dontShowAgain, onCheckedChange = { dontShowAgain = it })
                        Text(
                            text = stringResource(R.string.dismiss_next_time),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showDemoSelection = true },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.RocketLaunch, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.button_try_demo))
                        }

                        TextButton(
                            onClick = { 
                                onDismissWelcome(dontShowAgain)
                                welcomeVisible = false 
                            }
                        ) {
                            Text(stringResource(R.string.button_ok))
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppBackground(currentDestination = currentDestination)
        
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
                    AppDestinations.LOGS -> MaintenanceLogScreen(onNavigateToOptions = { currentDestination = AppDestinations.OPTIONS })
                    AppDestinations.EQUIPMENT -> EquipmentScreen()
                    AppDestinations.OPERATIONS -> OperationTypeScreen()
                    AppDestinations.REPORTS -> ReportsScreen(onBack = { currentDestination = AppDestinations.LOGS })
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
    EQUIPMENT(R.string.navigation_equipment, Icons.AutoMirrored.Filled.List),
    OPERATIONS(R.string.navigation_operations, Icons.Default.Build),
    REPORTS(R.string.navigation_reports, Icons.Default.Assessment),
    OPTIONS(R.string.navigation_options, Icons.Default.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoSelectionDialog(
    onDismiss: () -> Unit,
    onScenarioSelected: (DemoScenario) -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = stringResource(R.string.demo_selection_title), style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.demo_selection_desc), 
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DemoScenarioItem(
                        icon = Icons.Default.DirectionsCar,
                        label = stringResource(R.string.demo_scenario_cars),
                        onClick = { onScenarioSelected(DemoScenario.CARS) }
                    )
                    DemoScenarioItem(
                        icon = Icons.Default.Grass,
                        label = stringResource(R.string.demo_scenario_garden),
                        onClick = { onScenarioSelected(DemoScenario.GARDEN) }
                    )
                    DemoScenarioItem(
                        icon = Icons.Default.MonitorHeart,
                        label = stringResource(R.string.demo_scenario_health),
                        onClick = { onScenarioSelected(DemoScenario.HEALTH) }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DemoScenarioItem(
                        icon = Icons.Default.DirectionsBike,
                        label = stringResource(R.string.demo_scenario_bikes),
                        onClick = { onScenarioSelected(DemoScenario.BIKES) }
                    )
                    DemoScenarioItem(
                        icon = Icons.Default.RocketLaunch,
                        label = stringResource(R.string.demo_scenario_all),
                        onClick = { onScenarioSelected(DemoScenario.ALL) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        }
    }
}

@Composable
fun DemoScenarioItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
            .width(80.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
