package com.moxmose.moxequiplog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.moxmose.moxequiplog.ui.equipments.EquipmentsScreen
import com.moxmose.moxequiplog.ui.maintenancelog.MaintenanceLogScreen
import com.moxmose.moxequiplog.ui.operations.OperationTypeScreen
import com.moxmose.moxequiplog.ui.options.OptionsScreen
import com.moxmose.moxequiplog.ui.theme.MoxEquipLogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoxEquipLogTheme {
                MoxEquipLogApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun MoxEquipLogApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.LOGS) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = stringResource(it.labelRes)
                        )
                    },
                    label = { 
                        Text(
                            text = stringResource(it.labelRes),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        ) 
                    },
                    selected = it == currentDestination,
                    onClick = { if (it.enabled) currentDestination = it },
                    enabled = it.enabled,
                    alwaysShowLabel = false
                )
            }
        }
    ) { 
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.LOGS -> MaintenanceLogScreen()
                AppDestinations.EQUIPMENTS -> EquipmentsScreen()
                AppDestinations.OPERATIONS -> OperationTypeScreen()
                AppDestinations.REPORTS -> Greeting(name = "Reports", modifier = Modifier.padding(innerPadding))
                AppDestinations.OPTIONS -> OptionsScreen(modifier = Modifier.padding(innerPadding))
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
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MoxEquipLogTheme {
        Greeting("Android")
    }
}
