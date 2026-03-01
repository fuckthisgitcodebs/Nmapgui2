package com.example.nmapgui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nmapgui.ui.screens.*
import com.example.nmapgui.ui.theme.NmapGuiTheme

enum class Screen(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    SCAN("Scan", Icons.Default.Search),
    RESULTS("Results", Icons.Default.List),
    HISTORY("History", Icons.Default.History),
    SETTINGS("Settings", Icons.Default.Settings),
}

class MainActivity : ComponentActivity() {
    private val vm: NmapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by vm.settings.collectAsState()
            val state by vm.uiState.collectAsState()

            NmapGuiTheme(darkTheme = settings.darkTheme) {
                var currentScreen by remember { mutableStateOf(Screen.SCAN) }

                // Auto-navigate to Results when scan completes
                LaunchedEffect(state.scanComplete) {
                    if (state.scanComplete) {
                        currentScreen = Screen.RESULTS
                        vm.clearScanComplete()
                    }
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            Screen.entries.forEach { screen ->
                                val badgeCount = if (screen == Screen.RESULTS && state.isScanning) 1 else 0
                                NavigationBarItem(
                                    selected = currentScreen == screen,
                                    onClick = { currentScreen = screen },
                                    icon = {
                                        if (badgeCount > 0) {
                                            BadgedBox(badge = { Badge { Text("●") } }) {
                                                Icon(screen.icon, contentDescription = screen.label)
                                            }
                                        } else {
                                            Icon(screen.icon, contentDescription = screen.label)
                                        }
                                    },
                                    label = { Text(screen.label) },
                                )
                            }
                        }
                    }
                ) { padding ->
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)) {
                        when (currentScreen) {
                            Screen.SCAN -> ScanScreen(vm)
                            Screen.RESULTS -> ResultsScreen(vm)
                            Screen.HISTORY -> HistoryScreen(vm)
                            Screen.SETTINGS -> SettingsScreen(vm)
                        }
                    }
                }
            }
        }
    }
}
