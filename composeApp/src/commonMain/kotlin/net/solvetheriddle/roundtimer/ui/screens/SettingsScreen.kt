package net.solvetheriddle.roundtimer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.solvetheriddle.roundtimer.model.SettingsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsState: SettingsState,
    onNavigateUp: () -> Unit,
    onSettingChanged: (String, Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background,
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isLandscape = maxWidth > maxHeight
                val columnModifier = if (isLandscape) {
                    Modifier
                        .fillMaxWidth(0.5f)
                        .align(Alignment.Center)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                } else {
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                }
                Column(
                    modifier = columnModifier
                ) {
                    SettingItem(
                        name = "Subtle drumming",
                        subtitle = "Drum call from T-60 every 10s",
                        isChecked = settingsState.isSubtleDrummingEnabled
                    ) { onSettingChanged("subtleDrumming", it) }
                    SettingItem(
                        name = "Intense drumming",
                        subtitle = "Intense drumming from T-20",
                        isChecked = settingsState.isIntenseDrummingEnabled
                    ) { onSettingChanged("intenseDrumming", it) }
                    SettingItem(
                        name = "Overtime alarm",
                        subtitle = "Annoying alarm after timer expires",
                        isChecked = settingsState.isOvertimeAlarmEnabled
                    ) { onSettingChanged("overtimeAlarm", it) }
                    SettingItem(
                        name = "Timeout gong",
                        subtitle = "Gong when timer expires",
                        isChecked = settingsState.isTimeoutGongEnabled
                    ) { onSettingChanged("timeoutGong", it) }
                    SettingItem(
                        name = "Jonas scolding",
                        subtitle = "Humorous messages in Czech",
                        isChecked = settingsState.isJonasScoldingEnabled
                    ) { onSettingChanged("jonasScolding", it) }
                    SettingItem(
                        name = "Secret fast forward",
                        subtitle = "Click the background to skip 10s",
                        isChecked = settingsState.isSecretFastForwardEnabled
                    ) { onSettingChanged("secretFastForward", it) }
                }
            }
        }
    }
}

@Composable
private fun SettingItem(
    name: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(name, color = MaterialTheme.colorScheme.onBackground)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = isChecked, onCheckedChange = onCheckedChange)
    }
}
