package com.moxmose.moxequiplog.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moxmose.moxequiplog.R
import com.moxmose.moxequiplog.ui.options.DemoScenario

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeAdvisor(
    onDismiss: (Boolean) -> Unit,
    onScenarioSelected: (DemoScenario) -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var dontShowAgain by remember { mutableStateOf(false) }
    
    BasicAlertDialog(
        onDismissRequest = { onDismiss(dontShowAgain) }
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "WelcomeAdvisorContent"
                ) { step ->
                    when (step) {
                        0 -> WelcomeStep()
                        1 -> PrincipleStep()
                        2 -> PredictiveStep()
                        3 -> DemoStep(onScenarioSelected = onScenarioSelected)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (currentStep < 3) {
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
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0) {
                        TextButton(onClick = { currentStep-- }) {
                            Text(stringResource(R.string.button_back))
                        }
                    } else {
                        Spacer(Modifier)
                    }

                    if (currentStep < 3) {
                        TextButton(onClick = { currentStep++ }) {
                            Text(stringResource(R.string.button_next))
                        }
                    } else {
                        OutlinedButton(onClick = { onDismiss(dontShowAgain) }) {
                            Text(stringResource(R.string.button_skip_demo))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column {
        Text(
            text = stringResource(R.string.about_dialog_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.welcome_dialog_content),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun PrincipleStep() {
    Column {
        Text(
            text = stringResource(R.string.welcome_principle_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.welcome_principle_content),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun PredictiveStep() {
    Column {
        Text(
            text = stringResource(R.string.welcome_predictive_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.welcome_predictive_content),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun DemoStep(
    onScenarioSelected: (DemoScenario) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.welcome_demo_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.welcome_demo_content),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.Start)
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
    }
}
